package com.crownos.connect.input

import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import com.crownos.connect.privileged.HiddenBinders
import com.crownos.connect.privileged.ShizukuAccess
import com.crownos.connect.protocol.BUTTON_LEFT
import com.crownos.connect.protocol.BUTTON_MIDDLE
import com.crownos.connect.protocol.BUTTON_RIGHT
import com.crownos.connect.protocol.SCROLL_DETENT_V120
import com.crownos.connect.util.EvdevKeymap
import java.lang.reflect.Method
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ShizukuInput"
private const val INJECT_MODE_ASYNC = 0
private const val MAX_POINTERS = 10

@Singleton
class ShizukuInputInjector @Inject constructor(private val shizuku: ShizukuAccess) : InputInjector {
    private var inputManager: Any? = null
    private var injectMethod: Method? = null
    private val setActionButton: Method? by lazy {
        runCatching { MotionEvent::class.java.getMethod("setActionButton", Int::class.javaPrimitiveType) }.getOrNull()
    }

    private val touchIds = IntArray(MAX_POINTERS)
    private val touchX = FloatArray(MAX_POINTERS)
    private val touchY = FloatArray(MAX_POINTERS)
    private var touchCount = 0
    private var touchDownTime = 0L

    private var mouseButtons = 0
    private var mouseDownTime = 0L

    override val isAvailable: Boolean get() = shizuku.isReady && ensureBinder()

    private fun ensureBinder(): Boolean {
        if (injectMethod != null) return true
        val manager = HiddenBinders.systemInterface("input", "android.hardware.input.IInputManager") ?: return false
        injectMethod = manager.javaClass.methods.firstOrNull { it.name == "injectInputEvent" && it.parameterCount == 2 }
        inputManager = manager
        return injectMethod != null
    }

    private fun inject(event: InputEvent) {
        try {
            injectMethod?.invoke(inputManager, event, INJECT_MODE_ASYNC)
        } catch (failure: ReflectiveOperationException) {
            Log.w(TAG, "injecting input failed", failure)
            injectMethod = null
        } finally {
            (event as? MotionEvent)?.recycle()
        }
    }

    override fun touch(id: Int, phase: TouchPhase, x: Float, y: Float) {
        if (!isAvailable) return
        val now = SystemClock.uptimeMillis()
        var index = (0 until touchCount).firstOrNull { touchIds[it] == id } ?: -1
        when (phase) {
            TouchPhase.Down -> {
                if (index < 0) {
                    if (touchCount == MAX_POINTERS) return
                    index = touchCount++
                    touchIds[index] = id
                }
                touchX[index] = x
                touchY[index] = y
                if (touchCount == 1) touchDownTime = now
                val action = if (touchCount == 1) MotionEvent.ACTION_DOWN else pointerAction(MotionEvent.ACTION_POINTER_DOWN, index)
                inject(touchEvent(now, action))
            }
            TouchPhase.Move -> {
                if (index < 0) return
                touchX[index] = x
                touchY[index] = y
                inject(touchEvent(now, MotionEvent.ACTION_MOVE))
            }
            TouchPhase.Up -> {
                if (index < 0) return
                val action = if (touchCount == 1) MotionEvent.ACTION_UP else pointerAction(MotionEvent.ACTION_POINTER_UP, index)
                inject(touchEvent(now, action))
                removeTouch(index)
            }
        }
    }

    override fun cancelTouches() {
        if (touchCount == 0 || !isAvailable) return
        inject(touchEvent(SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL))
        touchCount = 0
    }

    private fun removeTouch(index: Int) {
        for (next in index until touchCount - 1) {
            touchIds[next] = touchIds[next + 1]
            touchX[next] = touchX[next + 1]
            touchY[next] = touchY[next + 1]
        }
        touchCount--
    }

    private fun pointerAction(base: Int, index: Int) = base or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)

    private fun touchEvent(now: Long, action: Int): MotionEvent {
        val properties = Array(touchCount) { MotionEvent.PointerProperties().apply { id = touchIds[it]; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val coords = Array(touchCount) { MotionEvent.PointerCoords().apply { x = touchX[it]; y = touchY[it]; pressure = 1f; size = 1f } }
        return MotionEvent.obtain(touchDownTime, now, action, touchCount, properties, coords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
    }

    override fun pointerMove(x: Float, y: Float) {
        if (!isAvailable) return
        val action = if (mouseButtons == 0) MotionEvent.ACTION_HOVER_MOVE else MotionEvent.ACTION_MOVE
        inject(mouseEvent(SystemClock.uptimeMillis(), action, x, y, 0))
    }

    override fun pointerButton(x: Float, y: Float, button: Int, pressed: Boolean) {
        if (!isAvailable) return
        val androidButton = when (button) {
            BUTTON_LEFT -> MotionEvent.BUTTON_PRIMARY
            BUTTON_RIGHT -> MotionEvent.BUTTON_SECONDARY
            BUTTON_MIDDLE -> MotionEvent.BUTTON_TERTIARY
            else -> return
        }
        val now = SystemClock.uptimeMillis()
        if (pressed) {
            if (mouseButtons and androidButton != 0) return
            if (mouseButtons == 0) {
                mouseDownTime = now
                mouseButtons = androidButton
                inject(mouseEvent(now, MotionEvent.ACTION_DOWN, x, y, 0))
            } else {
                mouseButtons = mouseButtons or androidButton
            }
            inject(mouseEvent(now, MotionEvent.ACTION_BUTTON_PRESS, x, y, androidButton))
        } else {
            if (mouseButtons and androidButton == 0) return
            mouseButtons = mouseButtons and androidButton.inv()
            inject(mouseEvent(now, MotionEvent.ACTION_BUTTON_RELEASE, x, y, androidButton))
            if (mouseButtons == 0) inject(mouseEvent(now, MotionEvent.ACTION_UP, x, y, 0))
        }
    }

    override fun scroll(x: Float, y: Float, horizontalV120: Int, verticalV120: Int) {
        if (!isAvailable) return
        val now = SystemClock.uptimeMillis()
        val coords = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            setAxisValue(MotionEvent.AXIS_VSCROLL, -verticalV120.toFloat() / SCROLL_DETENT_V120)
            setAxisValue(MotionEvent.AXIS_HSCROLL, horizontalV120.toFloat() / SCROLL_DETENT_V120)
        }
        val properties = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_MOUSE }
        inject(MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL, 1, arrayOf(properties), arrayOf(coords), 0, mouseButtons, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0))
    }

    private fun mouseEvent(now: Long, action: Int, x: Float, y: Float, actionButton: Int): MotionEvent {
        val properties = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_MOUSE }
        val coords = MotionEvent.PointerCoords().apply { this.x = x; this.y = y; pressure = if (mouseButtons != 0) 1f else 0f; size = 1f }
        val downTime = if (mouseButtons != 0 || action == MotionEvent.ACTION_UP) mouseDownTime else now
        val event = MotionEvent.obtain(downTime, now, action, 1, arrayOf(properties), arrayOf(coords), 0, mouseButtons, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0)
        if (actionButton != 0) runCatching { setActionButton?.invoke(event, actionButton) }
        return event
    }

    override fun key(evdevCode: Int, pressed: Boolean, heldKeys: Set<Int>) {
        if (!isAvailable) return
        val keycode = EvdevKeymap.androidKeycode(evdevCode) ?: return
        val now = SystemClock.uptimeMillis()
        val event = KeyEvent(
            now, now,
            if (pressed) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP,
            keycode, 0, EvdevKeymap.metaState(heldKeys),
            KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD,
        )
        inject(event)
    }

    fun sendKeyPress(keycode: Int): Boolean {
        if (!isAvailable) return false
        val now = SystemClock.uptimeMillis()
        listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
            inject(KeyEvent(now, now, action, keycode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD))
        }
        return true
    }
}
