package com.darkrockstudios.apps.hammer

import com.akuleshov7.ktoml.Toml
import com.darkrockstudios.apps.hammer.plugins.*
import com.darkrockstudios.apps.hammer.plugins.kweb.configureKweb
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.serialization.decodeFromString
import java.io.File
import java.security.KeyStore

fun main(args: Array<String>) {
	val parser = ArgParser("server")
	val configPathArg by parser.option(
		ArgType.String,
		shortName = "c",
		fullName = "config",
		description = "Server Config Path"
	)

	parser.parse(args)

	val config: ServerConfig = configPathArg?.let {
		loadConfig(it)
	} ?: ServerConfig()

	startServer(config)
}

private fun loadConfig(path: String): ServerConfig {
	val file = File(path)
	val tomlStr = file.readUtf8()
	return Toml.decodeFromString<ServerConfig>(tomlStr)
}

private fun startServer(config: ServerConfig) {
	val environment = applicationEngineEnvironment {
		val bindHost = "0.0.0.0"
		connector {
			port = config.port
			host = bindHost
		}

		config.sslCert?.apply {
			sslConnector(
				keyStore = getKeyStore(this),
				keyAlias = keyAlias ?: "",
				keyStorePassword = { storePassword.toCharArray() },
				privateKeyPassword = { (keyPassword ?: "").toCharArray() }) {
				keyStorePath = File(path)
				host = bindHost
			}
		}

		module{
			appMain(config)
		}
		watchPaths = listOf("classes")
	}

	embeddedServer(
		Jetty,
		environment = environment
	).start(wait = true)
}

private fun getKeyStore(sslConfig: SslCertConfig): KeyStore {
	val certFile = File(sslConfig.path)
	if (certFile.exists().not()) throw IllegalArgumentException("SSL Cert not found")
	return KeyStore.getInstance(certFile, sslConfig.storePassword.toCharArray())
}

fun Application.appMain(config: ServerConfig) {
	configureDependencyInjection()
	configureSerialization()
	configureMonitoring()
	configureHTTP()
	configureSecurity()
	configureRouting()
	configureKweb(config)
}
