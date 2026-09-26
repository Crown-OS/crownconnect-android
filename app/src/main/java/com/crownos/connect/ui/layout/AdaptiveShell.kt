package com.crownos.connect.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme

enum class LayoutClass { Compact, Expanded }

val LocalLayoutClass = staticCompositionLocalOf { LayoutClass.Compact }

private val EXPANDED_MIN_WIDTH = 600.dp
private val SHELL_GUTTER = 12.dp
private val SIDEBAR_WIDTH = 260.dp
private val PANE_RADIUS = 16.dp
private val SIDEBAR_GROUP_GAP = 6.dp

@Composable
fun LayoutClassProvider(content: @Composable (LayoutClass) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layoutClass = if (maxWidth >= EXPANDED_MIN_WIDTH) LayoutClass.Expanded else LayoutClass.Compact
        CompositionLocalProvider(LocalLayoutClass provides layoutClass) { content(layoutClass) }
    }
}

@Composable
fun Modifier.crownWindowBackground(): Modifier = background(Color.Black).background(CrownTheme.palette.surface.background)

@Composable
fun SidebarColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.background(CrownTheme.palette.sidebar.background).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SIDEBAR_GROUP_GAP),
        content = content,
    )
}

@Composable
fun ExpandedShell(sidebar: @Composable ColumnScope.() -> Unit, pane: @Composable () -> Unit) {
    val surface = CrownTheme.palette.surface
    val paneShape = RoundedCornerShape(PANE_RADIUS)
    Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(SHELL_GUTTER)) {
        SidebarColumn(Modifier.width(SIDEBAR_WIDTH - SHELL_GUTTER).fillMaxHeight().padding(end = SHELL_GUTTER), sidebar)
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(paneShape)
                .background(surface.background)
                .border(1.dp, surface.border, paneShape),
        ) {
            pane()
        }
    }
}
