package com.darkrockstudios.apps.hammer.common.projecteditor

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.childAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.animation.child.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.router.RouterState
import com.arkivanov.decompose.value.Value
import com.darkrockstudios.apps.hammer.common.projecteditor.sceneeditor.SceneEditorUi
import com.darkrockstudios.apps.hammer.common.projecteditor.scenelist.SceneListUi

private val MULTI_PANE_WIDTH_THRESHOLD = 800.dp
private const val LIST_PANE_WEIGHT = 0.4F
private const val DETAILS_PANE_WEIGHT = 0.6F

@Composable
fun ProjectEditorUi(
    component: ProjectEditorComponent,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val state by component.state.subscribeAsState()
        val isMultiPane = state.isMultiPane

        Row(modifier = Modifier.fillMaxSize()) {
            ListPane(
                routerState = component.listRouterState,
                modifier = Modifier.weight(if (isMultiPane) LIST_PANE_WEIGHT else 1F),
            )

            if (isMultiPane) {
                Box(modifier = Modifier.weight(DETAILS_PANE_WEIGHT))
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            if (isMultiPane) {
                Box(modifier = Modifier.weight(LIST_PANE_WEIGHT))
            }

            DetailsPane(
                routerState = component.detailsRouterState,
                modifier = Modifier.weight(if (isMultiPane) DETAILS_PANE_WEIGHT else 1F),
            )
        }

        val isMultiPaneRequired = this@BoxWithConstraints.maxWidth >= MULTI_PANE_WIDTH_THRESHOLD

        DisposableEffect(isMultiPaneRequired) {
            component.setMultiPane(isMultiPaneRequired)
            onDispose {}
        }
    }
}

@OptIn(ExperimentalDecomposeApi::class)
@Composable
private fun ListPane(routerState: Value<RouterState<*, ProjectEditorRoot.Child.List>>, modifier: Modifier) {
    Children(
        routerState = routerState,
        modifier = modifier,
        animation = childAnimation(fade()),
    ) {
        when (val child = it.instance) {
            is ProjectEditorRoot.Child.List.Scenes ->
                SceneListUi(
                    component = child.component,
                    modifier = Modifier.fillMaxSize()
                )
            is ProjectEditorRoot.Child.List.None -> Box {}
        }
    }
}

@OptIn(ExperimentalDecomposeApi::class)
@Composable
private fun DetailsPane(routerState: Value<RouterState<*, ProjectEditorRoot.Child.Detail>>, modifier: Modifier) {
    Children(
        routerState = routerState,
        modifier = modifier,
        animation = childAnimation(fade()),
    ) {
        when (val child = it.instance) {
            is ProjectEditorRoot.Child.Detail.None -> Box {}
            is ProjectEditorRoot.Child.Detail.Editor ->
                SceneEditorUi(
                    component = child.component,
                    modifier = Modifier.fillMaxSize()
                )
        }
    }
}