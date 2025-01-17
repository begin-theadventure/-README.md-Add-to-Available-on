package com.darkrockstudios.apps.hammer.account

import com.darkrockstudios.apps.hammer.Account
import com.darkrockstudios.apps.hammer.AuthToken
import com.darkrockstudios.apps.hammer.BaseTest
import com.darkrockstudios.apps.hammer.account.AccountsRepository.Companion.MAX_PASSWORD_LENGTH
import com.darkrockstudios.apps.hammer.account.AccountsRepository.Companion.MIN_PASSWORD_LENGTH
import com.darkrockstudios.apps.hammer.database.AccountDao
import com.darkrockstudios.apps.hammer.database.AuthTokenDao
import com.darkrockstudios.apps.hammer.utilities.toISO8601
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsRepositoryTest : BaseTest() {

	private lateinit var accountDao: AccountDao
	private lateinit var authTokenDao: AuthTokenDao

	private val userId = 1L
	private val email = "test@example.com"
	private val installId = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
	private val password = "power123"
	private val salt = "12345"
	private val bearerToken = "izWqAxTWhMU19TEJowrgXqPwzNmYDUEvJ4TB18wfUdj2LJcTtD30ZmNJj7TnET1A"
	private val refreshToken = "12345xTWhMU19TEJowrgXqPwzNmYDUEvJ4TB18wfUdj2LJcTtD30ZmNJj7TnET1A"
	private val hashedPassword = AccountsRepository.hashPassword(password = password, salt = salt)

	private lateinit var account: Account

	private fun createAuthToken() = AuthToken(
		userId = userId,
		installId = installId,
		token = bearerToken,
		refresh = refreshToken,
		created = (Clock.System.now() - 365.days).toISO8601(),
		expires = (Clock.System.now() + 30.days).toISO8601()
	)

	@Before
	override fun setup() {
		super.setup()

		accountDao = mockk()
		authTokenDao = mockk()

		setupKoin()

		account = Account(
			id = userId,
			email = email,
			salt = salt,
			password_hash = hashedPassword,
			created = (Clock.System.now() - 128.days).toISO8601(),
			isAdmin = true
		)
	}

	@Test
	fun `Login - Success`() = runTest {
		coEvery { accountDao.findAccount(any()) } returns account

		val authToken = createAuthToken()
		coEvery { authTokenDao.getTokenByInstallId(installId) } returns authToken

		val accountsRepository = AccountsRepository(accountDao, authTokenDao)

		val result = accountsRepository.login(email = email, installId = installId, password = password)
		assertTrue { result.isSuccess }
	}

	@Test
	fun `Login - Wrong password`() = runTest {
		coEvery { accountDao.findAccount(any()) } returns account
		val accountsRepository = AccountsRepository(accountDao, authTokenDao)

		val result = accountsRepository.login(email = email, installId = installId, password = password + "4")
		assertTrue { result.isFailure }
	}

	@Test
	fun `Login - No User`() = runTest {
		coEvery { accountDao.findAccount(any()) } returns null
		val accountsRepository = AccountsRepository(accountDao, authTokenDao)

		val result = accountsRepository.login(email = "no@account.com", installId = installId, password = "power1234")
		assertTrue { result.isFailure }
	}

	@Test
	fun `Create Account - Success`() = runTest {
		coEvery { accountDao.numAccounts() } returns 1
		coEvery { accountDao.findAccount(any()) } returns null
		coEvery { accountDao.createAccount(any(), any(), any(), any()) } returns userId
		coEvery { authTokenDao.setToken(any(), any(), any(), any()) } just Runs
		val accountsRepository = AccountsRepository(accountDao, authTokenDao)

		val result = accountsRepository.createAccount(email = email, installId = installId, password = password)
		assertTrue { result.isSuccess }

		val token = result.getOrThrow()
		assertTrue { token.isValid() }
	}

	@Test
	fun `Create Account - Failure - Existing Account`() = runTest {
		coEvery { accountDao.findAccount(any()) } returns account
		val accountsRepository = AccountsRepository(accountDao, authTokenDao)

		val result = accountsRepository.createAccount(email = email, installId = installId, password = password)
		assertTrue { result.isFailure }
	}

	@Test
	fun `Create Account - Failure - Password Short`() = runTest {
		coEvery { accountDao.findAccount(any()) } returns null
		val accountsRepository = AccountsRepository(accountDao, authTokenDao)

		val result = accountsRepository.createAccount(
			email = email,
			installId = installId,
			password = "x".repeat(MIN_PASSWORD_LENGTH - 1)
		)
		assertTrue { result.isFailure }
		val exception = result.exceptionOrNull()
		assertTrue { exception is InvalidPassword }
		assertEquals(AccountsRepository.Companion.PasswordResult.TOO_SHORT, (exception as InvalidPassword).result)
	}

	@Test
	fun `Create Account - Failure - Password Long`() = runTest {
		coEvery { accountDao.findAccount(any()) } returns null
		val accountsRepository = AccountsRepository(accountDao, authTokenDao)

		val result = accountsRepository.createAccount(
			email = email,
			installId = installId,
			password = "x".repeat(MAX_PASSWORD_LENGTH + 1)
		)
		assertTrue { result.isFailure }
		val exception = result.exceptionOrNull()
		assertTrue { exception is InvalidPassword }
		assertEquals(AccountsRepository.Companion.PasswordResult.TOO_LONG, (exception as InvalidPassword).result)
	}

	@Test
	fun `Create Account - Failure - Invalid Email`() = runTest {
		coEvery { accountDao.findAccount(any()) } returns account
		val accountsRepository = AccountsRepository(accountDao, authTokenDao)

		val result = accountsRepository.createAccount(
			email = "notanemail",
			installId = installId,
			password = password
		)
		assertTrue { result.isFailure }
		val exception = result.exceptionOrNull()
		assertTrue { exception is CreateFailed }
	}

	@Test
	fun `Check Token - Success`() = runTest {
		val token = createAuthToken()
		coEvery { authTokenDao.getTokenByAuthToken(any()) } returns token
		val accountsRepository = AccountsRepository(accountDao, authTokenDao)

		val result = accountsRepository.checkToken(userId, bearerToken)
		assertTrue { result.isSuccess }
		assertEquals(userId, result.getOrThrow())
	}

	@Test
	fun `Check Token - Failure`() = runTest {
		coEvery { authTokenDao.getTokenByAuthToken(any()) } returns null
		val accountsRepository = AccountsRepository(accountDao, authTokenDao)

		val result = accountsRepository.checkToken(userId, bearerToken)
		assertTrue { result.isFailure }
	}
}