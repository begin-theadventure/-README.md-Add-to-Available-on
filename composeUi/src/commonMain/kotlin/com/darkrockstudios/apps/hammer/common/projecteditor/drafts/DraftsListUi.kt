package com.darkrockstudios.apps.hammer.common.projecteditor.drafts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.darkrockstudios.apps.hammer.common.compose.Ui
import com.darkrockstudios.apps.hammer.common.data.drafts.DraftDef
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun DraftsListUi(
	component: DraftsList,
	modifier: Modifier = Modifier,
) {
	val state by component.state.subscribeAsState()

	LaunchedEffect(state.sceneItem) {
		component.loadDrafts()
	}

	Column {
		Button(
			onClick = { component.cancel() },
			modifier = Modifier.align(Alignment.End)
		) {
			Text("X")
		}
		Text("${state.sceneItem.name} Drafts:")

		LazyColumn(
			modifier = modifier.fillMaxWidth(),
			contentPadding = PaddingValues(Ui.Padding.XL)
		) {
			state.apply {
				if (drafts.isEmpty()) {
					item {
						Text("No Drafts Found")
					}
				} else {
					items(drafts.size) { index ->
						DraftItem(draftDef = drafts[index], onDraftSelected = component::selectDraft)
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DraftItem(
	draftDef: DraftDef,
	modifier: Modifier = Modifier,
	onDraftSelected: (DraftDef) -> Unit
) {
	Card(
		modifier = modifier
			.fillMaxWidth()
			.padding(Ui.Padding.XL)
			.combinedClickable(
				onClick = { onDraftSelected(draftDef) },
			)
	) {
		Column {
			Text("${draftDef.draftTimestamp} - ${draftDef.draftName}")

			val localInstant = draftDef.draftTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())
			Text("Created: $localInstant")
		}
	}
}