package com.crownos.connect.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme

@Composable
fun CrownProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val track = CrownTheme.palette.control.track
    val accent = CrownTheme.accent
    Canvas(modifier.defaultMinSize(minWidth = 240.dp).height(8.dp)) {
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = corner)
        val filled = progress.coerceIn(0f, 1f) * size.width
        if (filled >= size.height) {
            drawRoundRect(accent.horizontal(0f, size.width), size = Size(filled, size.height), cornerRadius = corner)
        }
    }
}
