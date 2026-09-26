package com.crownos.connect.protocol

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

const val BUTTON_LEFT = 0x110
const val BUTTON_RIGHT = 0x111
const val BUTTON_MIDDLE = 0x112
const val SCROLL_DETENT_V120 = 120

sealed interface InputEvent {
    data class PointerMotion(val x: Float, val y: Float) : InputEvent
    data class PointerRelative(val dx: Float, val dy: Float) : InputEvent
    data class PointerButton(val button: Int, val pressed: Boolean) : InputEvent
    data class Scroll(val horizontalV120: Int, val verticalV120: Int) : InputEvent
    data class Key(val code: Int, val pressed: Boolean) : InputEvent
    data class TouchDown(val id: Int, val x: Float, val y: Float) : InputEvent
    data class TouchMotion(val id: Int, val x: Float, val y: Float) : InputEvent
    data class TouchUp(val id: Int) : InputEvent
    data object TouchCancel : InputEvent
    data object Frame : InputEvent

    companion object {
        fun parse(json: JsonElement): InputEvent? {
            val body = json.variantBody()
            return when (json.variantName()) {
                "Frame" -> Frame
                "TouchCancel" -> TouchCancel
                "PointerMotion" -> body?.let { PointerMotion(it.float("x") ?: return null, it.float("y") ?: return null) }
                "PointerRelative" -> body?.let { PointerRelative(it.float("dx") ?: return null, it.float("dy") ?: return null) }
                "PointerButton" -> body?.let { PointerButton(it.int("button") ?: return null, it.flag("pressed")) }
                "Scroll" -> body?.let { Scroll(it.int("horizontal_v120") ?: 0, it.int("vertical_v120") ?: 0) }
                "Key" -> body?.let { Key(it.int("code") ?: return null, it.flag("pressed")) }
                "TouchDown" -> body?.let { TouchDown(it.int("id") ?: return null, it.float("x") ?: return null, it.float("y") ?: return null) }
                "TouchMotion" -> body?.let { TouchMotion(it.int("id") ?: return null, it.float("x") ?: return null, it.float("y") ?: return null) }
                "TouchUp" -> body?.let { TouchUp(it.int("id") ?: return null) }
                else -> null
            }
        }
    }
}

private fun JsonObject.float(key: String): Float? = get(key)?.jsonPrimitive?.float
private fun JsonObject.int(key: String): Int? = get(key)?.jsonPrimitive?.int
private fun JsonObject.flag(key: String): Boolean = get(key)?.jsonPrimitive?.boolean ?: false

class InputMomentWriter(capacity: Int = 256) {
    private val text = StringBuilder(capacity)
    private var events = 0

    fun begin(timeMillis: Long): InputMomentWriter {
        text.setLength(0)
        events = 0
        text.append("{\"timeMs\":").append(timeMillis and 0xFFFF_FFFFL).append(",\"events\":[")
        return this
    }

    fun add(event: InputEvent): InputMomentWriter {
        if (events++ > 0) text.append(',')
        when (event) {
            is InputEvent.PointerMotion -> text.append("{\"PointerMotion\":{\"x\":").append(event.x).append(",\"y\":").append(event.y).append("}}")
            is InputEvent.PointerRelative -> text.append("{\"PointerRelative\":{\"dx\":").append(event.dx).append(",\"dy\":").append(event.dy).append("}}")
            is InputEvent.PointerButton -> text.append("{\"PointerButton\":{\"button\":").append(event.button).append(",\"pressed\":").append(event.pressed).append("}}")
            is InputEvent.Scroll -> text.append("{\"Scroll\":{\"horizontal_v120\":").append(event.horizontalV120).append(",\"vertical_v120\":").append(event.verticalV120).append("}}")
            is InputEvent.Key -> text.append("{\"Key\":{\"code\":").append(event.code).append(",\"pressed\":").append(event.pressed).append("}}")
            is InputEvent.TouchDown -> touch("TouchDown", event.id, event.x, event.y)
            is InputEvent.TouchMotion -> touch("TouchMotion", event.id, event.x, event.y)
            is InputEvent.TouchUp -> text.append("{\"TouchUp\":{\"id\":").append(event.id).append("}}")
            InputEvent.TouchCancel -> text.append("\"TouchCancel\"")
            InputEvent.Frame -> text.append("\"Frame\"")
        }
        return this
    }

    val isEmpty: Boolean get() = events == 0

    fun finish(): String = text.append("]}").toString()

    private fun touch(name: String, id: Int, x: Float, y: Float) {
        text.append("{\"").append(name).append("\":{\"id\":").append(id)
            .append(",\"x\":").append(x.coerceIn(0f, 1f)).append(",\"y\":").append(y.coerceIn(0f, 1f)).append("}}")
    }
}
