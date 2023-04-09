package com.darkrockstudios.apps.hammer.common.data.globalsettings

import kotlinx.serialization.Serializable

@Serializable
data class GlobalSettings(
	val projectsDirectory: String,
	val uiTheme: UiTheme = UiTheme.FollowSystem,
	val automaticSyncing: Boolean = true,
	val nux: NewUserExperience = NewUserExperience()
)

@Serializable
data class NewUserExperience(
	val exampleProjectCreated: Boolean = false
)

enum class UiTheme {
	Light,
	Dark,
	FollowSystem
}