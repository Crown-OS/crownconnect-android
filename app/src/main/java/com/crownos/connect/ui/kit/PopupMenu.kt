package com.crownos.connect.ui.kit

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownMotion
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType
import com.crownos.connect.util.lerpPremultiplied
import com.crownos.connect.util.scaleAlpha

sealed interface MenuEntry {
    @Immutable
    data class Item(
        val label: String,
        val onClick: () -> Unit,
        val checked: Boolean = false,
        val destructive: Boolean = false,
        val enabled: Boolean = true,
    ) : MenuEntry

    data object Separator : MenuEntry
}

private val TRIGGER_WIDTH = 28.dp
private val TRIGGER_HEIGHT = 22.dp
private val TRIGGER_RADIUS = 8.dp
private val PANEL_RADIUS = 10.dp
private val PANEL_PADDING = 5.dp
private val ROW_HEIGHT = 28.dp
private val ROW_RADIUS = 6.dp
private val LABEL_START = 28.dp
private val LABEL_END = 18.dp
private val CHECK_START = 8.dp
private val SEPARATOR_HEIGHT = 7.dp

@Composable
fun CrownPopupMenu(
    entries: List<MenuEntry>,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = CrownIcons.Ellipsis,
    contentDescription: String? = null,
) {
    val palette = CrownTheme.palette
    var expanded by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val hovered = interactions.collectIsHoveredAsState().value
    val wash = animateSnappy((hovered || expanded).toProgress()).value

    Box(modifier) {
        Box(
            Modifier
                .size(TRIGGER_WIDTH, TRIGGER_HEIGHT)
                .drawBehind {
                    drawRoundRect(palette.surface.hover.scaleAlpha(wash), cornerRadius = CornerRadius(TRIGGER_RADIUS.toPx()))
                }
                .clickable(interactions, null, role = Role.Button) { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(
                icon,
                size = 16.dp,
                color = lerpPremultiplied(palette.text.iconMuted, palette.text.icon, wash),
                contentDescription = contentDescription,
            )
        }
        AnchoredPopover(expanded, onDismiss = { expanded = false }, alignment = PopoverAlignment.End) { motion ->
            MenuPanel(entries, motion) { expanded = false }
        }
    }
}

@Composable
private fun MenuPanel(entries: List<MenuEntry>, modifier: Modifier, onClose: () -> Unit) {
    val palette = CrownTheme.palette
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    Column(
        modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = 168.dp, max = 320.dp)
            .drawBehind {
                val radius = PANEL_RADIUS.toPx()
                drawRoundRect(palette.menu.background, cornerRadius = CornerRadius(radius))
                drawHairlineBorder(palette.menu.border, radius)
            }
            .padding(PANEL_PADDING),
    ) {
        entries.forEachIndexed { index, entry ->
            when (entry) {
                MenuEntry.Separator -> Canvas(Modifier.fillMaxWidth().height(SEPARATOR_HEIGHT)) {
                    val y = size.height / 2
                    drawLine(palette.menu.separator, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                is MenuEntry.Item -> MenuRow(entry, hoveredIndex == index, { hoveredIndex = if (it >= 0) index else -1 }) {
                    entry.onClick()
                    onClose()
                }
            }
        }
    }
}

@Composable
private fun MenuRow(item: MenuEntry.Item, hovered: Boolean, onHover: (Int) -> Unit, onClick: () -> Unit) {
    val palette = CrownTheme.palette
    val accent = CrownTheme.accent
    val highlight = remember { Animatable(0f) }
    LaunchedEffect(hovered) {
        if (hovered) highlight.snapTo(1f) else highlight.animateTo(0f, CrownMotion.Snappy)
    }
    val restingText = when {
        !item.enabled -> palette.menu.disabledText
        item.destructive -> palette.status.danger
        else -> palette.menu.text
    }
    val text = lerpPremultiplied(restingText, palette.menu.selectedText, if (item.enabled) highlight.value else 0f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .drawBehind {
                if (item.enabled) {
                    drawRoundRect(accent.end.scaleAlpha(highlight.value), cornerRadius = CornerRadius(ROW_RADIUS.toPx()))
                }
            }
            .trackHover(0, onHover)
            .clickable(remember { MutableInteractionSource() }, null, item.enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (item.checked) {
            LucideIcon(CrownIcons.Check, Modifier.padding(start = CHECK_START), size = 14.dp, color = text)
        }
        BasicText(
            item.label,
            modifier = Modifier.padding(start = LABEL_START, end = LABEL_END),
            style = CrownType.body.copy(color = text),
            maxLines = 1,
        )
    }
}
