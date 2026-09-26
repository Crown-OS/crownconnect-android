package com.crownos.connect.util

import androidx.compose.ui.graphics.Color

fun lerpPremultiplied(from: Color, to: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    val alpha = from.alpha + (to.alpha - from.alpha) * t
    if (alpha <= 0f) return Color.Transparent
    val red = (from.red * from.alpha + (to.red * to.alpha - from.red * from.alpha) * t) / alpha
    val green = (from.green * from.alpha + (to.green * to.alpha - from.green * from.alpha) * t) / alpha
    val blue = (from.blue * from.alpha + (to.blue * to.alpha - from.blue * from.alpha) * t) / alpha
    return Color(red.coerceIn(0f, 1f), green.coerceIn(0f, 1f), blue.coerceIn(0f, 1f), alpha)
}

fun Color.scaleAlpha(factor: Float): Color = copy(alpha = alpha * factor.coerceIn(0f, 1f))
