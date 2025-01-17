package com.darkrockstudios.apps.hammer.common.components.projectselection.projectslist

import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.darkrockstudios.apps.hammer.common.components.projecteditor.metadata.ProjectMetadata
import com.darkrockstudios.apps.hammer.common.components.projectselection.ProjectData
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.projectsync.SyncLogMessage
import com.darkrockstudios.apps.hammer.common.dependencyinjection.HammerComponent
import com.darkrockstudios.apps.hammer.common.fileio.HPath
import dev.icerock.moko.resources.StringResource

interface ProjectsList : HammerComponent {
	val state: Value<State>

	fun loadProjectList()
	fun selectProject(projectDef: ProjectDef)
	fun showCreate()
	fun hideCreate()
	fun createProject(projectName: String)
	fun deleteProject(projectDef: ProjectDef)
	fun syncProjects(callback: (Boolean) -> Unit)
	fun showProjectsSync()
	fun hideProjectsSync()
	fun cancelProjectsSync()
	suspend fun loadProjectMetadata(projectDef: ProjectDef): ProjectMetadata?
	fun onProjectNameUpdate(newProjectName: String)

	@Parcelize
	data class State(
		val projects: List<ProjectData> = mutableListOf(),
		val projectsPath: HPath,
		val isServerSynced: Boolean = false,
		val syncState: SyncState = SyncState(),
		val showCreateDialog: Boolean = false,
		val createDialogProjectName: String = "",
		val toast: StringResource? = null
	) : Parcelable

	@Parcelize
	data class SyncState(
		val showProjectSync: Boolean = false,
		val syncComplete: Boolean = false,
		val syncLog: List<SyncLogMessage> = emptyList(),
		val projectsStatus: Map<String, ProjectSyncStatus> = emptyMap()
	) : Parcelable

	@Parcelize
	data class ProjectSyncStatus(
		val projectName: String,
		val progress: Float = 0f,
		val status: Status = Status.Pending
	) : Parcelable

	enum class Status {
		Pending,
		Syncing,
		Failed,
		Complete,
		Canceled
	}
}