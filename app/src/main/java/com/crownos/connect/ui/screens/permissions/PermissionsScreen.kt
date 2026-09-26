package com.crownos.connect.ui.screens.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crownos.connect.permissions.AppPermission
import com.crownos.connect.permissions.SpecialAccess
import com.crownos.connect.privileged.ShizukuStatus
import com.crownos.connect.ui.kit.ButtonVariant
import com.crownos.connect.ui.kit.CrownCallout
import com.crownos.connect.ui.kit.CrownSpinner
import com.crownos.connect.ui.layout.ValueText
import com.crownos.connect.ui.kit.ButtonSize
import com.crownos.connect.ui.kit.CrownButton
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.CrownStatus
import com.crownos.connect.ui.kit.Tone
import com.crownos.connect.ui.layout.SettingRow
import com.crownos.connect.ui.layout.SettingsCardTitled
import com.crownos.connect.ui.layout.SettingsDivider
import com.crownos.connect.ui.layout.SettingsPage

@Composable
fun PermissionsScreen(onBack: (() -> Unit)?) {
    val viewModel = hiltViewModel<PermissionsViewModel>()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    SettingsPage("Permissions", CrownIcons.ShieldCheck, onBack = onBack) {
        ShizukuCard(viewModel)
        RuntimePermissionsCard(AppPermission.entries.filter { it.applies }, viewModel)
        SpecialAccessCard(viewModel)
    }
}

@Composable
private fun SpecialAccessCard(viewModel: PermissionsViewModel) {
    val granted by viewModel.special.collectAsStateWithLifecycle()
    SettingsCardTitled("Special access") {
        SpecialAccess.entries.forEachIndexed { index, access ->
            SettingRow(access.title, description = access.description, icon = access.icon) {
                if (access in granted) {
                    CrownStatus("Allowed", Tone.Success)
                } else {
                    CrownButton("Open", { viewModel.open(access) }, size = ButtonSize.Small)
                }
            }
            if (index < SpecialAccess.entries.lastIndex) SettingsDivider()
        }
    }
}

@Composable
fun RuntimePermissionsCard(permissions: List<AppPermission>, viewModel: PermissionsViewModel, title: String = "App permissions") {
    val granted by viewModel.granted.collectAsStateWithLifecycle()
    SettingsCardTitled(title) {
        permissions.forEachIndexed { index, permission ->
            PermissionRow(permission, permission in granted, viewModel::refresh)
            if (index < permissions.lastIndex) SettingsDivider()
        }
    }
}

@Composable
private fun PermissionRow(permission: AppPermission, granted: Boolean, onResult: () -> Unit) {
    val context = LocalContext.current
    var asked by remember { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        asked = true
        onResult()
    }
    SettingRow(permission.title, description = permission.description, icon = permission.icon) {
        when {
            granted -> CrownStatus("Allowed", Tone.Success)
            asked -> CrownButton("Open settings", {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }, size = ButtonSize.Small)
            else -> CrownButton("Allow", { request.launch(permission.permissions.toTypedArray()) }, size = ButtonSize.Small)
        }
    }
}

@Composable
private fun ShizukuCard(viewModel: PermissionsViewModel) {
    val status = viewModel.shizukuStatus.collectAsStateWithLifecycle().value
    val granting by viewModel.granting.collectAsStateWithLifecycle()
    val report by viewModel.grantReport.collectAsStateWithLifecycle()
    report?.let {
        if (it.failed.isEmpty()) {
            CrownCallout("Everything is set up", Tone.Success, body = "Shizuku granted ${it.granted} permissions.")
        } else {
            CrownCallout("Some grants failed", Tone.Warning, body = it.failed.joinToString("\n"))
        }
    }
    SettingsCardTitled("Privileged access") {
        SettingRow(
            "Shizuku",
            description = when (status) {
                ShizukuStatus.NotInstalled -> "Install Shizuku to control this phone fully without root"
                ShizukuStatus.NotRunning -> "Start Shizuku, then come back"
                ShizukuStatus.NeedsPermission -> "Allow CrownConnect to use Shizuku"
                is ShizukuStatus.Ready -> "Running as ${if (status.asRoot) "root" else "shell"}"
            },
            icon = CrownIcons.KeyRound,
        ) {
            when (status) {
                ShizukuStatus.NotInstalled -> CrownButton("Get", viewModel::installShizuku, size = ButtonSize.Small)
                ShizukuStatus.NotRunning -> CrownButton("Open", viewModel::openShizuku, size = ButtonSize.Small)
                ShizukuStatus.NeedsPermission -> CrownButton("Allow", viewModel::requestShizuku, size = ButtonSize.Small, variant = ButtonVariant.Primary)
                is ShizukuStatus.Ready -> CrownStatus("Connected", Tone.Success)
            }
        }
        if (status is ShizukuStatus.Ready) {
            SettingsDivider()
            SettingRow("Set up everything", description = "Grant every permission and special access in one step", icon = CrownIcons.Sparkles) {
                if (granting) CrownSpinner() else CrownButton("Grant", viewModel::grantEverything, size = ButtonSize.Small, variant = ButtonVariant.Primary)
            }
        }
        SettingsDivider()
        SettingRow("Remote control", description = "How your computer taps and types on this phone", icon = CrownIcons.MousePointer2) {
            ValueText(viewModel.remoteControl)
        }
    }
}
