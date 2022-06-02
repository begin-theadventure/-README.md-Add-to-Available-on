package com.darkrockstudios.apps.hammer.common.projecteditor

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.RouterState
import com.arkivanov.decompose.router.activeChild
import com.arkivanov.decompose.router.popWhile
import com.arkivanov.decompose.router.router
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.darkrockstudios.apps.hammer.common.data.Scene
import com.darkrockstudios.apps.hammer.common.projecteditor.sceneeditor.SceneEditor
import com.darkrockstudios.apps.hammer.common.projecteditor.sceneeditor.SceneEditorComponent

internal class DetailsRouter(
    componentContext: ComponentContext,
    private val onFinished: () -> Unit
) {

    private val router =
        componentContext.router<Config, ProjectEditorRoot.Child.Detail>(
            initialConfiguration = Config.None,
            key = "DetailsRouter",
            childFactory = ::createChild
        )

    val state: Value<RouterState<Config, ProjectEditorRoot.Child.Detail>> = router.state

    private fun createChild(config: Config, componentContext: ComponentContext): ProjectEditorRoot.Child.Detail =
        when (config) {
            is Config.None -> ProjectEditorRoot.Child.Detail.None
            is Config.SceneEditor -> ProjectEditorRoot.Child.Detail.Editor(
                sceneEditor(componentContext = componentContext, scene = config.scene)
            )
        }

    private fun sceneEditor(componentContext: ComponentContext, scene: Scene): SceneEditor =
        SceneEditorComponent(
            componentContext = componentContext,
            scene = scene
        )

    fun showScene(scene: Scene) {
        router.navigate(
            transformer = { stack ->
                stack.dropLastWhile { it is Config.SceneEditor }
                    .plus(Config.SceneEditor(scene = scene))
            },
            onComplete = { _, _ -> }
        )
    }

    fun closeScene() {
        router.popWhile { it !is Config.None }
    }

    fun isShown(): Boolean =
        when (router.activeChild.configuration) {
            is Config.None -> false
            is Config.SceneEditor -> true
        }

    sealed class Config : Parcelable {
        @Parcelize
        object None : Config()

        @Parcelize
        data class SceneEditor(val scene: Scene) : Config()
    }
}