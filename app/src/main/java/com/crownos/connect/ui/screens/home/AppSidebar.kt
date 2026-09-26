package com.crownos.connect.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crownos.connect.BuildConfig
import com.crownos.connect.runtime.PeerDevice
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.Tone
import com.crownos.connect.ui.kit.color
import com.crownos.connect.ui.layout.SidebarBrand
import com.crownos.connect.ui.layout.SidebarItem
import com.crownos.connect.ui.layout.SidebarSection
import com.crownos.connect.ui.navigation.Screen
import com.crownos.connect.ui.screens.devices.icon

@Composable
fun ColumnScope.AppSidebar(devices: List<PeerDevice>, selected: Screen, onSelect: (Screen) -> Unit) {
    SidebarBrand("CrownConnect", CrownIcons.MonitorSmartphone)
    SidebarSection("Devices") {
        devices.forEach { device ->
            val screen = Screen.Device(device.id)
            SidebarItem(
                device.name,
                selected == screen,
                { onSelect(screen) },
                icon = device.deviceClass.icon,
                trailing = { ConnectionDot(device.connected) },
            )
        }
        SidebarItem("Add a device", selected == Screen.AddDevice, { onSelect(Screen.AddDevice) }, icon = CrownIcons.Plus)
    }
    SidebarSection("System") {
        SidebarItem("Permissions", selected == Screen.Permissions, { onSelect(Screen.Permissions) }, icon = CrownIcons.ShieldCheck)
        SidebarItem("Settings", selected == Screen.Settings, { onSelect(Screen.Settings) }, icon = CrownIcons.Settings)
        if (BuildConfig.DEBUG) {
            SidebarItem("Widget gallery", selected == Screen.Gallery, { onSelect(Screen.Gallery) }, icon = CrownIcons.Palette)
        }
    }
}

@Composable
private fun ConnectionDot(connected: Boolean) {
    val color = (if (connected) Tone.Success else Tone.Neutral).color
    Canvas(Modifier.size(7.dp)) { drawCircle(color) }
}
