package com.crownos.connect.ui.screens.pairing

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crownos.connect.runtime.PairableComputer
import com.crownos.connect.runtime.PairingOutcome
import com.crownos.connect.ui.kit.ButtonSize
import com.crownos.connect.ui.kit.ButtonVariant
import com.crownos.connect.ui.kit.CrownButton
import com.crownos.connect.ui.kit.CrownCallout
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.CrownSpinner
import com.crownos.connect.ui.kit.Tone
import com.crownos.connect.ui.layout.SettingRow
import com.crownos.connect.ui.layout.SettingRowContent
import com.crownos.connect.ui.layout.SettingsCardTitled
import com.crownos.connect.ui.layout.SettingsDivider
import com.crownos.connect.ui.layout.SettingsPage
import com.crownos.connect.ui.screens.devices.icon
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType
import com.crownos.connect.util.Permissions

@Composable
fun AddDeviceScreen(onBack: (() -> Unit)?, onPaired: (String) -> Unit) {
    val viewModel = hiltViewModel<PairingViewModel>()
    val pairable by viewModel.pairable.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    var outcome by remember { mutableStateOf<PairingOutcome?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.results.collect { result ->
            outcome = result
            if (result.accepted) result.peer?.let(onPaired)
        }
    }

    SettingsPage("Add a device", CrownIcons.Plus, onBack = onBack) {
        when {
            pending -> PendingCallout()
            outcome?.accepted == false -> CrownCallout(
                "Pairing didn't complete",
                Tone.Warning,
                body = "Check that the computer is still in pairing mode, then try again.",
            )
        }
        ScanCard(viewModel::scanned)
        NearbyCard(pairable, pending, viewModel::pair)
    }
}

@Composable
private fun PendingCallout() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CrownSpinner()
        BasicText("Connecting…", style = CrownType.body.copy(color = CrownTheme.palette.text.body))
    }
}

@Composable
private fun ScanCard(onScanned: (String) -> Unit) {
    val context = LocalContext.current
    var cameraAllowed by remember { mutableStateOf(Permissions.granted(context, Manifest.permission.CAMERA)) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraAllowed = it }
    SettingsCardTitled("Scan a QR code") {
        SettingRow(
            "Open Cross-device on your computer",
            description = "Choose Pair a device in CrownOS Settings, then point this phone at the code.",
            icon = CrownIcons.QrCode,
        )
        SettingsDivider()
        SettingRowContent {
            if (cameraAllowed) {
                QrScanner(onScanned)
            } else {
                CrownButton("Allow camera", { request.launch(Manifest.permission.CAMERA) }, variant = ButtonVariant.Primary, icon = CrownIcons.Webcam)
            }
        }
    }
}

@Composable
private fun NearbyCard(pairable: List<PairableComputer>, pending: Boolean, onPair: (PairableComputer) -> Unit) {
    SettingsCardTitled("Nearby computers") {
        val nearby = pairable.filter { it.nearbyCode }
        if (nearby.isEmpty()) {
            SettingRow("Looking for computers in pairing mode", description = "They appear here while CrownOS Settings shows Pair a device.") {
                CrownSpinner()
            }
        }
        nearby.forEachIndexed { index, computer ->
            SettingRow(computer.deviceClass.name, description = computer.address, icon = computer.deviceClass.icon) {
                CrownButton("Pair", { onPair(computer) }, size = ButtonSize.Small, enabled = !pending)
            }
            if (index < nearby.lastIndex) SettingsDivider()
        }
    }
}
