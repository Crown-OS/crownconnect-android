package com.crownos.connect.unicursor

import android.os.SystemClock
import android.view.MotionEvent
import com.crownos.connect.input.KeyStateTracker
import com.crownos.connect.protocol.BUTTON_LEFT
import com.crownos.connect.protocol.BUTTON_MIDDLE
import com.crownos.connect.protocol.BUTTON_RIGHT
import com.crownos.connect.protocol.InputEvent
import com.crownos.connect.protocol.InputMomentWriter
import com.crownos.connect.protocol.SCROLL_DETENT_V120
import com.crownos.connect.util.EvdevKeymap
import kotlin.math.roundToInt

private val EVDEV_BUTTONS = arrayOf(
    MotionEvent.BUTTON_PRIMARY to BUTTON_LEFT,
    MotionEvent.BUTTON_SECONDARY to BUTTON_RIGHT,
    MotionEvent.BUTTON_TERTIARY to BUTTON_MIDDLE,
)

/** Turns this phone's captured mouse and keyboard into input moments for the computer holding the cursor. */
class CapturedInputSender(private val send: (String) -> Unit) {
    private val writer = InputMomentWriter()
    private val keys = KeyStateTracker()
    private var buttons = 0

    fun motion(event: MotionEvent) {
        writer.begin(SystemClock.uptimeMillis())
        if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
            writer.add(
                InputEvent.Scroll(
                    (event.getAxisValue(MotionEvent.AXIS_HSCROLL) * SCROLL_DETENT_V120).roundToInt(),
                    (-event.getAxisValue(MotionEvent.AXIS_VSCROLL) * SCROLL_DETENT_V120).roundToInt(),
                ),
            )
        } else {
            var dx = event.x
            var dy = event.y
            for (sample in 0 until event.historySize) {
                dx += event.getHistoricalX(sample)
                dy += event.getHistoricalY(sample)
            }
            if (dx != 0f || dy != 0f) writer.add(InputEvent.PointerRelative(dx, dy))
        }
        writeButtons(event.buttonState)
        flush()
    }

    fun key(androidKeycode: Int, pressed: Boolean): Boolean {
        val code = EvdevKeymap.evdevCode(androidKeycode) ?: return false
        if (keys.update(code, pressed)) {
            writer.begin(SystemClock.uptimeMillis()).add(InputEvent.Key(code, pressed))
            flush()
        }
        return true
    }

    fun releaseAll() {
        writer.begin(SystemClock.uptimeMillis())
        keys.drain().forEach { writer.add(InputEvent.Key(it, false)) }
        writeButtons(0)
        flush()
    }

    private fun writeButtons(state: Int) {
        val changed = state xor buttons
        EVDEV_BUTTONS.forEach { (android, evdev) ->
            if (changed and android != 0) writer.add(InputEvent.PointerButton(evdev, state and android != 0))
        }
        buttons = state
    }

    private fun flush() {
        if (writer.isEmpty) return
        send(writer.add(InputEvent.Frame).finish())
    }
}
