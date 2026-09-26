package com.crownos.connect.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import com.crownos.connect.protocol.BUTTON_LEFT
import com.crownos.connect.protocol.BUTTON_RIGHT
import com.crownos.connect.protocol.SCROLL_DETENT_V120
import com.crownos.connect.util.EvdevKeymap
import com.crownos.connect.util.ScreenGeometry
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_GESTURE_MILLIS = 10_000L
private const val SCROLL_GESTURE_MILLIS = 120L
private const val SCROLL_DISTANCE_DP_PER_DETENT = 64f

@Singleton
class AccessibilityInputInjector @Inject constructor(
    private val bridge: AccessibilityBridge,
    private val screen: ScreenGeometry,
) : InputInjector {
    private class Stroke(val path: Path, val startedAt: Long)

    private val touchStrokes = HashMap<Int, Stroke>()
    private var dragStroke: Stroke? = null

    override val isAvailable: Boolean get() = bridge.isConnected

    override fun touch(id: Int, phase: TouchPhase, x: Float, y: Float) {
        when (phase) {
            TouchPhase.Down -> touchStrokes[id] = Stroke(Path().apply { moveTo(x, y) }, SystemClock.uptimeMillis())
            TouchPhase.Move -> touchStrokes[id]?.path?.lineTo(x, y)
            TouchPhase.Up -> touchStrokes.remove(id)?.let(::dispatch)
        }
    }

    override fun cancelTouches() {
        touchStrokes.clear()
        dragStroke = null
    }

    override fun pointerMove(x: Float, y: Float) {
        dragStroke?.path?.lineTo(x, y)
    }

    override fun pointerButton(x: Float, y: Float, button: Int, pressed: Boolean) {
        when {
            button == BUTTON_RIGHT && pressed -> bridge.performGlobal(AccessibilityService.GLOBAL_ACTION_BACK)
            button != BUTTON_LEFT -> Unit
            pressed -> dragStroke = Stroke(Path().apply { moveTo(x, y) }, SystemClock.uptimeMillis())
            else -> dragStroke?.let { stroke ->
                stroke.path.lineTo(x, y)
                dispatch(stroke)
                dragStroke = null
            }
        }
    }

    override fun scroll(x: Float, y: Float, horizontalV120: Int, verticalV120: Int) {
        val distance = SCROLL_DISTANCE_DP_PER_DETENT * screen.size.density
        val dx = -horizontalV120.toFloat() / SCROLL_DETENT_V120 * distance
        val dy = -verticalV120.toFloat() / SCROLL_DETENT_V120 * distance
        val size = screen.size
        val path = Path().apply {
            moveTo(x, y)
            lineTo((x + dx).coerceIn(0f, size.width - 1f), (y + dy).coerceIn(0f, size.height - 1f))
        }
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, SCROLL_GESTURE_MILLIS)).build()
        bridge.service?.dispatchGesture(gesture, null, null)
    }

    override fun key(evdevCode: Int, pressed: Boolean, heldKeys: Set<Int>) {
        if (!pressed) return
        when (evdevCode) {
            EvdevKeymap.KEY_ESC, EvdevKeymap.KEY_BACK -> bridge.performGlobal(AccessibilityService.GLOBAL_ACTION_BACK)
            EvdevKeymap.KEY_HOMEPAGE -> bridge.performGlobal(AccessibilityService.GLOBAL_ACTION_HOME)
            EvdevKeymap.KEY_BACKSPACE -> editFocused { text, cursor ->
                if (cursor == 0) null else Edit(text.removeRange(cursor - 1, cursor), cursor - 1)
            }
            EvdevKeymap.KEY_ENTER -> pressEnter()
            else -> {
                if (heldKeys.any { it == EvdevKeymap.KEY_LEFTCTRL || it == EvdevKeymap.KEY_RIGHTCTRL || it == EvdevKeymap.KEY_LEFTMETA }) return
                val shifted = EvdevKeymap.KEY_LEFTSHIFT in heldKeys || EvdevKeymap.KEY_RIGHTSHIFT in heldKeys
                val character = EvdevKeymap.character(evdevCode, shifted) ?: return
                editFocused { text, cursor -> Edit(text.substring(0, cursor) + character + text.substring(cursor), cursor + 1) }
            }
        }
    }

    private fun dispatch(stroke: Stroke) {
        val duration = (SystemClock.uptimeMillis() - stroke.startedAt).coerceIn(1, MAX_GESTURE_MILLIS)
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(stroke.path, 0, duration)).build()
        bridge.service?.dispatchGesture(gesture, null, null)
    }

    private data class Edit(val text: String, val cursor: Int)

    private inline fun editFocused(change: (String, Int) -> Edit?) {
        val node = bridge.service?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable } ?: return
        val text = if (node.isShowingHintText) "" else node.text?.toString().orEmpty()
        val cursor = node.textSelectionEnd.takeIf { it in 0..text.length } ?: text.length
        val edit = change(text, cursor) ?: return
        node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, edit.text) },
        )
        node.performAction(
            AccessibilityNodeInfo.ACTION_SET_SELECTION,
            Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, edit.cursor)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, edit.cursor)
            },
        )
    }

    private fun pressEnter() {
        val node = bridge.service?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
        } else if (node.isMultiLine) {
            editFocused { text, cursor -> Edit(text.substring(0, cursor) + '\n' + text.substring(cursor), cursor + 1) }
        }
    }
}
