package com.crownos.connect.files

import com.crownos.connect.protocol.ProtocolJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

data class OfferedFile(val name: String, val length: Long)

sealed interface FfspEvent {
    data class LinkSettled(val device: String, val accepted: Boolean) : FfspEvent
    data class Offered(val device: String, val transfer: Int, val files: List<OfferedFile>) : FfspEvent
    data class Progress(val device: String, val deliveredBytes: Long, val totalBytes: Long) : FfspEvent
    data class Finished(val device: String, val outcome: String) : FfspEvent {
        val completed: Boolean get() = outcome.equals("Completed", ignoreCase = true)
    }
    data class Failed(val reason: String) : FfspEvent

    companion object {
        fun parse(text: String): FfspEvent? {
            val json = runCatching { ProtocolJson.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
            val device = json.text("device").orEmpty()
            return when (json.text("type")) {
                "linkSettled" -> LinkSettled(device, json["accepted"]?.jsonPrimitive?.boolean == true)
                "transferOffered" -> Offered(
                    device,
                    json["transfer"]?.jsonPrimitive?.int ?: return null,
                    json["entries"]?.jsonArray.orEmpty().map { entry ->
                        val fields = entry.jsonObject
                        OfferedFile(fields.text("name").orEmpty(), fields["len"]?.jsonPrimitive?.long ?: 0)
                    },
                )
                "transferProgress" -> Progress(
                    device,
                    json["deliveredBytes"]?.jsonPrimitive?.long ?: 0,
                    json["totalBytes"]?.jsonPrimitive?.long ?: 0,
                )
                "transferFinished" -> Finished(device, json.text("outcome").orEmpty())
                "laneFailed", "engineFailed" -> Failed(json.text("reason") ?: "the transfer failed")
                else -> null
            }
        }

        private fun JsonObject.text(key: String): String? = get(key)?.jsonPrimitive?.content
    }
}
