package com.crownos.connect.ui.screens.home

import androidx.compose.runtime.Composable
import com.crownos.connect.BuildConfig
import com.crownos.connect.protocol.BatteryDocument
import com.crownos.connect.runtime.PeerDevice
import com.crownos.connect.ui.kit.CrownCallout
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.CrownStatus
import com.crownos.connect.ui.kit.Tone
import com.crownos.connect.ui.layout.SettingRow
import com.crownos.connect.ui.layout.SettingsCardTitled
import com.crownos.connect.ui.layout.SettingsDivider
import com.crownos.connect.ui.layout.SettingsPage
import com.crownos.connect.ui.navigation.Screen
import com.crownos.connect.ui.screens.devices.icon
import com.crownos.connect.ui.screens.devices.statusText

@Composable
fun HomeScreen(devices: List<PeerDevice>, battery: (String) -> BatteryDocument?, onOpen: (Screen) -> Unit) {
    SettingsPage("CrownConnect", CrownIcons.MonitorSmartphone) {
        if (devices.isEmpty()) {
            CrownCallout(
                "No devices yet",
                Tone.Accent,
                body = "Pair your CrownOS computer to share screens, the camera, the clipboard and notifications.",
            )
        }
        SettingsCardTitled("Devices") {
            devices.forEach { device ->
                SettingRow(
                    device.name,
                    description = device.statusText(battery(device.id)),
                    icon = device.deviceClass.icon,
                    onClick = { onOpen(Screen.Device(device.id)) },
                ) {
                    CrownStatus(if (device.connected) "Online" else "Offline", if (device.connected) Tone.Success else Tone.Neutral)
                }
                SettingsDivider()
            }
            SettingRow("Add a device", icon = CrownIcons.Plus, onClick = { onOpen(Screen.AddDevice) })
        }
        SettingsCardTitled("System") {
            SettingRow("Permissions", description = "Access this phone gives CrownConnect", icon = CrownIcons.ShieldCheck, onClick = { onOpen(Screen.Permissions) })
            SettingsDivider()
            SettingRow("Settings", icon = CrownIcons.Settings, onClick = { onOpen(Screen.Settings) })
            if (BuildConfig.DEBUG) {
                SettingsDivider()
                SettingRow("Widget gallery", icon = CrownIcons.Palette, onClick = { onOpen(Screen.Gallery) })
            }
        }
    }
}
