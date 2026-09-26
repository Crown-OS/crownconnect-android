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
        RuntimePermissionsCard(AppPermission.entries.filter { it.applies }, viewModel)
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
