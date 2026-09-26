package com.crownos.connect.ui.screens.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crownos.connect.protocol.Feature
import com.crownos.connect.runtime.FeatureFailure
import com.crownos.connect.runtime.PeerDevice
import com.crownos.connect.ui.kit.ButtonSize
import com.crownos.connect.ui.kit.ButtonVariant
import com.crownos.connect.ui.kit.CrownButton
import com.crownos.connect.ui.kit.CrownCallout
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.CrownStatus
import com.crownos.connect.ui.kit.CrownToggle
import com.crownos.connect.ui.kit.Tone
import com.crownos.connect.ui.layout.SettingRow
import com.crownos.connect.ui.layout.SettingsCardTitled
import com.crownos.connect.ui.layout.SettingsDivider
import com.crownos.connect.ui.layout.SettingsPage
import com.crownos.connect.ui.layout.ValueText
import com.crownos.connect.ui.screens.devices.icon

@Composable
fun DeviceScreen(deviceId: String, onBack: (() -> Unit)?, onForgotten: () -> Unit) {
    val viewModel = hiltViewModel<DeviceViewModel, DeviceViewModel.Factory>(key = deviceId) { it.create(deviceId) }
    val device by viewModel.device.collectAsStateWithLifecycle()
    val states by viewModel.states.collectAsStateWithLifecycle()
    var failure by remember { mutableStateOf<FeatureFailure?>(null) }
    LaunchedEffect(viewModel) { viewModel.failures.collect { failure = it } }

    val current = device
    if (current == null) {
        SettingsPage("Device", CrownIcons.Laptop, onBack = onBack) {
            CrownCallout("This device is no longer paired", Tone.Neutral)
        }
        return
    }

    SettingsPage(current.name, current.deviceClass.icon, onBack = onBack) {
        failure?.let { CrownCallout(it.message, Tone.Warning) }
        StatusCard(current, viewModel.battery(states)?.let { "${it.percent}%${if (it.charging) ", charging" else ""}" })
        FeaturesCard(current, viewModel::setFeature)
        SettingsCardTitled("Device") {
            SettingRow("Forget this device", description = "Unpairs it on both ends") {
                CrownButton(
                    "Forget",
                    {
                        viewModel.forget()
                        onForgotten()
                    },
                    variant = ButtonVariant.Destructive,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(device: PeerDevice, battery: String?) {
    SettingsCardTitled("Status") {
        SettingRow("Connection", icon = CrownIcons.Wifi) {
            CrownStatus(if (device.connected) "Connected" else "Not connected", if (device.connected) Tone.Success else Tone.Neutral)
        }
        battery?.let {
            SettingsDivider()
            SettingRow("Battery", icon = CrownIcons.BatteryFull) { ValueText(it) }
        }
    }
}

@Composable
private fun FeaturesCard(device: PeerDevice, onToggle: (Feature, Boolean) -> Unit) {
    SettingsCardTitled("Features") {
        Feature.entries.forEachIndexed { index, feature ->
            val info = feature.info
            val enabled = feature in device.enabled
            val description = when {
                enabled && device.connected && feature !in device.available -> "Turned off on ${device.name}"
                else -> info.description
            }
            SettingRow(info.title, description = description, icon = info.icon) {
                CrownToggle(enabled, { onToggle(feature, it) })
            }
            if (index < Feature.entries.lastIndex) SettingsDivider()
        }
    }
}
