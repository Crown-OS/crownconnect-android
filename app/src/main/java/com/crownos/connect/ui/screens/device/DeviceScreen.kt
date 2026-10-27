package com.crownos.connect.ui.screens.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crownos.connect.protocol.Edge
import com.crownos.connect.protocol.Feature
import com.crownos.connect.runtime.FeatureFailure
import com.crownos.connect.runtime.PeerDevice
import com.crownos.connect.ui.kit.ButtonSize
import com.crownos.connect.ui.kit.ButtonVariant
import com.crownos.connect.ui.kit.CrownButton
import com.crownos.connect.ui.kit.CrownCallout
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.CrownSelect
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
    val streams by viewModel.streams.collectAsStateWithLifecycle()
    val transfer by viewModel.transfer.collectAsStateWithLifecycle()
    val receivedFiles by viewModel.receivedFiles.collectAsStateWithLifecycle()
    val computerEdge by viewModel.computerEdge.collectAsStateWithLifecycle()
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
        if (current.connected) {
            StreamingCard(current, streams, viewModel::shareScreen, viewModel::viewScreen, viewModel::listen, viewModel::open, viewModel::stopFeature)
            viewModel.media(states)?.let { NowPlayingCard(it, viewModel::send) }
            LaptopControlsCard(
                current.name,
                viewModel.volume(states),
                viewModel.hotspot(states),
                Feature.Hotspot in current.available,
                viewModel::send,
            )
        }
        FilesCard(transfer, receivedFiles, viewModel::dismissTransferFailure)
        FeaturesCard(current, viewModel::setFeature)
        if (Feature.Unicursor in current.enabled) CursorPlacementCard(current.name, computerEdge, viewModel::setComputerEdge)
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
private fun CursorPlacementCard(name: String, edge: Edge?, onSelect: (Edge) -> Unit) {
    SettingsCardTitled("Shared cursor") {
        SettingRow(
            "Where $name sits",
            description = "A mouse connected to this phone crosses over at that edge",
            icon = CrownIcons.MousePointer2,
        ) {
            CrownSelect(Edge.entries, edge, onSelect, { it.placementLabel }, placeholder = "Not set", width = PLACEMENT_SELECT_WIDTH)
        }
    }
}

private val PLACEMENT_SELECT_WIDTH = 132.dp

private val Edge.placementLabel: String
    get() = when (this) {
        Edge.Left -> "On the left"
        Edge.Right -> "On the right"
        Edge.Top -> "Above"
        Edge.Bottom -> "Below"
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
