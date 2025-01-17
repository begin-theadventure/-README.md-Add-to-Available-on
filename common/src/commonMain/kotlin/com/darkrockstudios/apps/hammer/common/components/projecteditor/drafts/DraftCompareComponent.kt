package com.darkrockstudios.apps.hammer.common.components.projecteditor.drafts

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.getAndUpdate
import com.darkrockstudios.apps.hammer.common.components.ProjectComponentBase
import com.darkrockstudios.apps.hammer.common.data.SceneItem
import com.darkrockstudios.apps.hammer.common.data.UpdateSource
import com.darkrockstudios.apps.hammer.common.data.drafts.DraftDef
import com.darkrockstudios.apps.hammer.common.data.drafts.SceneDraftRepository
import com.darkrockstudios.apps.hammer.common.data.projectInject
import com.darkrockstudios.apps.hammer.common.data.projecteditorrepository.ProjectEditorRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DraftCompareComponent(
	componentContext: ComponentContext,
	override val sceneItem: SceneItem,
	override val draftDef: DraftDef,
	private val cancelCompare: () -> Unit,
	private val backToEditor: () -> Unit
) : ProjectComponentBase(sceneItem.projectDef, componentContext), DraftCompare {

	private val draftsRepository: SceneDraftRepository by projectInject()
	private val projectEditor: ProjectEditorRepository by projectInject()

	private val _state = MutableValue(
		DraftCompare.State(
			sceneItem = sceneItem,
			draftDef = draftDef
		)
	)
	override val state: Value<DraftCompare.State> = _state

	override fun loadContents() {
		scope.launch {
			val currentBuffer = projectEditor.loadSceneBuffer(sceneItem)
			val draftContent = draftsRepository.loadDraft(sceneItem, draftDef)

			withContext(dispatcherMain) {
				_state.getAndUpdate {
					it.copy(
						sceneContent = currentBuffer.content,
						draftContent = draftContent
					)
				}
			}
		}
	}

	override fun pickMerged() {
		val content = state.value.sceneContent
		if (content != null) {
			projectEditor.onContentChanged(content, UpdateSource.Drafts)
			backToEditor()
		} else {
			Napier.e { "Cannot pick merged, merged content was NULL" }
		}
	}

	override fun pickDraft() {
		val content = state.value.draftContent
		if (content != null) {
			projectEditor.onContentChanged(content, UpdateSource.Drafts)
			backToEditor()
		} else {
			Napier.e { "Cannot pick draft, draft content was NULL" }
		}
	}

	override fun cancel() {
		cancelCompare()
	}
}