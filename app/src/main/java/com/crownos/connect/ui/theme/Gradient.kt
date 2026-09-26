package com.crownos.connect.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.crownos.connect.util.lerpPremultiplied
import com.crownos.connect.util.scaleAlpha

@Immutable
data class Gradient(val start: Color, val end: Color) {
    fun vertical(top: Float = 0f, bottom: Float = Float.POSITIVE_INFINITY): Brush =
        Brush.linearGradient(listOf(start, end), Offset(0f, top), Offset(0f, bottom))

    fun horizontal(left: Float, right: Float): Brush =
        Brush.linearGradient(listOf(start, end), Offset(left, 0f), Offset(right, 0f))

    fun scaleAlpha(factor: Float) = Gradient(start.scaleAlpha(factor), end.scaleAlpha(factor))

    fun lerp(to: Gradient, fraction: Float) =
        Gradient(lerpPremultiplied(start, to.start, fraction), lerpPremultiplied(end, to.end, fraction))

    companion object {
        fun flat(color: Color) = Gradient(color, color)
    }
}
