package com.darkrockstudios.apps.hammer.common.server

import com.darkrockstudios.apps.hammer.base.http.HEADER_SYNC_ID
import com.darkrockstudios.apps.hammer.base.http.HasProjectResponse
import com.darkrockstudios.apps.hammer.base.http.LoadSceneResponse
import com.darkrockstudios.apps.hammer.base.http.SaveSceneResponse
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.SceneItem
import com.darkrockstudios.apps.hammer.common.data.globalsettings.GlobalSettingsRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

class ServerProjectApi(
	httpClient: HttpClient,
	globalSettingsRepository: GlobalSettingsRepository
) : Api(httpClient, globalSettingsRepository) {

	suspend fun beginProjectSync(userId: Long, projectName: String): Result<String> {
		return get(
			path = "/project/$userId/$projectName/begin_sync",
			parse = { it.body() }
		)
	}

	suspend fun endProjectSync(userId: Long, projectName: String, syncId: String): Result<String> {
		return get(
			path = "/project/$userId/$projectName/end_sync",
			parse = { it.body() },
			builder = {
				headers {
					append(HEADER_SYNC_ID, syncId)
				}
			}
		)
	}

	suspend fun getProjectLastSync(userId: Long, projectName: String, syncId: String): Result<HasProjectResponse> {
		return get(
			path = "/project/$userId/$projectName/has_project",
			parse = { it.body() },
			builder = {
				headers {
					append(HEADER_SYNC_ID, syncId)
				}
			}
		)
	}

	suspend fun uploadScene(
		scene: SceneItem,
		path: List<Int>,
		content: String,
		syncId: String
	): Result<SaveSceneResponse> {
		val projectName = scene.projectDef.name
		return post(
			path = "/project/$userId/$projectName/upload_scene/${scene.id}",
			parse = { it.body() },
			builder = {
				setBody(
					FormDataContent(
						Parameters.build {
							append("sceneName", scene.name)
							append("sceneContent", content)
							append("sceneType", scene.type.name)
							append("sceneOrder", scene.order.toString())
							append("scenePath", path.joinToString("/"))
						}
					)
				)
				headers {
					append(HEADER_SYNC_ID, syncId)
				}
			}
		)
	}

	suspend fun downloadScene(projectDef: ProjectDef, sceneId: Int, syncId: String): Result<LoadSceneResponse> {
		val projectName = projectDef.name
		return post(
			path = "/project/$userId/$projectName/download_scene/$sceneId",
			parse = { it.body() },
			builder = {
				headers {
					append(HEADER_SYNC_ID, syncId)
				}
			}
		)
	}

	suspend fun setSyncData(projectDef: ProjectDef, syncId: String, lastId: Int): Result<String> {
		val projectName = projectDef.name
		return post(
			path = "/project/$userId/$projectName/set_sync_data",
			parse = { it.body() },
			builder = {
				setBody(
					FormDataContent(
						Parameters.build {
							append("lastId", lastId.toString())
						}
					)
				)
				headers {
					append(HEADER_SYNC_ID, syncId)
				}
			}
		)
	}
}
