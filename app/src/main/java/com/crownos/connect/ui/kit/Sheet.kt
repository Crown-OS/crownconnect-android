package com.crownos.connect.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.glass.GlassStyle
import com.crownos.connect.ui.glass.LocalRootBackdrop
import com.crownos.connect.ui.glass.glass
import com.crownos.connect.ui.layout.LayoutClass
import com.crownos.connect.ui.layout.LocalLayoutClass
import com.crownos.connect.ui.theme.CrownMotion

private val SHEET_RADIUS = 22.dp
private val SHEET_MARGIN = 12.dp
private val SHEET_PADDING = 24.dp
private val SHEET_MAX_WIDTH = 440.dp
private const val SCRIM_ALPHA = 0.4f

@Composable
fun CrownSheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val openness = remember { Animatable(0f) }
    var present by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible) present = true
        openness.animateTo(visible.toProgress(), CrownMotion.Snappy)
        if (!visible) present = false
    }
    if (!present) return

    val compact = LocalLayoutClass.current == LayoutClass.Compact
    val backdrop = LocalRootBackdrop.current
    Box(Modifier.fillMaxSize(), contentAlignment = if (compact) Alignment.BottomCenter else Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = openness.value.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
        )
        val surface = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(SHEET_MARGIN)
            .widthIn(max = SHEET_MAX_WIDTH)
            .fillMaxWidth()
            .graphicsLayer {
                val t = openness.value.coerceIn(0f, 1f)
                if (compact) {
                    translationY = (1f - t) * (size.height + SHEET_MARGIN.toPx())
                } else {
                    alpha = t
                    scaleX = 0.95f + 0.05f * t
                    scaleY = scaleX
                    transformOrigin = TransformOrigin.Center
                }
            }
        Column(
            (if (backdrop != null) surface.glass(backdrop, SHEET_RADIUS, GlassStyle.Sheet) else surface)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .padding(SHEET_PADDING),
            content = content,
        )
    }
}
