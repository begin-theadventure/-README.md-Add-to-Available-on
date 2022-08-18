package com.darkrockstudios.apps.hammer.common.projecteditor.scenelist.scenetree

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.darkrockstudios.apps.hammer.common.data.SceneItem
import com.darkrockstudios.apps.hammer.common.tree.TreeValue

typealias ItemUi = @Composable (
    node: TreeValue<SceneItem>,
    toggleExpanded: (nodeId: Int) -> Unit,
    draggable: Modifier,
) -> Unit