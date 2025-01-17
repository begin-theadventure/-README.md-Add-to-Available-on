package com.darkrockstudios.apps.hammer.common.projecteditor.drafts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.darkrockstudios.apps.hammer.MR
import com.darkrockstudios.apps.hammer.common.components.projecteditor.drafts.DraftCompare
import com.darkrockstudios.apps.hammer.common.compose.LocalScreenCharacteristic
import com.darkrockstudios.apps.hammer.common.compose.Ui
import com.darkrockstudios.apps.hammer.common.compose.moko.get
import com.darkrockstudios.apps.hammer.common.compose.rememberStrRes
import com.darkrockstudios.apps.hammer.common.projecteditor.sceneeditor.getInitialEditorContent
import com.darkrockstudios.richtexteditor.ui.RichTextEditor
import com.darkrockstudios.richtexteditor.ui.defaultRichTextFieldStyle

@Composable
fun DraftCompareUi(component: DraftCompare) {
	val screen = LocalScreenCharacteristic.current

	LaunchedEffect(component.sceneItem) {
		component.loadContents()
	}

	Column(modifier = Modifier.fillMaxSize()) {
		if (LocalScreenCharacteristic.current.needsExplicitClose) {
			IconButton(
				onClick = { component.cancel() },
				modifier = Modifier.align(Alignment.End)
			) {
				Icon(
					Icons.Default.Cancel,
					contentDescription = MR.strings.draft_compare_cancel_button.get(),
					tint = MaterialTheme.colorScheme.onBackground
				)
			}
		}

		val content = remember {
			movableContentOf {
				DraftContent(
					modifier = Modifier.weight(1f),
					component = component,
				)

				CurrentContent(
					modifier = Modifier.weight(1f),
					component = component
				)
			}
		}

		if (screen.windowWidthClass != WindowWidthSizeClass.Compact) {
			Row(modifier = Modifier.fillMaxSize()) {
				content()
			}
		} else {
			Column(modifier = Modifier.fillMaxSize()) {
				content()
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrentContent(
	modifier: Modifier,
	component: DraftCompare
) {
	val state by component.state.subscribeAsState()
	var sceneText by remember(state.sceneContent) { mutableStateOf(getInitialEditorContent(state.sceneContent)) }

	Card(modifier = modifier.padding(Ui.Padding.L)) {
		Column(modifier = Modifier.padding(Ui.Padding.L)) {
			Text(
				MR.strings.draft_compare_current_header.get(),
				style = MaterialTheme.typography.headlineLarge
			)
			Text(
				MR.strings.draft_compare_current_subheader.get(),
				style = MaterialTheme.typography.bodySmall,
				fontStyle = FontStyle.Italic
			)

			Button(onClick = { component.pickMerged() }) {
				Text(MR.strings.draft_compare_current_accept_button.get())
			}

			RichTextEditor(
				modifier = Modifier.fillMaxSize(),
				value = sceneText,
				onValueChange = { rtv ->
					sceneText = rtv
				},
				textFieldStyle = defaultRichTextFieldStyle().copy(
					placeholder = MR.strings.draft_compare_current_body_placeholder.get(),
					textColor = MaterialTheme.colorScheme.onBackground,
					placeholderColor = MaterialTheme.colorScheme.onBackground,
				)
			)
		}
	}
}

@Composable
private fun DraftContent(
	modifier: Modifier,
	component: DraftCompare,
) {
	val strRes = rememberStrRes()
	val state by component.state.subscribeAsState()
	var draftText by remember(state.draftContent) { mutableStateOf(getInitialEditorContent(state.draftContent)) }

	Card(modifier = modifier.padding(Ui.Padding.L)) {
		Column(modifier = Modifier.padding(Ui.Padding.L)) {

			/*
			val date = remember(component.draftDef.draftTimestamp) {
				val created = component.draftDef.draftTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())
				created.format("dd MMM `yy")
			}
			*/

			Text(
				strRes.get(MR.strings.draft_compare_draft_header, component.draftDef.draftName),
				style = MaterialTheme.typography.headlineLarge
			)
			Text(
				MR.strings.draft_compare_draft_subheader.get(),
				style = MaterialTheme.typography.bodySmall,
				fontStyle = FontStyle.Italic
			)
			Button(onClick = { component.pickDraft() }) {
				Text(MR.strings.draft_compare_draft_accept_button.get())
			}

			RichTextEditor(
				modifier = Modifier.fillMaxSize(),
				value = draftText,
				onValueChange = { rtv ->
					draftText = rtv
				},
				textFieldStyle = defaultRichTextFieldStyle().copy(
					placeholder = MR.strings.draft_compare_draft_body_placeholder.get(),
					textColor = MaterialTheme.colorScheme.onBackground,
					placeholderColor = MaterialTheme.colorScheme.onBackground,
				),
				readOnly = true
			)
		}
	}
}