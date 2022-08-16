package com.darkrockstudios.apps.hammer.common.data

import com.darkrockstudios.apps.hammer.common.tree.NodeCoordinates

data class MoveRequest(val id: Int, val position: InsertPosition)

data class InsertPosition(val coords: NodeCoordinates, val before: Boolean = true)