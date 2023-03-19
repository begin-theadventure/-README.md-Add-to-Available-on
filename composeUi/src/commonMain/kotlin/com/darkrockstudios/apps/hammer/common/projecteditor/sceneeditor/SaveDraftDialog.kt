package com.darkrockstudios.apps.hammer.common.projecteditor.sceneeditor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.darkrockstudios.apps.hammer.common.components.projecteditor.sceneeditor.SceneEditor
import com.darkrockstudios.apps.hammer.common.compose.MpDialog
import com.darkrockstudios.apps.hammer.common.compose.Ui

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SaveDraftDialog(
	state: SceneEditor.State,
	component: SceneEditor,
	showSnackbar: (message: String) -> Unit
) {
	var draftName by remember { mutableStateOf("") }

	MpDialog(
		visible = state.isSavingDraft,
		title = "Save Draft:",
		onCloseRequest = {
			component.endSaveDraft()
			draftName = ""
		}
	) {
		Box(modifier = Modifier.fillMaxWidth()) {
			Column(
				modifier = Modifier
					.width(IntrinsicSize.Max)
					.padding(Ui.Padding.XL)
					.align(Alignment.Center)
			) {
				TextField(
					value = draftName,
					onValueChange = { draftName = it },
					singleLine = true,
					placeholder = { Text("Draft name") }
				)

				Spacer(modifier = Modifier.size(Ui.Padding.XL))

				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Button(onClick = {
						if (component.saveDraft(draftName)) {
							component.endSaveDraft()
							draftName = ""
							showSnackbar("Draft Saved")
						}
					}) {
						Text("Save")
					}
					Button(onClick = {
						component.endSaveDraft()
						draftName = ""
					}) {
						Text("Cancel")
					}
				}
			}
		}
	}
}