package com.crownos.connect.ui.kit

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownMotion
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.util.scaleAlpha

private const val HEAD_SWEEP_DEGREES = 90f

@Composable
fun CrownSpinner(modifier: Modifier = Modifier, size: Dp = 15.dp) {
    val turnMillis = (1000f / CrownMotion.SPINNER_TURNS_PER_SECOND).toInt()
    val rotation = rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(turnMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    val fadeIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fadeIn.animateTo(1f, CrownMotion.Snappy) }
    val track = CrownTheme.palette.control.track.scaleAlpha(0.55f)
    val head = CrownTheme.accent.end

    Canvas(modifier.size(size).alpha(fadeIn.value)) {
        val stroke = maxOf(1.dp.toPx(), 0.14f * this.size.minDimension)
        val inset = stroke / 2
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        drawArc(
            head, rotation.value - 90f, HEAD_SWEEP_DEGREES, false, Offset(inset, inset), arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}
