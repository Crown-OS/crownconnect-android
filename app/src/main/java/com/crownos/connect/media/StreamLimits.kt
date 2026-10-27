package com.crownos.connect.media

import com.crownos.connect.protocol.VideoLimits
import com.crownos.connect.util.ScreenSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val LONG_SIDE = 1920
private const val SHORT_SIDE = 1080
private const val ALIGNMENT = 16
const val MIRROR_FPS = 60

object StreamLimits {
    fun forScreen(screen: ScreenSize, supported: (Int, Int) -> Boolean): VideoLimits {
        val scale = min(1f, LONG_SIDE.toFloat() / max(screen.width, screen.height))
        val width = align(screen.width * scale)
        val height = align(screen.height * scale)
        if (supported(width, height)) return VideoLimits(width, height, MIRROR_FPS)
        return VideoLimits(LONG_SIDE, SHORT_SIDE, MIRROR_FPS)
    }

    val forViewing = VideoLimits(LONG_SIDE, SHORT_SIDE, MIRROR_FPS)

    private fun align(value: Float): Int = max(ALIGNMENT, (value / ALIGNMENT).roundToInt() * ALIGNMENT)
}
