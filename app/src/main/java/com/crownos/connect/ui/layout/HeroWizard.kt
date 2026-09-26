package com.crownos.connect.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.kit.CrownStepBar
import com.crownos.connect.ui.kit.SoftShadow
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType

private val CARD_WIDTH = 886.dp
private val CARD_HEIGHT = 574.dp
private val CARD_RADIUS = 22.dp
private val HERO_PANE_WIDTH = 366.dp
private val CONTENT_PADDING = 34.dp
private val COMPACT_HERO_HEIGHT = 220.dp

@Composable
fun HeroWizard(
    title: String,
    subtitle: String,
    steps: Int,
    completed: Int,
    hero: @Composable BoxScope.() -> Unit,
    action: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = CrownTheme.palette
    val accent = CrownTheme.accent
    val compact = LocalLayoutClass.current == LayoutClass.Compact
    val body: @Composable ColumnScope.() -> Unit = {
        BasicText(title, style = CrownType.heroTitle.copy(color = palette.text.primary))
        Spacer(Modifier.height(8.dp))
        BasicText(subtitle, style = CrownType.heroSubtitle.copy(color = palette.text.muted))
        if (steps > 0) {
            Spacer(Modifier.height(18.dp))
            CrownStepBar(steps, completed)
        }
        Spacer(Modifier.height(26.dp))
        content()
    }

    if (compact) {
        Column(Modifier.fillMaxSize().background(palette.surface.background)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(COMPACT_HERO_HEIGHT)
                    .clip(RoundedCornerShape(bottomStart = CARD_RADIUS, bottomEnd = CARD_RADIUS))
                    .drawBehind { drawRect(accent.vertical(0f, size.height)) }
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center,
                content = hero,
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                content = body,
            )
            Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp)) { action() }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(palette.surface.background), contentAlignment = Alignment.Center) {
        SoftShadow(CARD_RADIUS) {
            Row(Modifier.size(CARD_WIDTH, CARD_HEIGHT).clip(RoundedCornerShape(CARD_RADIUS)).background(palette.surface.raised)) {
                Box(
                    Modifier
                        .width(HERO_PANE_WIDTH)
                        .fillMaxHeight()
                        .drawBehind { drawRect(accent.vertical(0f, size.height)) },
                    contentAlignment = Alignment.Center,
                    content = hero,
                )
                Column(
                    Modifier.weight(1f).fillMaxHeight().padding(CONTENT_PADDING),
                    verticalArrangement = Arrangement.Top,
                ) {
                    body()
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.height(18.dp))
                    action()
                }
            }
        }
    }
}
