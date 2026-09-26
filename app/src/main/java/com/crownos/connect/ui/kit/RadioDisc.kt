package com.crownos.connect.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.crownos.connect.ui.theme.CrownMotion
import com.crownos.connect.ui.theme.Gradient
import com.crownos.connect.ui.theme.RadioColors
import com.crownos.connect.util.lerpPremultiplied

fun DrawScope.drawRadioDisc(center: Offset, radius: Float, selected: Float, colors: RadioColors, accent: Gradient) {
    val rim = Gradient.flat(colors.unselectedBorder).lerp(accent, selected)
    drawCircle(rim.vertical(center.y - radius, center.y + radius), radius, center)
    val holeRadius = radius - (1f + 4f * selected) * density
    drawCircle(lerpPremultiplied(colors.unselectedBackground, colors.hole, selected), holeRadius, center)
}

@Composable
fun rememberKickedProgress(active: Boolean, spec: AnimationSpec<Float>, kick: Float): Animatable<Float, *> {
    val progress = remember { Animatable(active.toProgress()) }
    LaunchedEffect(active) {
        progress.animateTo(active.toProgress(), spec, initialVelocity = if (active) kick else -kick)
    }
    return progress
}

@Composable
fun rememberSelectionProgress(selected: Boolean) =
    rememberKickedProgress(selected, CrownMotion.Crisp, CrownMotion.SELECTION_KICK)
