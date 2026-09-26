package com.crownos.connect.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crownos.connect.BuildConfig
import com.crownos.connect.runtime.RuntimeStatus
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.CrownSelect
import com.crownos.connect.ui.kit.CrownStatus
import com.crownos.connect.ui.kit.CrownTextInput
import com.crownos.connect.ui.kit.Tone
import com.crownos.connect.ui.layout.SettingRow
import com.crownos.connect.ui.layout.SettingRowContent
import com.crownos.connect.ui.layout.SettingsCardTitled
import com.crownos.connect.ui.layout.SettingsDivider
import com.crownos.connect.ui.layout.SettingsPage
import com.crownos.connect.ui.layout.ValueText
import com.crownos.connect.ui.theme.CrownAccent
import com.crownos.connect.ui.theme.ThemeMode

private val SELECT_WIDTH = 140.dp

@Composable
fun SettingsScreen(onBack: (() -> Unit)?) {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val name by viewModel.deviceName.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accent by viewModel.accent.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    SettingsPage("Settings", CrownIcons.Settings, onBack = onBack) {
        SettingsCardTitled("This phone") {
            SettingRow("Name", description = "What your computers call this phone")
            SettingRowContent { DeviceNameField(name, viewModel::rename) }
        }
        SettingsCardTitled("Appearance") {
            SettingRow("Theme", icon = CrownIcons.Sun) {
                CrownSelect(ThemeMode.entries, themeMode, viewModel::setThemeMode, { it.name }, width = SELECT_WIDTH)
            }
            SettingsDivider()
            SettingRow("Accent color", icon = CrownIcons.Palette) {
                CrownSelect(CrownAccent.entries, accent, viewModel::setAccent, CrownAccent::label, width = SELECT_WIDTH)
            }
        }
        SettingsCardTitled("About") {
            SettingRow("Connection service") { RuntimeStatusLabel(status) }
            (status as? RuntimeStatus.Running)?.let { running ->
                SettingsDivider()
                SettingRow("Device ID") { ValueText(running.deviceId.take(8)) }
                SettingsDivider()
                SettingRow("Port") { ValueText(running.port.toString()) }
            }
            SettingsDivider()
            SettingRow("Version") { ValueText(BuildConfig.VERSION_NAME) }
        }
    }
}

@Composable
private fun DeviceNameField(name: String, onRename: (String) -> Unit) {
    var draft by remember(name) { mutableStateOf(name) }
    val focus = LocalFocusManager.current
    CrownTextInput(
        draft,
        { draft = it },
        Modifier.fillMaxWidth(),
        placeholder = "Phone name",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onRename(draft)
            focus.clearFocus()
        }),
    )
}

@Composable
private fun RuntimeStatusLabel(status: RuntimeStatus) = when (status) {
    is RuntimeStatus.Running -> CrownStatus("Running", Tone.Success)
    RuntimeStatus.Starting -> CrownStatus("Starting", Tone.Warning)
    RuntimeStatus.Stopped -> CrownStatus("Stopped", Tone.Neutral)
    is RuntimeStatus.Failed -> CrownStatus(status.message, Tone.Danger)
}
