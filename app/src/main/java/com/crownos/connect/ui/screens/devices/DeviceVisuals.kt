package com.crownos.connect.ui.screens.devices

import com.crownos.connect.protocol.BatteryDocument
import com.crownos.connect.protocol.DeviceClass
import com.crownos.connect.runtime.PeerDevice
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.Tone

val DeviceClass.icon: Int
    get() = when (this) {
        DeviceClass.Computer -> CrownIcons.Laptop
        DeviceClass.Tablet -> CrownIcons.TabletSmartphone
        DeviceClass.Phone, DeviceClass.Watch -> CrownIcons.Smartphone
    }

val PeerDevice.statusTone: Tone get() = if (connected) Tone.Success else Tone.Neutral

fun PeerDevice.statusText(battery: BatteryDocument?): String {
    val connection = if (connected) "Connected" else "Not connected"
    val charge = battery?.let { " · ${it.percent}%${if (it.charging) " charging" else ""}" }.orEmpty()
    return connection + charge
}
