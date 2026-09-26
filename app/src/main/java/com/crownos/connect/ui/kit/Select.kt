package com.crownos.connect.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownMotion
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType
import com.crownos.connect.util.scaleAlpha
import kotlin.math.roundToInt

private val TRIGGER_WIDTH = 252.dp
private val TRIGGER_HEIGHT = 34.dp
private val TRIGGER_RADIUS = 16.dp
private val PANEL_RADIUS = 18.dp
private val PANEL_INSET = 5.dp
private val ROW_HEIGHT = 30.dp
private val ROW_RADIUS = 16.dp
private val ROW_TEXT_START = 8.dp
private val CHECK_SIZE = 16.dp
private const val SWAP_THRESHOLD = 0.12f

@Composable
fun <T> CrownSelect(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "Select",
    width: Dp = TRIGGER_WIDTH,
    enabled: Boolean = true,
) {
    val palette = CrownTheme.palette
    var expanded by remember { mutableStateOf(false) }
    val selectedText = selected?.let(label)
    var shownText by remember { mutableStateOf(selectedText) }
    val valueAlpha = remember { Animatable(1f) }
    LaunchedEffect(selectedText) {
        if (selectedText == shownText) return@LaunchedEffect
        valueAlpha.animateTo(0f, CrownMotion.Crisp, -CrownMotion.VALUE_FADE_KICK) {
            if (value <= SWAP_THRESHOLD) shownText = selectedText
        }
        shownText = selectedText
        valueAlpha.animateTo(1f, CrownMotion.Crisp, CrownMotion.VALUE_FADE_KICK)
    }

    Box(modifier) {
        Box(
            Modifier
                .width(width)
                .height(TRIGGER_HEIGHT)
                .drawBehind {
                    val radius = TRIGGER_RADIUS.toPx()
                    drawRoundRect(palette.popover.triggerBackground, cornerRadius = CornerRadius(radius))
                    drawHairlineBorder(palette.popover.border, radius)
                }
                .clickable(remember { MutableInteractionSource() }, null, enabled, role = Role.DropdownList) {
                    expanded = true
                },
        ) {
            val muted = shownText == null || !enabled
            BasicText(
                shownText ?: placeholder,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp, end = 38.dp)
                    .alpha(valueAlpha.value),
                style = CrownType.body.copy(color = if (muted) palette.popover.mutedText else palette.popover.text),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            LucideIcon(
                CrownIcons.ChevronDown,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(20.dp),
                size = 20.dp,
                color = palette.popover.mutedText,
            )
        }
        AnchoredPopover(expanded, onDismiss = { expanded = false }) { motion ->
            SelectPanel(options, selected, label, width, motion) {
                onSelect(it)
                expanded = false
            }
        }
    }
}

@Composable
private fun <T> SelectPanel(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    width: Dp,
    modifier: Modifier,
    onPick: (T) -> Unit,
) {
    val palette = CrownTheme.palette
    val rowHeightPx = with(LocalDensity.current) { ROW_HEIGHT.toPx() }
    val selectedIndex = options.indexOf(selected)
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    val hoverAlpha = remember { Animatable(0f) }
    val hoverY = remember { Animatable(0f) }
    val checkY = remember { Animatable(selectedIndex.coerceAtLeast(0) * rowHeightPx) }
    LaunchedEffect(hoveredIndex) {
        if (hoveredIndex < 0) {
            hoverAlpha.animateTo(0f, CrownMotion.Snappy)
            return@LaunchedEffect
        }
        val target = hoveredIndex * rowHeightPx
        if (hoverAlpha.value <= 0f) hoverY.snapTo(target) else hoverY.animateTo(target, CrownMotion.Snappy)
    }
    LaunchedEffect(hoveredIndex >= 0) { if (hoveredIndex >= 0) hoverAlpha.animateTo(1f, CrownMotion.Snappy) }
    LaunchedEffect(selectedIndex) { checkY.animateTo(selectedIndex.coerceAtLeast(0) * rowHeightPx, CrownMotion.Snappy) }

    Column(
        modifier
            .width(width)
            .drawBehind {
                val radius = PANEL_RADIUS.toPx()
                drawRoundRect(palette.popover.background, cornerRadius = CornerRadius(radius))
                drawHairlineBorder(palette.popover.border, radius)
                val inset = PANEL_INSET.toPx()
                drawRoundRect(
                    palette.popover.hoverBackground.scaleAlpha(hoverAlpha.value),
                    Offset(inset, inset + hoverY.value),
                    Size(size.width - inset * 2, rowHeightPx),
                    CornerRadius(ROW_RADIUS.toPx()),
                )
            }
            .padding(PANEL_INSET),
    ) {
        Box {
            Column {
                options.forEachIndexed { index, option ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(ROW_HEIGHT)
                            .trackHover(index, onHover = { hoveredIndex = it })
                            .clickable(remember { MutableInteractionSource() }, null) { onPick(option) }
                            .padding(start = ROW_TEXT_START, end = CHECK_SIZE + 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicText(
                            label(option),
                            style = CrownType.body.copy(color = palette.popover.text),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (selectedIndex >= 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(0, checkY.value.roundToInt()) }
                        .height(ROW_HEIGHT)
                        .padding(end = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LucideIcon(CrownIcons.Check, size = CHECK_SIZE, color = palette.popover.text)
                }
            }
        }
    }
}

internal fun Modifier.trackHover(index: Int, onHover: (Int) -> Unit): Modifier = pointerInput(index) {
    awaitPointerEventScope {
        while (true) {
            when (awaitPointerEvent().type) {
                PointerEventType.Enter, PointerEventType.Press -> onHover(index)
                PointerEventType.Exit, PointerEventType.Release -> onHover(-1)
            }
        }
    }
}
