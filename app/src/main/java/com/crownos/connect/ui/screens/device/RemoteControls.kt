package com.crownos.connect.ui.screens.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crownos.connect.protocol.HotspotDocument
import com.crownos.connect.protocol.MediaCommand
import com.crownos.connect.protocol.MediaSessionDocument
import com.crownos.connect.protocol.PeerCommand
import com.crownos.connect.protocol.VolumeDocument
import com.crownos.connect.ui.kit.ButtonSize
import com.crownos.connect.ui.kit.ButtonVariant
import com.crownos.connect.ui.kit.CrownButton
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.CrownProgressBar
import com.crownos.connect.ui.kit.CrownSlider
import com.crownos.connect.ui.kit.CrownToggle
import com.crownos.connect.ui.kit.LucideIcon
import com.crownos.connect.ui.layout.SettingRow
import com.crownos.connect.ui.layout.SettingRowContent
import com.crownos.connect.ui.layout.SettingsCardTitled
import com.crownos.connect.ui.layout.SettingsDivider
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val VOLUME_SEND_DELAY_MILLIS = 80L

@Composable
fun NowPlayingCard(media: MediaSessionDocument, onCommand: (PeerCommand) -> Unit) {
    val palette = CrownTheme.palette
    SettingsCardTitled("Now playing") {
        SettingRowContent {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    BasicText(media.title.ifEmpty { media.app }, style = CrownType.choiceTitle.copy(color = palette.text.primary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicText(
                        listOf(media.artist, media.app).filter { it.isNotEmpty() }.joinToString(" · "),
                        style = CrownType.choiceSubtitle.copy(color = palette.text.muted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (media.durationMillis > 0) {
                    CrownProgressBar(media.positionMillis.toFloat() / media.durationMillis, Modifier.fillMaxWidth())
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                    CrownButton(null, { onCommand(PeerCommand.Media(MediaCommand.Previous)) }, variant = ButtonVariant.Ghost, icon = CrownIcons.SkipBack)
                    CrownButton(
                        null,
                        { onCommand(PeerCommand.Media(if (media.playing) MediaCommand.Pause else MediaCommand.Play)) },
                        variant = ButtonVariant.Primary,
                        icon = if (media.playing) CrownIcons.Pause else CrownIcons.Play,
                    )
                    CrownButton(null, { onCommand(PeerCommand.Media(MediaCommand.Next)) }, variant = ButtonVariant.Ghost, icon = CrownIcons.SkipForward)
                }
            }
        }
    }
}

@Composable
fun LaptopControlsCard(
    name: String,
    volume: VolumeDocument?,
    hotspot: HotspotDocument?,
    hotspotAllowed: Boolean,
    onCommand: (PeerCommand) -> Unit,
) {
    SettingsCardTitled("$name controls") {
        volume?.let {
            VolumeRow(it, onCommand)
            SettingsDivider()
        }
        if (hotspotAllowed && hotspot != null) {
            SettingRow("Hotspot", description = hotspot.ssid ?: if (hotspot.enabled) "On" else "Off", icon = CrownIcons.RadioTower) {
                CrownToggle(hotspot.enabled, { onCommand(PeerCommand.SetHotspot(it)) })
            }
            SettingsDivider()
        }
        SettingRow("Lock $name", icon = CrownIcons.Lock) {
            CrownButton("Lock", { onCommand(PeerCommand.Lock) }, size = ButtonSize.Small)
        }
    }
}

@Composable
private fun VolumeRow(volume: VolumeDocument, onCommand: (PeerCommand) -> Unit) {
    var draft by remember { mutableStateOf<Float?>(null) }
    val shown = draft ?: (volume.percent / 100f)
    LaunchedEffect(draft) {
        val value = draft ?: return@LaunchedEffect
        delay(VOLUME_SEND_DELAY_MILLIS)
        onCommand(PeerCommand.SetVolume((value * 100).roundToInt(), volume.muted))
    }
    val palette = CrownTheme.palette
    SettingRowContent {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LucideIcon(if (volume.muted) CrownIcons.VolumeX else CrownIcons.Volume2, size = 18.dp, color = palette.text.icon)
                Spacer(Modifier.width(10.dp))
                BasicText("Volume", Modifier.weight(1f), style = CrownType.rowTitle.copy(color = palette.text.body))
                CrownButton(
                    if (volume.muted) "Unmute" else "Mute",
                    { onCommand(PeerCommand.SetVolume(volume.percent, !volume.muted)) },
                    size = ButtonSize.Small,
                    variant = ButtonVariant.Ghost,
                )
            }
            CrownSlider(shown, { draft = it }, Modifier.fillMaxWidth(), onValueChangeFinished = { draft = null })
        }
    }
}
