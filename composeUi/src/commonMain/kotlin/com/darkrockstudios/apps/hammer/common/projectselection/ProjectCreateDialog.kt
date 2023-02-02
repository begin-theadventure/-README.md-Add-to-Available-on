package com.darkrockstudios.apps.hammer.common.projectselection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.darkrockstudios.apps.hammer.common.compose.MpDialog
import com.darkrockstudios.apps.hammer.common.compose.Ui

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectCreateDialog(show: Boolean, component: ProjectSelection, close: () -> Unit) {
	MpDialog(
		onCloseRequest = close,
		visible = show,
		title = "Create Project",
		modifier = Modifier.padding(Ui.Padding.XL)
	) {
		var newProjectNameText by remember { mutableStateOf("") }
		Box(modifier = Modifier.fillMaxWidth()) {
			Column(
				modifier = Modifier
					.width(IntrinsicSize.Max)
					.align(Alignment.Center)
			) {
				TextField(
					value = newProjectNameText,
					onValueChange = { newProjectNameText = it },
					label = { Text("New Project Name") },
				)

				Spacer(modifier = Modifier.size(Ui.Padding.L))

				Button(onClick = {
					component.createProject(newProjectNameText)
					newProjectNameText = ""
					close()
				}) {
					Text("Create Project")
				}
			}
		}
	}
}