package com.crownos.connect.media

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import com.crownos.connect.protocol.VideoCodec

object CodecProbe {
    private val preference = listOf(VideoCodec.Hevc, VideoCodec.H264)

    fun hardwareVideoCodecs(): List<VideoCodec> {
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { it.isHardwareAccelerated }
        return preference
            .filter { codec -> codecs.any { it.isEncoder && it.handles(codec) } && codecs.any { !it.isEncoder && it.handles(codec) } }
            .ifEmpty { listOf(VideoCodec.H264) }
    }

    fun encodesSize(width: Int, height: Int): Boolean =
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .filter { it.isEncoder && it.isHardwareAccelerated }
            .any { info ->
                preference.any { codec ->
                    info.handles(codec) && runCatching {
                        info.getCapabilitiesForType(codec.mime).videoCapabilities.isSizeSupported(width, height)
                    }.getOrDefault(false)
                }
            }

    private fun MediaCodecInfo.handles(codec: VideoCodec) = supportedTypes.any { it.equals(codec.mime, ignoreCase = true) }
}
