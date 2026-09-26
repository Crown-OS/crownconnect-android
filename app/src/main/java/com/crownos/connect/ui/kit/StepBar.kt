package com.crownos.connect.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownMotion
import com.crownos.connect.ui.theme.CrownTheme

private val TRACK_HEIGHT = 5.dp
private val TRACK_GAP = 10.dp

@Composable
fun CrownStepBar(steps: Int, completed: Int, modifier: Modifier = Modifier) {
    val filled = remember { Animatable(completed.toFloat()) }
    LaunchedEffect(completed) {
        val kick = if (completed > filled.value) CrownMotion.STEP_KICK else -CrownMotion.STEP_KICK
        filled.animateTo(completed.toFloat(), CrownMotion.Crisp, initialVelocity = kick)
    }
    val track = CrownTheme.palette.control.track
    val accent = CrownTheme.accent
    Canvas(modifier.fillMaxWidth().height(TRACK_HEIGHT)) {
        if (steps <= 0) return@Canvas
        val gap = TRACK_GAP.toPx()
        val width = (size.width - gap * (steps - 1)) / steps
        val corner = CornerRadius(size.height / 2)
        repeat(steps) { index ->
            val left = index * (width + gap)
            drawRoundRect(track, Offset(left, 0f), Size(width, size.height), corner)
            val coverage = (filled.value - index).coerceIn(0f, 1f)
            if (coverage > 0f) {
                clipRect(left, 0f, left + width * coverage, size.height) {
                    drawRoundRect(accent.horizontal(left, left + width), Offset(left, 0f), Size(width, size.height), corner)
                }
            }
        }
    }
}
