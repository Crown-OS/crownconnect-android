package com.crownos.connect.util

import android.util.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaptureSizesTest {
    private val camera = listOf(Size(4000, 3000), Size(1920, 1080), Size(1440, 1080), Size(640, 480))

    @Test
    fun theSmallestCoveringSizeWithTheSameShapeWins() {
        assertEquals(Size(1920, 1080), CaptureSizes.closest(camera, Size(1600, 900)))
        assertEquals(Size(1440, 1080), CaptureSizes.closest(camera, Size(1024, 768)))
    }

    @Test
    fun tooLargeARequestFallsBackToTheLargestSize() {
        assertEquals(Size(4000, 3000), CaptureSizes.closest(camera, Size(7680, 4320)))
        assertNull(CaptureSizes.closest(emptyList(), Size(1280, 720)))
    }

    @Test
    fun theCropKeepsTheTargetShape() {
        assertCrop(0.75f to 1f, CaptureSizes.cropScale(Size(1920, 1080), Size(1280, 960)))
        assertCrop(1f to 0.75f, CaptureSizes.cropScale(Size(1440, 1080), Size(1280, 720)))
        assertCrop(1f to 1f, CaptureSizes.cropScale(Size(1920, 1080), Size(1280, 720)))
    }

    private fun assertCrop(expected: Pair<Float, Float>, actual: Pair<Float, Float>) {
        assertEquals(expected.first, actual.first, 1e-5f)
        assertEquals(expected.second, actual.second, 1e-5f)
    }
}
