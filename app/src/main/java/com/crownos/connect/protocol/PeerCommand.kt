package com.crownos.connect.protocol

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

const val EDGE_POSITION_MAX = 65_535

sealed interface MediaCommand {
    data object Play : MediaCommand
    data object Pause : MediaCommand
    data object Next : MediaCommand
    data object Previous : MediaCommand
    data class Seek(val percent: Int) : MediaCommand
}

sealed interface CallRequest {
    data class Pickup(val callId: String) : CallRequest
    data class Decline(val callId: String) : CallRequest
    data class Hangup(val callId: String) : CallRequest
    data class Dial(val number: String) : CallRequest
}

sealed interface PeerCommand {
    fun toJson(): JsonElement

    data class Call(val request: CallRequest) : PeerCommand {
        override fun toJson() = buildJsonObject {
            put("Call", when (request) {
                is CallRequest.Pickup -> tagged("Pickup") { put("call_id", request.callId) }
                is CallRequest.Decline -> tagged("Decline") { put("call_id", request.callId) }
                is CallRequest.Hangup -> tagged("Hangup") { put("call_id", request.callId) }
                is CallRequest.Dial -> tagged("Dial") { put("number", request.number) }
            })
        }
    }

    data class Media(val command: MediaCommand) : PeerCommand {
        override fun toJson() = buildJsonObject {
            put("Media", when (command) {
                MediaCommand.Play -> JsonPrimitive("Play")
                MediaCommand.Pause -> JsonPrimitive("Pause")
                MediaCommand.Next -> JsonPrimitive("Next")
                MediaCommand.Previous -> JsonPrimitive("Previous")
                is MediaCommand.Seek -> tagged("Seek") { put("percent", command.percent.coerceIn(0, 100)) }
            })
        }
    }

    data class Reply(val conversationId: String, val text: String) : PeerCommand {
        override fun toJson() = tagged("Reply") {
            put("conversation_id", conversationId)
            put("text", text)
        }
    }

    data class SetVolume(val percent: Int, val muted: Boolean) : PeerCommand {
        override fun toJson() = tagged("SetVolume") {
            put("percent", percent.coerceIn(0, 100))
            put("muted", muted)
        }
    }

    data object Lock : PeerCommand {
        override fun toJson() = JsonPrimitive("Lock")
    }

    data class SetHotspot(val enabled: Boolean) : PeerCommand {
        override fun toJson() = tagged("SetHotspot") { put("enabled", enabled) }
    }

    data class FileOffer(val transferId: Long) : PeerCommand {
        override fun toJson() = tagged("FileOffer") { put("ffsp_transfer_id", transferId) }
    }

    data class UnicursorEnter(val edge: Edge, val position: Int) : PeerCommand {
        override fun toJson() = tagged("UnicursorEnter") {
            put("edge", edge.name)
            put("position", position.coerceIn(0, EDGE_POSITION_MAX))
        }
    }

    data class UnicursorLeave(val edge: Edge, val position: Int) : PeerCommand {
        override fun toJson() = tagged("UnicursorLeave") {
            put("edge", edge.name)
            put("position", position.coerceIn(0, EDGE_POSITION_MAX))
        }
    }

    companion object {
        fun parse(json: JsonElement): PeerCommand? {
            val body = json.variantBody()
            return when (json.variantName()) {
                "Lock" -> Lock
                "Media" -> parseMedia((json as? JsonObject)?.get("Media"))?.let(::Media)
                "Call" -> parseCall((json as? JsonObject)?.get("Call"))?.let(::Call)
                "Reply" -> body?.let { Reply(it.string("conversation_id") ?: return null, it.string("text") ?: return null) }
                "SetVolume" -> body?.let {
                    SetVolume(it["percent"]?.jsonPrimitive?.int ?: return null, it["muted"]?.jsonPrimitive?.boolean ?: false)
                }
                "SetHotspot" -> body?.let { SetHotspot(it["enabled"]?.jsonPrimitive?.boolean ?: return null) }
                "FileOffer" -> body?.let { FileOffer(it["ffsp_transfer_id"]?.jsonPrimitive?.long ?: return null) }
                "UnicursorEnter" -> body?.let {
                    UnicursorEnter(enumByName<Edge>(it.string("edge")) ?: return null, it["position"]?.jsonPrimitive?.int ?: return null)
                }
                "UnicursorLeave" -> body?.let {
                    UnicursorLeave(enumByName<Edge>(it.string("edge")) ?: return null, it["position"]?.jsonPrimitive?.int ?: return null)
                }
                else -> null
            }
        }

        private fun parseMedia(json: JsonElement?): MediaCommand? = when (json?.variantName()) {
            "Play" -> MediaCommand.Play
            "Pause" -> MediaCommand.Pause
            "Next" -> MediaCommand.Next
            "Previous" -> MediaCommand.Previous
            "Seek" -> json.variantBody()?.get("percent")?.jsonPrimitive?.int?.let(MediaCommand::Seek)
            else -> null
        }

        private fun parseCall(json: JsonElement?): CallRequest? {
            val body = json?.variantBody() ?: return null
            return when (json.variantName()) {
                "Pickup" -> body.string("call_id")?.let(CallRequest::Pickup)
                "Decline" -> body.string("call_id")?.let(CallRequest::Decline)
                "Hangup" -> body.string("call_id")?.let(CallRequest::Hangup)
                "Dial" -> body.string("number")?.let(CallRequest::Dial)
                else -> null
            }
        }
    }
}

private inline fun tagged(name: String, body: JsonObjectBuilder.() -> Unit): JsonObject =
    buildJsonObject { put(name, buildJsonObject(body)) }
