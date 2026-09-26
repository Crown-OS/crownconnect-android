package com.crownos.connect.ui.screens.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.kit.ButtonSize
import com.crownos.connect.ui.kit.ButtonVariant
import com.crownos.connect.ui.kit.Choice
import com.crownos.connect.ui.kit.CrownBadge
import com.crownos.connect.ui.kit.CrownButton
import com.crownos.connect.ui.kit.CrownCallout
import com.crownos.connect.ui.kit.CrownChoiceList
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.CrownPopupMenu
import com.crownos.connect.ui.kit.CrownProgressBar
import com.crownos.connect.ui.kit.CrownRadioGroup
import com.crownos.connect.ui.kit.CrownSearchField
import com.crownos.connect.ui.kit.CrownSegmentedBar
import com.crownos.connect.ui.kit.CrownSelect
import com.crownos.connect.ui.kit.CrownSlider
import com.crownos.connect.ui.kit.CrownSpinner
import com.crownos.connect.ui.kit.CrownStatus
import com.crownos.connect.ui.kit.CrownStepBar
import com.crownos.connect.ui.kit.CrownTextInput
import com.crownos.connect.ui.kit.CrownToggle
import com.crownos.connect.ui.kit.MenuEntry
import com.crownos.connect.ui.kit.Segment
import com.crownos.connect.ui.kit.Tone
import com.crownos.connect.ui.layout.SettingRow
import com.crownos.connect.ui.layout.SettingRowContent
import com.crownos.connect.ui.layout.SettingsCardTitled
import com.crownos.connect.ui.layout.SettingsDivider
import com.crownos.connect.ui.layout.SettingsPage
import com.crownos.connect.ui.layout.ValueText
import com.crownos.connect.ui.layout.WizardRail
import com.crownos.connect.ui.theme.CrownAccent
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.ThemeMode

@Composable
fun GalleryScreen(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    accent: CrownAccent,
    onAccent: (CrownAccent) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    SettingsPage("Widget gallery", CrownIcons.Palette, onBack = onBack) {
        SettingsCardTitled("Theme") {
            SettingRow("Appearance", description = "Cross-fades every palette slot") {
                CrownSelect(ThemeMode.entries, themeMode, onThemeMode, { it.name }, width = 140.dp)
            }
            SettingsDivider()
            SettingRow("Accent color") {
                CrownSelect(CrownAccent.entries, accent, onAccent, CrownAccent::label, width = 140.dp)
            }
        }
        ButtonsCard()
        ControlsCard()
        ChoicesCard()
        FeedbackCard()
    }
}

@Composable
private fun ButtonsCard() {
    SettingsCardTitled("Buttons") {
        SettingRowContent {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CrownButton("Primary", {}, variant = ButtonVariant.Primary)
                CrownButton("Secondary", {})
                CrownButton("Ghost", {}, variant = ButtonVariant.Ghost)
                CrownButton("Delete", {}, variant = ButtonVariant.Destructive, icon = CrownIcons.Trash2)
                CrownButton("Small", {}, size = ButtonSize.Small)
                CrownButton("Disabled", {}, enabled = false)
                CrownPopupMenu(
                    listOf(
                        MenuEntry.Item("Rename", {}),
                        MenuEntry.Item("Pinned", {}, checked = true),
                        MenuEntry.Separator,
                        MenuEntry.Item("Forget device", {}, destructive = true),
                    ),
                )
            }
        }
    }
}

@Composable
private fun ControlsCard() {
    var toggled by remember { mutableStateOf(true) }
    var level by remember { mutableFloatStateOf(0.4f) }
    var text by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    SettingsCardTitled("Controls") {
        SettingRow("Toggle", description = "Knob stretches with spring velocity") {
            CrownToggle(toggled, { toggled = it })
        }
        SettingsDivider()
        SettingRowContent { CrownSlider(level, { level = it }, Modifier.fillMaxWidth()) }
        SettingsDivider()
        SettingRowContent { CrownTextInput(text, { text = it }, Modifier.fillMaxWidth(), placeholder = "Device name") }
        SettingsDivider()
        SettingRowContent { CrownSearchField(query, { query = it }, Modifier.fillMaxWidth()) }
        SettingsDivider()
        SettingRow("Read-only value") { ValueText("47470") }
    }
}

@Composable
private fun ChoicesCard() {
    var radio by remember { mutableStateOf("Wi-Fi") }
    var choice by remember { mutableStateOf("laptop") }
    SettingsCardTitled("Choices") {
        SettingRowContent {
            CrownRadioGroup(listOf("Wi-Fi", "USB", "Bluetooth"), radio, { radio = it }, { it })
        }
        SettingsDivider()
        SettingRowContent {
            CrownChoiceList(
                listOf(
                    Choice("laptop", "CrownBook", "Connected over Wi-Fi", CrownIcons.Laptop),
                    Choice("desk", "Workstation", "Last seen yesterday", CrownIcons.Monitor),
                    Choice("tablet", "Tablet", "Unavailable", CrownIcons.TabletSmartphone, enabled = false),
                ),
                choice,
                { choice = it },
            )
        }
    }
}

@Composable
private fun FeedbackCard() {
    var step by remember { mutableIntStateOf(1) }
    val palette = CrownTheme.palette
    SettingsCardTitled("Feedback") {
        SettingRowContent {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    CrownBadge("Connected", Tone.Success, icon = CrownIcons.Wifi)
                    CrownBadge("Pairing", Tone.Accent)
                    CrownStatus("Online", Tone.Success)
                    CrownStatus("Offline", Tone.Neutral)
                    CrownSpinner()
                }
                CrownCallout("Screen sharing is active", Tone.Warning, body = "CrownBook is viewing this screen.")
                CrownProgressBar(0.62f, Modifier.fillMaxWidth())
                CrownSegmentedBar(
                    listOf(Segment(0.35f, CrownTheme.accent.end), Segment(0.2f, palette.status.success), Segment(0.1f, palette.status.warning)),
                )
                CrownStepBar(4, step)
                WizardRail(listOf("Welcome", "Access", "Pair", "Done"), step)
                CrownButton("Next step", { step = (step + 1) % 5 }, size = ButtonSize.Small)
            }
        }
    }
}
