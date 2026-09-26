package com.crownos.connect.runtime

import androidx.compose.runtime.Immutable
import com.crownos.connect.llts.LltsDevice
import com.crownos.connect.protocol.DeviceClass
import com.crownos.connect.protocol.Feature
import com.crownos.connect.protocol.enumByName

@Immutable
data class PeerDevice(
    val id: String,
    val name: String,
    val deviceClass: DeviceClass,
    val connected: Boolean,
    val enabled: Set<Feature>,
    val available: Set<Feature>,
    val active: Set<Feature>,
) {
    fun allows(feature: Feature) = feature in available

    companion object {
        fun from(device: LltsDevice) = PeerDevice(
            id = device.id,
            name = device.name,
            deviceClass = enumByName<DeviceClass>(device.deviceClass) ?: DeviceClass.Computer,
            connected = device.connected,
            enabled = device.enabled.featureSet(),
            available = device.available.featureSet(),
            active = device.active.featureSet(),
        )

        private fun List<String>.featureSet(): Set<Feature> = mapNotNullTo(mutableSetOf()) { enumByName<Feature>(it) }
    }
}

data class FeatureFailure(val peer: String, val feature: Feature, val reason: String) {
    val message: String
        get() = when (reason) {
            "Unavailable" -> "${feature.name} isn't available on this device pair."
            "NoCommonCodec" -> "The devices share no video codec for ${feature.name}."
            "Refused" -> "The other device refused ${feature.name}."
            "AlreadyActive" -> "${feature.name} is already running."
            "NotConnected" -> "The device isn't connected."
            else -> "${feature.name} failed: $reason"
        }
}
