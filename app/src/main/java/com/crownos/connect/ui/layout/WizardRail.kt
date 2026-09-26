package com.crownos.connect.ui.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.LucideIcon
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType
import com.crownos.connect.util.scaleAlpha

private val DOT_SIZE = 24.dp
private val CONNECTOR_WIDTH = 2.dp
private val CURRENT_RING_RADIUS = 15.5.dp
private val CURRENT_RING_WIDTH = 2.dp
private const val PENDING_ALPHA = 0.55f

@Composable
fun WizardRail(steps: List<String>, current: Int, modifier: Modifier = Modifier) {
    val palette = CrownTheme.palette
    val accent = CrownTheme.accent.end
    val pending = palette.control.track.scaleAlpha(PENDING_ALPHA)

    Box(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(DOT_SIZE)) {
            val column = size.width / steps.size
            val radius = DOT_SIZE.toPx() / 2
            val y = size.height / 2
            for (index in 0 until steps.size - 1) {
                val start = column * (index + 0.5f) + radius
                val end = column * (index + 1.5f) - radius
                drawLine(if (index < current) accent else pending, Offset(start, y), Offset(end, y), CONNECTOR_WIDTH.toPx())
            }
            steps.indices.forEach { index ->
                val center = Offset(column * (index + 0.5f), y)
                drawCircle(if (index <= current) accent else pending, radius, center)
                if (index == current) {
                    drawCircle(accent, CURRENT_RING_RADIUS.toPx(), center, style = Stroke(CURRENT_RING_WIDTH.toPx()))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            steps.forEachIndexed { index, label ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.size(DOT_SIZE), contentAlignment = Alignment.Center) {
                        if (index < current) LucideIcon(CrownIcons.Check, size = 12.dp, strokeWidth = 2f, color = palette.text.onAccent)
                    }
                    val color = when {
                        index == current -> palette.text.primary
                        index < current -> palette.text.body
                        else -> palette.text.muted
                    }
                    BasicText(label, style = CrownType.small.copy(color = color, textAlign = TextAlign.Center), maxLines = 1)
                }
            }
        }
    }
}
