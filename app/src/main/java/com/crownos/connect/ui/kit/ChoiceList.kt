package com.crownos.connect.ui.kit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownMotion
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType
import com.crownos.connect.ui.theme.Gradient
import com.crownos.connect.util.lerpPremultiplied
import com.crownos.connect.util.scaleAlpha

@Immutable
data class Choice<T>(
    val value: T,
    val title: String,
    val subtitle: String? = null,
    @DrawableRes val icon: Int? = null,
    val enabled: Boolean = true,
)

private val ROW_HEIGHT = 72.dp
private val ROW_GAP = 8.dp
private val ROW_RADIUS = 14.dp
private val ROW_PADDING = 14.dp
private val TILE_SIZE = 44.dp
private val TILE_RADIUS = 11.dp
private val RADIO_SIZE = 18.dp
private const val SELECTED_TINT = 0.07f
private const val TILE_SELECTED_ALPHA = 0.16f

@Composable
fun <T> CrownChoiceList(
    choices: List<Choice<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
        choices.forEach { choice -> ChoiceRow(choice, choice.value == selected) { onSelect(choice.value) } }
    }
}

@Composable
private fun <T> ChoiceRow(choice: Choice<T>, selected: Boolean, onClick: () -> Unit) {
    val palette = CrownTheme.palette
    val accent = CrownTheme.accent
    val interactions = remember { MutableInteractionSource() }
    val hovered = interactions.collectIsHoveredAsState().value || interactions.collectIsPressedAsState().value
    val hover = rememberKickedProgress(hovered && choice.enabled, CrownMotion.Snappy, CrownMotion.HOVER_KICK)
    val selection = rememberSelectionProgress(selected)

    Row(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .alpha(if (choice.enabled) 1f else 0.45f)
            .drawBehind {
                val radius = CornerRadius(ROW_RADIUS.toPx())
                val base = lerpPremultiplied(palette.surface.raised, palette.surface.hover, hover.value)
                drawRoundRect(lerpPremultiplied(base, accent.end, SELECTED_TINT * selection.value), cornerRadius = radius)
                val border = Gradient.flat(palette.surface.border).lerp(accent, selection.value)
                drawHairlineBorder(border.vertical(0f, size.height), ROW_RADIUS.toPx())
            }
            .selectable(selected, interactions, null, choice.enabled, Role.RadioButton, onClick)
            .padding(horizontal = ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        choice.icon?.let { icon ->
            Box(
                Modifier
                    .size(TILE_SIZE)
                    .drawBehind {
                        val tile = lerpPremultiplied(
                            palette.surface.sunken, accent.end.scaleAlpha(TILE_SELECTED_ALPHA), selection.value,
                        )
                        drawRoundRect(tile, cornerRadius = CornerRadius(TILE_RADIUS.toPx()))
                    },
                contentAlignment = Alignment.Center,
            ) {
                LucideIcon(
                    icon,
                    size = 22.dp,
                    color = lerpPremultiplied(palette.text.iconMuted, accent.end, selection.value),
                )
            }
            Spacer(Modifier.width(ROW_PADDING))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(
                choice.title,
                style = CrownType.choiceTitle.copy(color = palette.text.primary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            choice.subtitle?.let {
                BasicText(
                    it,
                    style = CrownType.choiceSubtitle.copy(color = palette.text.muted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (choice.enabled) {
            Spacer(Modifier.width(ROW_PADDING))
            Canvas(Modifier.size(RADIO_SIZE)) {
                drawRadioDisc(center, size.minDimension / 2, selection.value, palette.radio, accent)
            }
        }
    }
}
