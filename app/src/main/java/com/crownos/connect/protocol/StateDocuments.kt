package com.crownos.connect.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@Serializable
data class BatteryDocument(
    val percent: Int,
    val charging: Boolean,
    @SerialName("time_to_empty_min") val timeToEmptyMinutes: Int? = null,
)

@Serializable
data class VolumeDocument(val percent: Int, val muted: Boolean)

@Serializable
data class MediaSessionDocument(
    val app: String,
    val title: String,
    val artist: String,
    @SerialName("position_ms") val positionMillis: Long,
    @SerialName("duration_ms") val durationMillis: Long,
    val playing: Boolean,
)

@Serializable
data class HotspotDocument(val enabled: Boolean, val ssid: String? = null)

@Serializable
enum class CallStatus { Ringing, Dialing, Active, Held, Ended }

@Serializable
data class CallInfo(
    @SerialName("call_id") val callId: String,
    val number: String,
    @SerialName("contact_name") val contactName: String? = null,
    val status: CallStatus,
    @SerialName("answered_unix_ms") val answeredUnixMillis: Long? = null,
)

@Serializable
data class CallStateDocument(val calls: List<CallInfo>)

@Serializable
data class NotificationAction(
    val id: String,
    val label: String,
    @SerialName("accepts_text") val acceptsText: Boolean,
)

@Serializable
data class NotificationEntry(
    val id: String,
    val app: String,
    val title: String,
    val body: String,
    val actions: List<NotificationAction>,
)

@Serializable
data class NotificationsDocument(val active: List<NotificationEntry>)

data class ClipboardDocument(val mime: String, val text: String?) {
    fun toJson(): JsonObject = buildJsonObject {
        put("mime", mime)
        put("content", buildJsonObject { put("Inline", text.orEmpty()) })
    }

    companion object {
        const val MAX_INLINE_BYTES = 60 * 1024

        fun parse(json: JsonElement?): ClipboardDocument? {
            val fields = (json as? JsonObject) ?: return null
            val content = fields["content"] ?: return null
            val text = if (content.variantName() == "Inline") {
                (content.jsonObject["Inline"] as? JsonPrimitive)?.content
            } else {
                null
            }
            return ClipboardDocument(fields.string("mime") ?: "text/plain", text)
        }
    }
}

fun <T> KSerializer<T>.encodeDocument(value: T): JsonElement = ProtocolJson.encodeToJsonElement(this, value)

fun <T> KSerializer<T>.decodeDocument(json: JsonElement?): T? {
    if (json == null || json is JsonNull) return null
    return try {
        ProtocolJson.decodeFromJsonElement(this, json)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
