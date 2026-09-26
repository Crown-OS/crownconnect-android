package com.crownos.connect.unicursor

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.crownos.connect.R
import com.crownos.connect.input.AccessibilityBridge
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val ARROW_TIP_OFFSET_DP = 1.5f

@Singleton
class CursorOverlay @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accessibility: AccessibilityBridge,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var host: FrameLayout? = null
    private var arrow: ImageView? = null
    private var windowManager: WindowManager? = null

    val canShow: Boolean get() = accessibility.isConnected || Settings.canDrawOverlays(context)

    fun show(x: Float, y: Float) = mainHandler.post {
        if (host == null) attach()
        moveArrow(x, y)
        arrow?.visibility = View.VISIBLE
    }

    fun moveTo(x: Float, y: Float) = mainHandler.post { moveArrow(x, y) }

    fun hide() = mainHandler.post {
        val manager = windowManager
        host?.let { runCatching { manager?.removeView(it) } }
        host = null
        arrow = null
        windowManager = null
    }

    private fun moveArrow(x: Float, y: Float) {
        val view = arrow ?: return
        val offset = ARROW_TIP_OFFSET_DP * view.resources.displayMetrics.density
        view.translationX = x - offset
        view.translationY = y - offset
    }

    private fun attach() {
        val service = accessibility.service
        val (ownerContext, type) = if (service != null) {
            service to WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            context to WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
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
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        runCatching { manager.addView(frame, parameters) }.onSuccess {
            host = frame
            arrow = cursor
            windowManager = manager
        }
    }
}
