package com.crownos.connect.media

import com.crownos.connect.protocol.VideoLimits
import com.crownos.connect.util.ScreenSize
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamLimitsTest {
    @Test
    fun aPortraitPhoneKeepsItsShapeWithinTheLongSide() {
        val limits = StreamLimits.forScreen(ScreenSize(1080, 2400, 3f)) { _, _ -> true }
        assertEquals(VideoLimits(864, 1920, 60), limits)
    }

    @Test
    fun anUnsupportedShapeFallsBackToLandscapeHd() {
        val limits = StreamLimits.forScreen(ScreenSize(1080, 2400, 3f)) { width, _ -> width >= 1280 }
        assertEquals(VideoLimits(1920, 1080, 60), limits)
    }

    @Test
    fun smallScreensAreNotUpscaled() {
        assertEquals(VideoLimits(720, 1280, 60), StreamLimits.forScreen(ScreenSize(720, 1280, 2f)) { _, _ -> true })
    }
}
