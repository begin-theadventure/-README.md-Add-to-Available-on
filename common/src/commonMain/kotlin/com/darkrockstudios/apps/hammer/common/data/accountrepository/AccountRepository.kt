package com.darkrockstudios.apps.hammer.common.data.accountrepository

import com.benasher44.uuid.uuid4
import com.darkrockstudios.apps.hammer.base.http.Token
import com.darkrockstudios.apps.hammer.common.data.globalsettings.GlobalSettingsRepository
import com.darkrockstudios.apps.hammer.common.data.globalsettings.ServerSettings
import com.darkrockstudios.apps.hammer.common.dependencyinjection.updateCredentials
import com.darkrockstudios.apps.hammer.common.server.HttpFailureException
import com.darkrockstudios.apps.hammer.common.server.ServerAccountApi
import io.ktor.client.*
import io.ktor.client.plugins.auth.providers.*

class AccountRepository(
	private val globalSettingsRepository: GlobalSettingsRepository,
	private val accountApi: ServerAccountApi,
	private val httpClient: HttpClient
) {
	suspend fun setupServer(
		ssl: Boolean,
		url: String,
		email: String,
		password: String,
		create: Boolean
	): Result<Boolean> {
		val newSettings = ServerSettings(
			userId = -1,
			ssl = ssl,
			url = url,
			email = email,
			installId = uuid4().toString(),
			bearerToken = null,
			refreshToken = null,
		)

		globalSettingsRepository.updateServerSettings(newSettings)

		val result = if (create) {
			accountApi.createAccount(
				email = email,
				password = password,
				installId = "asd"
			)
		} else {
			accountApi.login(
				email = email,
				password = password,
				installId = "asd"
			)
		}

		return if (result.isSuccess) {
			val token: Token = result.getOrThrow()

			val authedSettings = newSettings.copy(
				userId = token.userId,
				bearerToken = token.auth,
				refreshToken = token.refresh
			)

			val bearerTokens = BearerTokens(accessToken = token.auth, refreshToken = token.refresh)
			httpClient.updateCredentials(bearerTokens)
			globalSettingsRepository.updateServerSettings(authedSettings)

			Result.success(true)
		} else {
			globalSettingsRepository.deleteServerSettings()

			val message = (result.exceptionOrNull() as? HttpFailureException)?.error?.message ?: "Unknown error"
			Result.failure(ServerSetupFailed(message))
		}
	}

	suspend fun testAuth(): Boolean {
		return accountApi.testAuth().isSuccess
	}
}

class ServerSetupFailed(message: String) : Exception(message)