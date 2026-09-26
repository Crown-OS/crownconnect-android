package com.crownos.connect.ui.kit

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownMotion
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.util.scaleAlpha

private const val TRACK_HEIGHT = 8f
private const val THUMB_WIDTH = 26f
private const val THUMB_HEIGHT = 20f
private const val THUMB_HALF_WIDTH = THUMB_WIDTH / 2
private const val SHADOW_INFLATE = 1.75f
private const val THUMB_BLOOM_ALPHA = 150f / 255f
private const val THUMB_BLOOM_SIGMA = 5f
private val WHITE_SMOKE = Color(0xFFF5F5F5)

@Composable
fun CrownSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
    enabled: Boolean = true,
) {
    val displayed = remember { Animatable(value.coerceIn(0f, 1f)) }
    LaunchedEffect(value) { displayed.animateTo(value.coerceIn(0f, 1f), CrownMotion.Snappy) }
    val palette = CrownTheme.palette
    val accent = CrownTheme.accent
    val latestChange by rememberUpdatedState(onValueChange)
    val latestFinish by rememberUpdatedState(onValueChangeFinished)
    val screenDensity = LocalDensity.current.density
    val bloomPaint = remember(screenDensity) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(sigmaToRadius(THUMB_BLOOM_SIGMA * screenDensity), BlurMaskFilter.Blur.NORMAL)
        }
    }

    fun fractionAt(x: Float, width: Float, density: Float): Float {
        val inset = THUMB_HALF_WIDTH * density
        return ((x - inset) / (width - inset * 2).coerceAtLeast(1f)).coerceIn(0f, 1f)
    }

    Canvas(
        modifier
            .widthIn(min = 120.dp)
            .height((THUMB_HEIGHT + 10).dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                if (enabled) setProgress { latestChange(it.coerceIn(0f, 1f)); true }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { latestChange(fractionAt(it.x, size.width.toFloat(), density)); latestFinish() }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = { latestFinish() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        latestChange(fractionAt(change.position.x, size.width.toFloat(), density))
                    },
                )
            },
    ) {
        val unit = density
        val inset = THUMB_HALF_WIDTH * unit
        val trackWidth = (size.width - inset * 2).coerceAtLeast(0f)
        val trackHeight = TRACK_HEIGHT * unit
        val trackTop = (size.height - trackHeight) / 2
        val corner = CornerRadius(trackHeight / 2)
        drawRoundRect(palette.control.track, Offset(inset, trackTop), Size(trackWidth, trackHeight), corner)

        val filled = displayed.value.coerceIn(0f, 1f) * trackWidth
        if (filled > 0f) {
            drawRoundRect(
                accent.horizontal(inset, inset + trackWidth),
                Offset(inset, trackTop),
                Size(filled, trackHeight),
                corner,
            )
        }
        drawThumb(Offset(inset + filled, size.height / 2), palette.control.knob, palette.control.knobShadow, bloomPaint)
    }
}

private fun DrawScope.drawThumb(center: Offset, knob: Color, knobShadow: Color, bloomPaint: Paint) {
    val unit = density
    val width = THUMB_WIDTH * unit
    val height = THUMB_HEIGHT * unit
    val topLeft = Offset(center.x - width / 2, center.y - height / 2)
    val corner = CornerRadius(height / 2)
    val shadowInflate = SHADOW_INFLATE * unit

    drawRoundRect(
        knobShadow,
        Offset(topLeft.x - shadowInflate, topLeft.y - shadowInflate),
        Size(width + shadowInflate * 2, height + shadowInflate * 2),
        CornerRadius(height / 2 + shadowInflate),
    )
    drawRoundRect(knob.scaleAlpha(0.85f), topLeft, Size(width, height), corner)
    drawRoundRect(innerRingBrush(center, width / 2, WHITE_SMOKE, 1.1f, 0.78f), topLeft, Size(width, height), corner)

    bloomPaint.color = knob.scaleAlpha(THUMB_BLOOM_ALPHA).toArgb()
    val bloomCorner = 5f * unit
    drawIntoCanvas {
        it.nativeCanvas.drawRoundRect(
            topLeft.x, topLeft.y, topLeft.x + width, topLeft.y + height, bloomCorner, bloomCorner, bloomPaint,
        )
    }
}

internal fun sigmaToRadius(sigma: Float): Float = ((sigma - 0.5f) / 0.57735f).coerceAtLeast(0.5f)
