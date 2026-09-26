package com.crownos.connect.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object CrownMotion {
    private const val REST_THRESHOLD = 0.0005f

    val Snappy: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 320f, visibilityThreshold = REST_THRESHOLD)
    val Smooth: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 180f, visibilityThreshold = REST_THRESHOLD)
    val Crisp: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 900f, visibilityThreshold = REST_THRESHOLD)

    const val POPOVER_KICK = 12f
    const val SELECTION_KICK = 12f
    const val HOVER_KICK = 6f
    const val STEP_KICK = 6f
    const val VALUE_FADE_KICK = 12f

    const val THEME_FADE_MILLIS = 240
    const val SPINNER_TURNS_PER_SECOND = 0.85f

    val Smoothstep = Easing { t -> t * t * (3f - 2f * t) }
    val ThemeFade: TweenSpec<Float> = tween(THEME_FADE_MILLIS, easing = Smoothstep)
}
