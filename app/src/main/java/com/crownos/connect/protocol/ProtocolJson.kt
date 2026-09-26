package com.crownos.connect.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

val ProtocolJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
    encodeDefaults = true
}

internal fun JsonElement.variantName(): String? = when (this) {
    is JsonPrimitive -> contentOrNull
    is JsonObject -> keys.singleOrNull()
    else -> null
}

internal fun JsonElement.variantBody(): JsonObject? = (this as? JsonObject)?.values?.singleOrNull() as? JsonObject

internal fun JsonObject.string(key: String): String? = get(key)?.jsonPrimitive?.contentOrNull
