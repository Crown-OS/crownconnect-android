package com.crownos.connect.ui.screens.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.crownos.connect.media.MediaStream
import com.crownos.connect.protocol.CameraLens
import com.crownos.connect.protocol.Feature
import com.crownos.connect.protocol.FeatureRequest
import com.crownos.connect.protocol.MediaRole
import com.crownos.connect.runtime.PeerDevice
import com.crownos.connect.ui.kit.ButtonSize
import com.crownos.connect.ui.kit.ButtonVariant
import com.crownos.connect.ui.kit.CrownButton
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.CrownToggle
import com.crownos.connect.ui.layout.SettingRow
import com.crownos.connect.ui.layout.SettingsCardTitled
import com.crownos.connect.ui.layout.SettingsDivider

@Composable
fun StreamingCard(
    device: PeerDevice,
    streams: List<MediaStream>,
    onShare: (allowControl: Boolean) -> Unit,
    onView: () -> Unit,
    onListen: () -> Unit,
    onOpen: (MediaStream) -> Unit,
    onStop: (Feature) -> Unit,
) {
    val sharing = streams.firstOrNull { it.feature == Feature.Mirror && it.role == MediaRole.Encoder }
    val viewing = streams.firstOrNull { it.feature == Feature.Mirror && it.role == MediaRole.Decoder }
    val monitor = streams.firstOrNull { it.feature == Feature.Monitor && it.role == MediaRole.Decoder }
    val webcam = streams.firstOrNull { it.feature == Feature.Camera && it.role == MediaRole.Encoder }
    val microphone = streams.firstOrNull { it.feature == Feature.Mic && it.role == MediaRole.Encoder }
    val listening = streams.firstOrNull { it.feature == Feature.Mic && it.role == MediaRole.Decoder }
    val mirrorAllowed = Feature.Mirror in device.available
    var allowControl by rememberSaveable { mutableStateOf(true) }

    SettingsCardTitled("Streaming") {
        if (sharing != null) {
            SettingRow("Sharing this screen", description = "${device.name} can see this phone", icon = CrownIcons.ScreenShare) {
                StopButton { onStop(Feature.Mirror) }
            }
        } else {
            SettingRow("Share this screen", description = "Show this phone on ${device.name}", icon = CrownIcons.ScreenShare) {
                CrownButton("Share", { onShare(allowControl) }, size = ButtonSize.Small, variant = ButtonVariant.Primary, enabled = mirrorAllowed && viewing == null)
            }
            SettingsDivider()
            SettingRow("Allow control", description = "Let ${device.name} tap and type on this phone", icon = CrownIcons.MousePointer2) {
                CrownToggle(allowControl, { allowControl = it })
            }
        }
        SettingsDivider()
        if (viewing != null) {
            SettingRow("Viewing ${device.name}", icon = CrownIcons.Monitor) { OpenAndStop({ onOpen(viewing) }, { onStop(Feature.Mirror) }) }
        } else {
            SettingRow("View ${device.name}'s screen", description = "Watch and control it from this phone", icon = CrownIcons.Monitor) {
                CrownButton("View", onView, size = ButtonSize.Small, enabled = mirrorAllowed && sharing == null)
            }
        }
        monitor?.let {
            SettingsDivider()
            SettingRow("Second display", description = "${device.name} extends its desktop here", icon = CrownIcons.MonitorSmartphone) {
                OpenAndStop({ onOpen(it) }, { onStop(Feature.Monitor) })
            }
        }
        webcam?.let {
            val lens = (it.request as? FeatureRequest.Camera)?.lens ?: CameraLens.Back
            SettingsDivider()
            SettingRow("Webcam in use", description = "${lens.name} camera · ${it.width}×${it.height}", icon = CrownIcons.Webcam) {
                StopButton { onStop(Feature.Camera) }
            }
        }
        SettingsDivider()
        when {
            microphone != null -> SettingRow("Microphone in use", description = "${device.name} is listening", icon = CrownIcons.Mic) {
                StopButton { onStop(Feature.Mic) }
            }
            listening != null -> SettingRow("Listening to ${device.name}", description = "Its microphone plays on this phone", icon = CrownIcons.Volume2) {
                StopButton { onStop(Feature.Mic) }
            }
            else -> SettingRow("Listen to ${device.name}", description = "Play its microphone on this phone", icon = CrownIcons.Volume2) {
                CrownButton("Listen", onListen, size = ButtonSize.Small, enabled = Feature.Mic in device.available)
            }
        }
    }
}

@Composable
private fun StopButton(onStop: () -> Unit) =
    CrownButton("Stop", onStop, size = ButtonSize.Small, variant = ButtonVariant.Destructive)

@Composable
private fun OpenAndStop(onOpen: () -> Unit, onStop: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CrownButton("Open", onOpen, size = ButtonSize.Small)
        StopButton(onStop)
    }
}
