package com.crownos.connect.media

import androidx.compose.runtime.Immutable
import com.crownos.connect.llts.LltsEvent
import com.crownos.connect.protocol.Feature
import com.crownos.connect.protocol.FeatureRequest
import com.crownos.connect.protocol.MediaRole
import com.crownos.connect.protocol.enumByName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
data class MediaStream(
    val peer: String,
    val feature: Feature,
    val role: MediaRole,
    val index: Int,
    val isVideo: Boolean,
    val width: Int,
    val height: Int,
    val fps: Int,
    val request: FeatureRequest?,
) {
    val key: String get() = "$peer/$index"

    companion object {
        fun from(event: LltsEvent.MediaStart): MediaStream? {
            val feature = enumByName<Feature>(event.feature) ?: return null
            val role = enumByName<MediaRole>(event.role) ?: return null
            return MediaStream(
                peer = event.peer,
                feature = feature,
                role = role,
                index = event.stream.index,
                isVideo = event.params.kind == "video",
                width = event.params.width,
                height = event.params.height,
                fps = (event.params.fpsMhz / 1000).toInt().coerceAtLeast(1),
                request = FeatureRequest.parse(event.request),
            )
        }
    }
}

@Singleton
class MediaStreams @Inject constructor() {
    private val streamsState = MutableStateFlow<Map<String, MediaStream>>(emptyMap())
    val streams: StateFlow<Map<String, MediaStream>> = streamsState.asStateFlow()

    fun started(stream: MediaStream) = streamsState.update { it + (stream.key to stream) }

    fun stopped(peer: String, index: Int): MediaStream? {
        var removed: MediaStream? = null
        streamsState.update { current ->
            removed = current["$peer/$index"]
            current - "$peer/$index"
        }
        return removed
    }

    fun forPeer(peer: String): List<MediaStream> = streamsState.value.values.filter { it.peer == peer }

    fun find(peer: String, index: Int): MediaStream? = streamsState.value["$peer/$index"]

    fun clearPeer(peer: String): List<MediaStream> {
        val removed = forPeer(peer)
        streamsState.update { current -> current.filterValues { it.peer != peer } }
        return removed
    }
}
