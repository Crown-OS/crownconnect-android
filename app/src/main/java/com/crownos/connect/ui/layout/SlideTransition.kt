package com.crownos.connect.ui.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import com.crownos.connect.ui.theme.CrownMotion

@Composable
fun <T> SlideTransition(
    page: T,
    order: (T) -> Int,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    var current by remember { mutableStateOf(page) }
    var outgoing by remember { mutableStateOf<T?>(null) }
    var direction by remember { mutableStateOf(1f) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(page) {
        if (page == current) return@LaunchedEffect
        direction = if (order(page) >= order(current)) 1f else -1f
        outgoing = current
        current = page
        progress.snapTo(0f)
        progress.animateTo(1f, CrownMotion.Snappy)
        outgoing = null
    }

    Box(modifier.clipToBounds()) {
        outgoing?.let { leaving ->
            key(leaving) {
                Box(Modifier.graphicsLayer { translationX = -size.width * direction * progress.value }) {
                    content(leaving)
                }
            }
        }
        key(current) {
            Box(Modifier.graphicsLayer { translationX = size.width * direction * (1f - progress.value) }) {
                content(current)
            }
        }
    }
}
