package com.crownos.connect.media

import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.unit.IntSize
import com.crownos.connect.protocol.BUTTON_LEFT
import com.crownos.connect.protocol.BUTTON_MIDDLE
import com.crownos.connect.protocol.BUTTON_RIGHT
import com.crownos.connect.protocol.InputEvent
import com.crownos.connect.protocol.InputMomentWriter
import com.crownos.connect.protocol.SCROLL_DETENT_V120
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.util.EvdevKeymap
import kotlin.math.roundToInt

class RemoteInputSender(private val runtime: LltsRuntime, private val peer: String) {
    private val writer = InputMomentWriter()
    private var primary = false
    private var secondary = false
    private var tertiary = false

    fun onPointer(event: PointerEvent, area: IntSize) {
        if (area.width <= 0 || area.height <= 0) return
        writer.begin(SystemClock.uptimeMillis())
        event.changes.forEach { change ->
            val (x, y) = normalized(change.position, area)
            if (change.type == PointerType.Mouse) {
                when (event.type) {
                    PointerEventType.Scroll -> writer.add(
                        InputEvent.Scroll(
                            (change.scrollDelta.x * SCROLL_DETENT_V120).roundToInt(),
                            (change.scrollDelta.y * SCROLL_DETENT_V120).roundToInt(),
                        ),
                    )
                    else -> {
                        writer.add(InputEvent.PointerMotion(x, y))
                        writeButtons(event.buttons)
                    }
                }
            } else {
                val id = change.id.value.toInt()
                when {
                    change.pressed && !change.previousPressed -> writer.add(InputEvent.TouchDown(id, x, y))
                    change.pressed -> writer.add(InputEvent.TouchMotion(id, x, y))
                    change.previousPressed -> writer.add(InputEvent.TouchUp(id))
                }
            }
            change.consume()
        }
        flush()
    }

    fun onKey(androidKeycode: Int, pressed: Boolean): Boolean {
        val code = EvdevKeymap.evdevCode(androidKeycode) ?: return false
        writer.begin(SystemClock.uptimeMillis()).add(InputEvent.Key(code, pressed))
        flush()
        return true
    }

    fun type(text: String) {
        text.forEach { character ->
            val stroke = EvdevKeymap.keystrokeFor(character) ?: return@forEach
            writer.begin(SystemClock.uptimeMillis())
            if (stroke.shifted) writer.add(InputEvent.Key(EvdevKeymap.KEY_LEFTSHIFT, true))
            writer.add(InputEvent.Key(stroke.code, true)).add(InputEvent.Key(stroke.code, false))
            if (stroke.shifted) writer.add(InputEvent.Key(EvdevKeymap.KEY_LEFTSHIFT, false))
            flush()
        }
    }

    fun backspace() {
        writer.begin(SystemClock.uptimeMillis())
            .add(InputEvent.Key(EvdevKeymap.KEY_BACKSPACE, true))
            .add(InputEvent.Key(EvdevKeymap.KEY_BACKSPACE, false))
        flush()
    }

    private fun writeButtons(buttons: PointerButtons) {
        primary = writeButton(BUTTON_LEFT, primary, buttons.isPrimaryPressed)
        secondary = writeButton(BUTTON_RIGHT, secondary, buttons.isSecondaryPressed)
        tertiary = writeButton(BUTTON_MIDDLE, tertiary, buttons.isTertiaryPressed)
    }

    private fun writeButton(button: Int, was: Boolean, now: Boolean): Boolean {
        if (was != now) writer.add(InputEvent.PointerButton(button, now))
        return now
    }

    private fun normalized(position: Offset, area: IntSize) =
        (position.x / area.width).coerceIn(0f, 1f) to (position.y / area.height).coerceIn(0f, 1f)

    private fun flush() {
        if (writer.isEmpty) return
        runtime.sendInput(peer, writer.add(InputEvent.Frame).finish())
    }
}
