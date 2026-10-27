package com.crownos.connect.util

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ScreenSize(val width: Int, val height: Int, val density: Float)

@Singleton
class ScreenGeometry @Inject constructor(@ApplicationContext context: Context) {
    private val displays = context.getSystemService(DisplayManager::class.java)

    @Volatile
    var size: ScreenSize = measure()
        private set

    val peakRefreshRate: Float
        get() = displays.getDisplay(Display.DEFAULT_DISPLAY)?.supportedModes?.maxOfOrNull { it.refreshRate } ?: 0f

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) size = measure()
        }

        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
    }

    init {
        displays.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
    }

    private fun measure(): ScreenSize {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        displays.getDisplay(Display.DEFAULT_DISPLAY)?.getRealMetrics(metrics)
        return ScreenSize(metrics.widthPixels.coerceAtLeast(1), metrics.heightPixels.coerceAtLeast(1), metrics.density.coerceAtLeast(1f))
    }
}
