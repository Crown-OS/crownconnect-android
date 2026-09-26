package com.crownos.connect.protocol

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

data class VideoLimits(val maxWidth: Int, val maxHeight: Int, val maxFps: Int) {
    fun toJson(): JsonObject = buildJsonObject {
        put("max_width", maxWidth)
        put("max_height", maxHeight)
        put("max_fps", maxFps)
    }

    companion object {
        fun parse(json: JsonObject?): VideoLimits? = json?.let {
            VideoLimits(
                it["max_width"]?.jsonPrimitive?.int ?: return null,
                it["max_height"]?.jsonPrimitive?.int ?: return null,
                it["max_fps"]?.jsonPrimitive?.int ?: return null,
            )
        }
    }
}

sealed interface FeatureRequest {
    val feature: Feature
    fun toJson(): JsonElement

    data class Mirror(val direction: Direction, val limits: VideoLimits, val remoteInput: Boolean) : FeatureRequest {
        override val feature get() = Feature.Mirror
        override fun toJson() = buildJsonObject {
            put("Mirror", buildJsonObject {
                put("direction", direction.name)
                put("limits", limits.toJson())
                put("remote_input", remoteInput)
            })
        }
    }

    data class Camera(val lens: CameraLens, val limits: VideoLimits) : FeatureRequest {
        override val feature get() = Feature.Camera
        override fun toJson() = buildJsonObject {
            put("Camera", buildJsonObject {
                put("lens", lens.name)
                put("limits", limits.toJson())
            })
        }
    }

    data object Mic : FeatureRequest {
        override val feature get() = Feature.Mic
        override fun toJson() = JsonPrimitive("Mic")
    }

    data class Monitor(
        val width: Int,
        val height: Int,
        val refreshMilliHz: Long,
        val scale120: Int,
        val placement: Edge,
    ) : FeatureRequest {
        override val feature get() = Feature.Monitor
        override fun toJson() = buildJsonObject {
            put("Monitor", buildJsonObject {
                put("width", width)
                put("height", height)
                put("refresh_mhz", refreshMilliHz)
                put("scale_120", scale120)
                put("placement", placement.name)
            })
        }
    }

    companion object {
        fun parse(json: JsonElement): FeatureRequest? {
            val body = json.variantBody()
            return when (json.variantName()) {
                "Mic" -> Mic
                "Mirror" -> body?.let {
                    Mirror(
                        enumByName<Direction>(it.string("direction")) ?: return null,
                        VideoLimits.parse(it["limits"]?.jsonObject) ?: return null,
                        it["remote_input"]?.jsonPrimitive?.boolean ?: false,
                    )
                }
                "Camera" -> body?.let {
                    Camera(
                        enumByName<CameraLens>(it.string("lens")) ?: return null,
                        VideoLimits.parse(it["limits"]?.jsonObject) ?: return null,
                    )
                }
                "Monitor" -> body?.let {
                    Monitor(
                        it["width"]?.jsonPrimitive?.int ?: return null,
                        it["height"]?.jsonPrimitive?.int ?: return null,
                        it["refresh_mhz"]?.jsonPrimitive?.long ?: return null,
                        it["scale_120"]?.jsonPrimitive?.int ?: 120,
                        enumByName<Edge>(it.string("placement")) ?: Edge.Right,
                    )
                }
                else -> null
            }
        }
    }
}
