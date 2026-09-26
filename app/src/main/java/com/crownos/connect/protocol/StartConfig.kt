package com.crownos.connect.protocol

import kotlinx.serialization.Serializable

const val DEFAULT_LLTS_PORT = 47_470

@Serializable
data class StartConfig(
    val name: String,
    val model: String,
    val `class`: DeviceClass,
    val port: Int = DEFAULT_LLTS_PORT,
    val videoCodecs: List<VideoCodec>,
    val audioCodecs: List<String> = listOf("Opus"),
    val features: List<Feature> = Feature.entries,
) {
    fun toJson(): String = ProtocolJson.encodeToString(serializer(), this)
}
