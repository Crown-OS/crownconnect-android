package com.crownos.connect.service

import com.crownos.connect.protocol.Feature

data class SharedStream(val feature: Feature, val peerName: String)

data class ConnectionStatus(val connected: List<String>, val sharing: List<SharedStream>) {
    val title: String
        get() = when {
            sharing.isNotEmpty() -> "Sharing with ${sharing.map { it.peerName }.distinct().joinToString()}"
            else -> "CrownConnect"
        }

    val text: String
        get() = when {
            sharing.isNotEmpty() -> sharing.map { it.feature.label }.distinct().joinToString(", ")
            connected.isEmpty() -> "Waiting for your computers"
            connected.size == 1 -> "Connected to ${connected.first()}"
            else -> "Connected to ${connected.size} devices"
        }

    private val Feature.label: String
        get() = when (this) {
            Feature.Mirror -> "Screen"
            Feature.Camera -> "Camera"
            Feature.Mic -> "Microphone"
            Feature.Monitor -> "Display"
            else -> name
        }
}
