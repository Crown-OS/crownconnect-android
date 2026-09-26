package com.crownos.connect.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme

@Composable
fun CrownDivider(modifier: Modifier = Modifier, height: Dp = 1.dp, color: Color = CrownTheme.palette.surface.border) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        val y = size.height / 2
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
    }
}
