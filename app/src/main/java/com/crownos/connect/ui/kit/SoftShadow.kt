package com.crownos.connect.ui.kit

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme

private const val SHADOW_SIGMA = 10f
private val SHADOW_OFFSET_Y = 3.dp

@Composable
fun SoftShadow(cornerRadius: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val density = LocalDensity.current.density
    val shadow = CrownTheme.palette.surface.shadow
    val paint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(sigmaToRadius(SHADOW_SIGMA * density), BlurMaskFilter.Blur.NORMAL)
        }
    }
    Box(
        modifier.drawBehind {
            paint.color = shadow.toArgb()
            val radius = cornerRadius.toPx()
            val offset = SHADOW_OFFSET_Y.toPx()
            drawIntoCanvas {
                it.nativeCanvas.drawRoundRect(0f, offset, size.width, size.height + offset, radius, radius, paint)
            }
        },
    ) { content() }
}
