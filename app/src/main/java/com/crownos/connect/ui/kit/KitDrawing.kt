package com.crownos.connect.ui.kit

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.crownos.connect.util.scaleAlpha

private const val FOCUS_RING_WIDTH_DP = 3f
const val FOCUS_RING_ALPHA = 0.5f

fun DrawScope.drawHairlineBorder(brush: Brush, radius: Float, topLeft: Offset = Offset.Zero, size: Size = this.size) {
    val hairline = 1f * density
    val inset = hairline / 2
    drawRoundRect(
        brush = brush,
        topLeft = topLeft + Offset(inset, inset),
        size = Size(size.width - hairline, size.height - hairline),
        cornerRadius = CornerRadius((radius - inset).coerceAtLeast(0f)),
        style = Stroke(hairline),
    )
}

fun DrawScope.drawHairlineBorder(color: Color, radius: Float, topLeft: Offset = Offset.Zero, size: Size = this.size) {
    if (color.alpha > 0f) drawHairlineBorder(Brush.linearGradient(listOf(color, color)), radius, topLeft, size)
}

fun DrawScope.drawOuterFocusRing(accentStart: Color, accentEnd: Color, radius: Float, progress: Float) {
    if (progress <= 0f) return
    val width = FOCUS_RING_WIDTH_DP * density
    val half = width / 2
    val alpha = FOCUS_RING_ALPHA * progress
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(accentStart.scaleAlpha(alpha), accentEnd.scaleAlpha(alpha)), -half, size.height + half),
        topLeft = Offset(-half, -half),
        size = Size(size.width + width, size.height + width),
        cornerRadius = CornerRadius(radius + half),
        style = Stroke(width),
    )
}

fun DrawScope.drawInnerFocusRing(accentStart: Color, accentEnd: Color, radius: Float, progress: Float) {
    if (progress <= 0f) return
    val width = FOCUS_RING_WIDTH_DP * density
    val half = width / 2
    val alpha = FOCUS_RING_ALPHA * progress
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(accentStart.scaleAlpha(alpha), accentEnd.scaleAlpha(alpha))),
        topLeft = Offset(half, half),
        size = Size(size.width - width, size.height - width),
        cornerRadius = CornerRadius((radius - half).coerceAtLeast(0f)),
        style = Stroke(width),
    )
}

fun outerGlowBrush(center: Offset, innerRadius: Float, outerRadius: Float, color: Color, rim: Float, halo: Float): Brush {
    val edge = innerRadius / outerRadius
    return Brush.radialGradient(
        0f to color.copy(alpha = 0f),
        (edge - 0.02f).coerceAtLeast(0f) to color.copy(alpha = rim),
        edge to color.copy(alpha = halo),
        1f to color.copy(alpha = 0f),
        center = center,
        radius = outerRadius,
    )
}

fun innerRingBrush(center: Offset, radius: Float, color: Color, strength: Float, start: Float): Brush =
    Brush.radialGradient(
        0f to color.copy(alpha = 0f),
        start to color.copy(alpha = 0f),
        1f to color.copy(alpha = strength.coerceIn(0f, 1f)),
        center = center,
        radius = radius,
    )
