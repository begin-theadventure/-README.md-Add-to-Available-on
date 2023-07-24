//val androidx_compose_version: String by extra
val jetbrains_compose_version: String by extra
val jetpack_compose_compiler_version: String by extra

val RELEASE_STORE_FILE = System.getenv("RELEASE_STORE_FILE") ?: "/"
val RELEASE_STORE_PASSWORD = System.getenv("RELEASE_STORE_PASSWORD") ?: ""
val RELEASE_KEY_ALIAS = System.getenv("RELEASE_KEY_ALIAS") ?: ""
val RELEASE_KEY_PASSWORD = System.getenv("RELEASE_KEY_PASSWORD") ?: ""

plugins {
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.compose)
	alias(libs.plugins.jetbrains.kover)
}

group = "com.darkrockstudios.apps.hammer"
version = libs.versions.app.get()

repositories {
	mavenCentral()
}

dependencies {
	api(project(":composeUi"))
	implementation("androidx.activity:activity-compose:1.7.2")
	implementation(libs.koin.android)
	implementation("androidx.glance:glance:1.0.0-beta01")
	implementation("androidx.glance:glance-appwidget:1.0.0-beta01")
	implementation("androidx.glance:glance-material3:1.0.0-beta01")

	implementation("androidx.work:work-runtime-ktx:2.8.1")
	implementation("com.google.android.material:material:1.9.0")
	implementation("androidx.appcompat:appcompat:1.6.1")

	androidTestImplementation("androidx.test.ext:junit:1.1.5")
	androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
	androidTestImplementation("androidx.test:core:1.5.0")
	androidTestImplementation("androidx.test:core-ktx:1.5.0")
	androidTestImplementation("androidx.test:runner:1.5.2")
	androidTestUtil("androidx.test:orchestrator:1.4.2")
	androidTestImplementation("androidx.test:runner:1.5.2")
}

android {
	namespace = "com.darkrockstudios.apps.hammer.android"
	compileSdk = libs.versions.android.sdk.compile.get().toInt()
	defaultConfig {
		applicationId = "com.darkrockstudios.apps.hammer.android"
		minSdk = libs.versions.android.sdk.min.get().toInt()
		targetSdk = libs.versions.android.sdk.target.get().toInt()
		versionCode = libs.versions.android.version.code.get().toInt()
		versionName = libs.versions.app.get()

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}
	buildFeatures {
		compose = true
	}
	composeOptions {
		kotlinCompilerExtensionVersion = jetpack_compose_compiler_version
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	signingConfigs {
		create("release") {
			keyAlias = RELEASE_KEY_ALIAS
			keyPassword = RELEASE_KEY_PASSWORD
			storeFile = file(RELEASE_STORE_FILE)
			storePassword = RELEASE_STORE_PASSWORD
		}
	}

	buildTypes {
		debug {
			applicationIdSuffix = ".dev"
			versionNameSuffix = "-dev"
		}

		release {
			isMinifyEnabled = false
			isShrinkResources = false

			signingConfig = signingConfigs.getByName("release")

			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				File("proguard-rules.pro")
			)
		}
	}
}