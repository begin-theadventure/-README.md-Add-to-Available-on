package com.darkrockstudios.apps.hammer.common.components.projectsync

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.getAndUpdate
import com.darkrockstudios.apps.hammer.base.http.ApiProjectEntity
import com.darkrockstudios.apps.hammer.common.components.ProjectComponentBase
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.drafts.SceneDraftRepository
import com.darkrockstudios.apps.hammer.common.data.encyclopediarepository.EncyclopediaRepository
import com.darkrockstudios.apps.hammer.common.data.globalsettings.GlobalSettingsRepository
import com.darkrockstudios.apps.hammer.common.data.notesrepository.NotesRepository
import com.darkrockstudios.apps.hammer.common.data.projectInject
import com.darkrockstudios.apps.hammer.common.data.projecteditorrepository.ProjectEditorRepository
import com.darkrockstudios.apps.hammer.common.data.projectsync.*
import com.darkrockstudios.apps.hammer.common.data.timelinerepository.TimeLineRepository
import com.darkrockstudios.apps.hammer.common.dependencyinjection.injectMainDispatcher
import com.soywiz.krypto.encoding.Base64
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class ProjectSyncComponent(
	componentContext: ComponentContext,
	projectDef: ProjectDef,
	private val dismissSync: () -> Unit
) : ProjectComponentBase(projectDef, componentContext), ProjectSync {

	private val mainDispatcher by injectMainDispatcher()

	private val globalSettingsRepository: GlobalSettingsRepository by inject()
	private val projectEditorRepository: ProjectEditorRepository by projectInject()
	private val encyclopediaRepository: EncyclopediaRepository by projectInject()
	private val notesRepository: NotesRepository by projectInject()
	private val timeLineRepository: TimeLineRepository by projectInject()
	private val sceneDraftRepository: SceneDraftRepository by projectInject()
	private val projectSynchronizer: ClientProjectSynchronizer by projectInject()

	private var syncJob: Job? = null

	private val _state = MutableValue(
		ProjectSync.State()
	)
	override val state: Value<ProjectSync.State> = _state

	private suspend fun updateSyncLog(log: SyncLogMessage?) {
		if (log != null) {
			Napier.log(log.level.toNapierLevel(), "ProjectSync", null, "${log.projectName} - ${log.message}")
			withContext(mainDispatcher) {
				_state.getAndUpdate {
					val existingLog = it.syncLog
					it.copy(
						syncLog = existingLog + log
					)
				}
			}
		}
	}

	private suspend fun updateSync(isSyncing: Boolean, progress: Float, log: SyncLogMessage? = null) {
		updateSyncLog(log)

		withContext(mainDispatcher) {
			_state.getAndUpdate {
				it.copy(
					isSyncing = isSyncing,
					syncProgress = progress
				)
			}
		}
	}

	override fun syncProject(onComplete: (Boolean) -> Unit) {
		syncJob?.cancel(CancellationException("Starting another sync"))
		syncJob = scope.launch {
			updateSync(true, 0f, syncLogI("Project Sync Started", projectDef))
			val success = projectSynchronizer.sync(::onSyncProgress, ::updateSyncLog, ::onConflict, ::onSyncComplete)

			_state.getAndUpdate {
				it.copy(
					failed = !success
				)
			}

			// Auto-close dialog on success
			if (success && globalSettingsRepository.globalSettings.autoCloseSyncDialog) {
				endSync()
			} else {
				if (!success) {
					showLog(true)
				}
			}

			onComplete(success)
		}
	}

	override fun resolveConflict(resolvedEntity: ApiProjectEntity) {
		projectSynchronizer.resolveConflict(resolvedEntity)

		_state.getAndUpdate {
			it.copy(
				entityConflict = null
			)
		}
	}

	override fun endSync() {
		scope.launch {
			syncJob = null
			withContext(mainDispatcher) {
				_state.getAndUpdate {
					it.copy(
						entityConflict = null,
						isSyncing = false,
						syncProgress = 0f,
						syncLog = emptyList()
					)
				}

				dismissSync()
			}
		}
	}

	override fun cancelSync() {
		scope.launch {
			syncJob?.cancel(CancellationException("User canceled sync"))
			syncJob = null

			updateSyncLog(syncLogW("User canceled project sync", projectDef))

			withContext(mainDispatcher) {
				_state.getAndUpdate {
					it.copy(
						entityConflict = null,
						isSyncing = false,
					)
				}
			}
		}
	}

	override fun showLog(show: Boolean) {
		_state.getAndUpdate {
			it.copy(
				showLog = show
			)
		}
	}

	private suspend fun onSyncProgress(progress: Float, log: SyncLogMessage? = null) {
		Napier.d("Sync progress: $progress")
		updateSync(true, progress, log)
	}

	private suspend fun onConflict(serverEntity: ApiProjectEntity) {
		Napier.d("Sync conflict")

		when (serverEntity) {
			is ApiProjectEntity.SceneEntity -> onSceneConflict(serverEntity)
			is ApiProjectEntity.NoteEntity -> onNoteConflict(serverEntity)
			is ApiProjectEntity.TimelineEventEntity -> onTimelineEventConflict(serverEntity)
			is ApiProjectEntity.EncyclopediaEntryEntity -> onEncyclopediaEntryConflict(serverEntity)
			is ApiProjectEntity.SceneDraftEntity -> onSceneDraftConflict(serverEntity)
		}
	}

	private suspend fun onNoteConflict(serverEntity: ApiProjectEntity.NoteEntity) {
		val local = notesRepository.getNoteFromId(serverEntity.id)?.note
			?: throw IllegalStateException("Failed to get local note")

		val localEntity = ApiProjectEntity.NoteEntity(
			id = local.id,
			created = local.created,
			content = local.content
		)

		withContext(mainDispatcher) {
			_state.getAndUpdate {
				it.copy(
					entityConflict = ProjectSync.EntityConflict.NoteConflict(
						serverNote = serverEntity,
						clientNote = localEntity
					)
				)
			}
		}
	}

	private suspend fun onTimelineEventConflict(serverEntity: ApiProjectEntity.TimelineEventEntity) {
		val local = timeLineRepository.getTimelineEvent(serverEntity.id)
			?: throw IllegalStateException("Failed to get local note")

		val localEntity = ApiProjectEntity.TimelineEventEntity(
			id = local.id,
			date = local.date,
			content = local.content,
			order = local.order
		)

		withContext(mainDispatcher) {
			_state.getAndUpdate {
				it.copy(
					entityConflict = ProjectSync.EntityConflict.TimelineEventConflict(
						serverEvent = serverEntity,
						clientEvent = localEntity
					)
				)
			}
		}
	}

	private suspend fun onEncyclopediaEntryConflict(serverEntity: ApiProjectEntity.EncyclopediaEntryEntity) {
		val local = encyclopediaRepository.loadEntry(serverEntity.id).entry
		val def = local.toDef(projectDef)
		val image = if (encyclopediaRepository.hasEntryImage(def, "jpg")) {
			val imageBytes = encyclopediaRepository.loadEntryImage(def, "jpg")
			val imageBase64 = Base64.encode(imageBytes, url = true)
			ApiProjectEntity.EncyclopediaEntryEntity.Image(imageBase64, "jpg")
		} else {
			null
		}

		val localEntity = ApiProjectEntity.EncyclopediaEntryEntity(
			id = local.id,
			name = local.name,
			entryType = local.type.name,
			text = local.text,
			tags = local.tags,
			image = image
		)

		withContext(mainDispatcher) {
			_state.getAndUpdate {
				it.copy(
					entityConflict = ProjectSync.EntityConflict.EncyclopediaEntryConflict(
						serverEntry = serverEntity,
						clientEntry = localEntity
					)
				)
			}
		}
	}

	private suspend fun onSceneDraftConflict(serverEntity: ApiProjectEntity.SceneDraftEntity) {
		val local = sceneDraftRepository.getDraftDef(serverEntity.id)
			?: throw IllegalStateException("Failed to get local note")
		val localContent = sceneDraftRepository.loadDraftRaw(local)
			?: throw IllegalStateException("Failed to load local draft content")

		val localEntity = ApiProjectEntity.SceneDraftEntity(
			id = local.id,
			name = local.draftName,
			sceneId = local.sceneId,
			created = local.draftTimestamp,
			content = localContent
		)

		withContext(mainDispatcher) {
			_state.getAndUpdate {
				it.copy(
					entityConflict = ProjectSync.EntityConflict.SceneDraftConflict(
						serverEntry = serverEntity,
						clientEntry = localEntity
					)
				)
			}
		}
	}

	private suspend fun onSyncComplete() {
		updateSyncLog(syncLogI("Sync complete!", projectDef))
		updateSync(false, 1f)
	}

	private suspend fun onSceneConflict(serverEntity: ApiProjectEntity.SceneEntity) {
		val local = projectEditorRepository.getSceneItemFromId(serverEntity.id)
			?: throw IllegalStateException("Failed to get local scene")

		val path = projectEditorRepository.getPathSegments(local)
		val content = projectEditorRepository.loadSceneMarkdownRaw(local)

		val localEntity = ApiProjectEntity.SceneEntity(
			id = local.id,
			sceneType = local.type.toApiType(),
			name = local.name,
			order = local.order,
			content = content,
			path = path
		)

		withContext(mainDispatcher) {
			_state.getAndUpdate {
				it.copy(
					entityConflict = ProjectSync.EntityConflict.SceneConflict(
						serverScene = serverEntity,
						clientScene = localEntity
					)
				)
			}
		}
	}
}