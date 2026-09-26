package com.crownos.connect.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme

@Immutable
data class Segment(val fraction: Float, val color: Color)

@Composable
fun CrownSegmentedBar(segments: List<Segment>, modifier: Modifier = Modifier, height: Dp = 14.dp) {
    val track = CrownTheme.palette.control.track
    Canvas(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50))) {
        drawRect(track)
        var x = 0f
        segments.forEach { segment ->
            val width = segment.fraction.coerceIn(0f, 1f) * size.width
            drawRect(segment.color, Offset(x, 0f), Size(width, size.height))
            x += width
        }
    }
}
