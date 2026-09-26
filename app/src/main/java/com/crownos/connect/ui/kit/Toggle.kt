package com.crownos.connect.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownMotion
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType
import kotlin.math.abs

private const val TRACK_WIDTH = 48f
private const val TRACK_HEIGHT = 24f
private const val KNOB_OUTER_WIDTH = 32f
private const val KNOB_PADDING = 2f
private const val KNOB_HEIGHT = TRACK_HEIGHT - 2 * KNOB_PADDING
private const val KNOB_BASE_WIDTH = KNOB_OUTER_WIDTH - 2 * KNOB_PADDING
private const val KNOB_GLOW_RADIUS = 4f
private const val KNOB_STRETCH_PER_VELOCITY = 0.15f
private const val KNOB_MAX_STRETCH = 0.1f
private val WHEAT = Color(0xFFF5DEB3)

@Composable
fun CrownToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    val progress = remember { Animatable(checked.toProgress()) }
    LaunchedEffect(checked) { progress.animateTo(checked.toProgress(), CrownMotion.Snappy) }
    val palette = CrownTheme.palette
    val accent = CrownTheme.accent

    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(TRACK_WIDTH.dp, TRACK_HEIGHT.dp)) {
            val t = progress.value.coerceIn(0f, 1f)
            val track = palette.control.toggleOff.lerp(accent, t)
            drawRoundRect(
                brush = track.vertical(0f, size.height),
                cornerRadius = CornerRadius(size.height / 2),
            )
            drawKnob(t, progress.velocity, palette.control.knob)
        }
        label?.let { BasicText(it, style = CrownType.body.copy(color = palette.text.body)) }
    }
}

private fun DrawScope.drawKnob(t: Float, velocity: Float, knobColor: Color) {
    val unit = density
    val stretch = (abs(velocity) * KNOB_STRETCH_PER_VELOCITY).coerceIn(0f, KNOB_MAX_STRETCH)
    val knobWidth = KNOB_BASE_WIDTH * (1 + stretch) * unit
    val knobHeight = KNOB_HEIGHT * unit
    val radius = knobHeight / 2
    val center = Offset((KNOB_OUTER_WIDTH / 2 + t * (TRACK_WIDTH - KNOB_OUTER_WIDTH)) * unit, size.height / 2)
    val glow = KNOB_GLOW_RADIUS * unit

    drawPill(center, knobWidth + glow * 2, knobHeight + glow * 2) { topLeft, pillSize, corner ->
        drawRoundRect(outerGlowBrush(center, radius, radius + glow, Color.Black, 0.14f, 0.08f), topLeft, pillSize, corner)
    }
    drawPill(center, knobWidth, knobHeight) { topLeft, pillSize, corner ->
        drawRoundRect(knobColor, topLeft, pillSize, corner)
        drawRoundRect(innerRingBrush(center, radius, WHEAT, 0.08f, 0.60f), topLeft, pillSize, corner)
    }
}

private inline fun DrawScope.drawPill(
    center: Offset,
    width: Float,
    height: Float,
    paint: DrawScope.(Offset, Size, CornerRadius) -> Unit,
) = paint(Offset(center.x - width / 2, center.y - height / 2), Size(width, height), CornerRadius(height / 2))
