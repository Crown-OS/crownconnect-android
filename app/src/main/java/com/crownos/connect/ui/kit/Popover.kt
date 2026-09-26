package com.crownos.connect.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.platform.LocalDensity
import com.crownos.connect.ui.theme.CrownMotion

enum class PopoverAlignment { Start, End }

private val POPOVER_SLIDE = 8.dp

private class AnchoredPositionProvider(
    private val gapPx: Int,
    private val alignment: PopoverAlignment,
    private val onFlip: (Boolean) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val below = anchorBounds.bottom + gapPx
        val flipped = below + popupContentSize.height > windowSize.height &&
            anchorBounds.top - gapPx - popupContentSize.height >= 0
        onFlip(flipped)
        val y = if (flipped) anchorBounds.top - gapPx - popupContentSize.height else below
        val x = when (alignment) {
            PopoverAlignment.Start -> anchorBounds.left
            PopoverAlignment.End -> anchorBounds.right - popupContentSize.width
        }.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        return IntOffset(x, y)
    }
}

@Composable
fun AnchoredPopover(
    expanded: Boolean,
    onDismiss: () -> Unit,
    gap: Dp = 5.dp,
    alignment: PopoverAlignment = PopoverAlignment.Start,
    content: @Composable (Modifier) -> Unit,
) {
    val openness = remember { Animatable(0f) }
    var present by remember { mutableStateOf(expanded) }
    var flipped by remember { mutableStateOf(false) }
    LaunchedEffect(expanded) {
        if (expanded) present = true
        val kick = if (expanded) CrownMotion.POPOVER_KICK else -CrownMotion.POPOVER_KICK
        openness.animateTo(expanded.toProgress(), CrownMotion.Crisp, initialVelocity = kick)
        if (!expanded) present = false
    }
    if (!present) return

    val gapPx = with(LocalDensity.current) { gap.roundToPx() }
    val slidePx = with(LocalDensity.current) { POPOVER_SLIDE.toPx() }
    val provider = remember(gapPx, alignment) { AnchoredPositionProvider(gapPx, alignment) { flipped = it } }
    Popup(popupPositionProvider = provider, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        content(
            Modifier.graphicsLayer {
                val t = openness.value.coerceIn(0f, 1f)
                alpha = t
                scaleX = 0.95f + 0.05f * t
                scaleY = scaleX
                transformOrigin = TransformOrigin(0.5f, if (flipped) 1f else 0f)
                translationY = (if (flipped) slidePx else -slidePx) * (1 - t)
            },
        )
    }
}
