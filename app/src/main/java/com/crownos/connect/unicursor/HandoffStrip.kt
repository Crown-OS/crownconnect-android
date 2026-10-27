package com.crownos.connect.unicursor

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.crownos.connect.input.OverlayOwner
import com.crownos.connect.protocol.Edge
import com.crownos.connect.util.EdgeGeometry
import com.crownos.connect.util.ScreenGeometry
import com.crownos.connect.util.ScreenPoint
import kotlin.math.max

private const val TAG = "HandoffStrip"
private const val FOCUS_TIMEOUT_MILLIS = 400L
private const val THICKNESS_DP = 1f
private const val MIN_THICKNESS_PX = 2

/** A hairline window on the edge facing a computer; the mouse reaching it hands the cursor over. */
@SuppressLint("ViewConstructor")
class HandoffStrip private constructor(
    owner: OverlayOwner,
    val peer: String,
    private val edge: Edge,
    private val screen: ScreenGeometry,
    private val listener: Listener,
) : View(owner.context) {
    interface Listener {
        fun onEntered(strip: HandoffStrip, edge: Edge, fraction: Float)
        fun onInput(strip: HandoffStrip, moment: String)
    }

    private enum class Phase { Idle, Grabbing, OnComputer }

    private val windows = owner.context.getSystemService(WindowManager::class.java)
    private val layout = stripLayout(owner.windowType, edge, max(MIN_THICKNESS_PX, (THICKNESS_DP * resources.displayMetrics.density).toInt()))
    private val sender = CapturedInputSender { listener.onInput(this, it) }
    private val focusTimeout = Runnable {
        Log.w(TAG, "the handoff strip never got focus")
        settle()
    }
    private var phase = Phase.Idle
    private var armed = true
    private var entryFraction = 0f

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        if (!event.isFromPhysicalMouse()) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_EXIT -> armed = true
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> if (armed && phase == Phase.Idle) grab(event)
        }
        return true
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus && phase == Phase.Grabbing) requestPointerCapture()
    }

    override fun onPointerCaptureChange(hasCapture: Boolean) {
        super.onPointerCaptureChange(hasCapture)
        when {
            hasCapture && phase == Phase.Grabbing -> {
                removeCallbacks(focusTimeout)
                phase = Phase.OnComputer
                listener.onEntered(this, edge, entryFraction)
            }
            !hasCapture && phase == Phase.OnComputer -> settle()
        }
    }

    override fun onCapturedPointerEvent(event: MotionEvent): Boolean {
        if (phase == Phase.OnComputer) sender.motion(event)
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (phase != Phase.OnComputer) return super.dispatchKeyEvent(event)
        when (event.action) {
            KeyEvent.ACTION_DOWN -> sender.key(event.keyCode, true)
            KeyEvent.ACTION_UP -> sender.key(event.keyCode, false)
        }
        return true
    }

    fun handBack() {
        if (phase != Phase.Idle) settle()
    }

    fun detach() {
        settle()
        runCatching { windows.removeView(this) }
    }

    private fun addToScreen(): Boolean = runCatching { windows.addView(this, layout) }
        .onFailure { Log.w(TAG, "cannot place the handoff strip: ${it.message}") }
        .isSuccess

    private fun grab(event: MotionEvent) {
        armed = false
        phase = Phase.Grabbing
        val size = screen.size
        entryFraction = EdgeGeometry.fractionAlong(edge, ScreenPoint(event.rawX, event.rawY), size.width, size.height)
        setWindowFocusable(true)
        requestFocus()
        if (hasWindowFocus()) requestPointerCapture()
        postDelayed(focusTimeout, FOCUS_TIMEOUT_MILLIS)
    }

    private fun settle() {
        removeCallbacks(focusTimeout)
        if (phase == Phase.OnComputer) sender.releaseAll()
        phase = Phase.Idle
        if (hasPointerCapture()) releasePointerCapture()
        setWindowFocusable(false)
    }

    private fun setWindowFocusable(focusable: Boolean) {
        val flags = if (focusable) {
            layout.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            layout.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        if (flags == layout.flags) return
        layout.flags = flags
        runCatching { windows.updateViewLayout(this, layout) }
    }

    private fun MotionEvent.isFromPhysicalMouse(): Boolean {
        val source = device ?: return false
        return !source.isVirtual && source.supportsSource(InputDevice.SOURCE_MOUSE) && isFromSource(InputDevice.SOURCE_MOUSE)
    }

    companion object {
        fun attach(owner: OverlayOwner, peer: String, edge: Edge, screen: ScreenGeometry, listener: Listener): HandoffStrip? =
            HandoffStrip(owner, peer, edge, screen, listener).takeIf { it.addToScreen() }

        @SuppressLint("RtlHardcoded")
        private fun stripLayout(type: Int, edge: Edge, thickness: Int): WindowManager.LayoutParams {
            val vertical = edge == Edge.Left || edge == Edge.Right
            return WindowManager.LayoutParams(
                if (vertical) thickness else WindowManager.LayoutParams.MATCH_PARENT,
                if (vertical) WindowManager.LayoutParams.MATCH_PARENT else thickness,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSPARENT,
            ).apply {
                gravity = when (edge) {
                    Edge.Left -> Gravity.LEFT or Gravity.TOP
                    Edge.Right -> Gravity.RIGHT or Gravity.TOP
                    Edge.Top -> Gravity.TOP or Gravity.LEFT
                    Edge.Bottom -> Gravity.BOTTOM or Gravity.LEFT
                }
                layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                title = "CrownConnect cursor handoff"
            }
        }
    }
}
