package com.darkrockstudios.apps.hammer.common.projecthome

import com.arkivanov.decompose.value.Value
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.encyclopediarepository.entry.EntryType
import com.darkrockstudios.apps.hammer.common.dependencyinjection.HammerComponent
import com.darkrockstudios.apps.hammer.common.projectroot.Router

interface ProjectHome : Router, HammerComponent {
	val state: Value<State>

	suspend fun exportProject(path: String)
	fun beginProjectExport()
	fun endProjectExport()

	data class State(
		val projectDef: ProjectDef,
		val created: String,
		val numberOfScenes: Int = 0,
		val totalWords: Int = 0,
		val wordsByChapter: Map<String, Int> = emptyMap(),
		val encyclopediaEntriesByType: Map<EntryType, Int> = emptyMap(),
		val showExportDialog: Boolean = false
	)
}