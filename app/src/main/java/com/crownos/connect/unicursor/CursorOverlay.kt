package com.crownos.connect.unicursor

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Choreographer
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.crownos.connect.R
import com.crownos.connect.input.AccessibilityBridge
import com.crownos.connect.util.ScreenGeometry
import com.crownos.connect.util.ScreenPoint
import com.crownos.connect.util.VsyncCoalescer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val ARROW_TIP_OFFSET_DP = 1.5f

@Singleton
class CursorOverlay @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accessibility: AccessibilityBridge,
    private val screen: ScreenGeometry,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var host: FrameLayout? = null
    private var arrow: ImageView? = null
    private var windowManager: WindowManager? = null
    private var tipOffset = 0f

    @Volatile
    private var target = ScreenPoint(0f, 0f)

    @Volatile
    private var frames: VsyncCoalescer? = null

    val canShow: Boolean get() = accessibility.isConnected || Settings.canDrawOverlays(context)

    fun show(point: ScreenPoint) {
        target = point
        mainHandler.post {
            if (host == null) attach()
            placeArrow()
        }
    }

    fun moveTo(point: ScreenPoint) {
        target = point
        frames?.request()
    }

    fun hide() {
        mainHandler.post {
            frames?.cancel()
            frames = null
            val manager = windowManager
            host?.let { runCatching { manager?.removeView(it) } }
            host = null
            arrow = null
            windowManager = null
        }
    }

    private fun placeArrow() {
        val view = arrow ?: return
        val point = target
        view.translationX = point.x - tipOffset
        view.translationY = point.y - tipOffset
    }

    private fun attach() {
        val owner = accessibility.overlayOwner(context)
        val ownerContext = owner.context
        val manager = ownerContext.getSystemService(WindowManager::class.java)
        val cursor = ImageView(ownerContext).apply {
            setImageResource(R.drawable.cursor_arrow)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        val frame = FrameLayout(ownerContext).apply {
            addView(cursor, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        }
        val parameters = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            owner.windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            preferredRefreshRate = screen.peakRefreshRate
        }
        runCatching { manager.addView(frame, parameters) }.onSuccess {
            host = frame
            arrow = cursor
            windowManager = manager
            tipOffset = ARROW_TIP_OFFSET_DP * cursor.resources.displayMetrics.density
            frames = VsyncCoalescer(Choreographer.getInstance(), ::placeArrow)
        }
    }
}
