package com.crownos.connect.ui.layout

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.glass.GlassBorder
import com.crownos.connect.ui.glass.GlassStyle
import com.crownos.connect.ui.glass.glass
import com.crownos.connect.ui.glass.glassSource
import com.crownos.connect.ui.glass.rememberGlassBackdrop
import com.crownos.connect.ui.kit.ButtonSize
import com.crownos.connect.ui.kit.ButtonVariant
import com.crownos.connect.ui.kit.CrownButton
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.animateSnappy
import com.crownos.connect.ui.kit.toProgress
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType

private val WIDE_PAGE_PADDING = 32.dp
private val COMPACT_PAGE_PADDING = 20.dp
private val TOP_BAR_HEIGHT = 52.dp

@Composable
fun SettingsPage(
    title: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val compact = LocalLayoutClass.current == LayoutClass.Compact
    val padding = if (compact) COMPACT_PAGE_PADDING else WIDE_PAGE_PADDING
    val scroll = rememberScrollState()
    val backdrop = rememberGlassBackdrop()
    var headerBottom by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topInset = if (compact) statusBarTop + TOP_BAR_HEIGHT else 0.dp

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .glassSource(backdrop)
                .verticalScroll(scroll)
                .padding(top = topInset)
                .padding(padding)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(CARD_GAP),
        ) {
            PageHeader(title, icon, Modifier.onSizeChanged { headerBottom = it.height })
            content()
        }
        if (compact) {
            val titleShown = animateSnappy((scroll.value > headerBottom).toProgress()).value
            PageTopBar(title, titleShown, onBack, actions, Modifier.glass(backdrop, 0.dp, GlassStyle.Chrome, GlassBorder.Bottom))
        }
    }
}

@Composable
private fun PageTopBar(
    title: String,
    titleAlpha: Float,
    onBack: (() -> Unit)?,
    actions: @Composable () -> Unit,
    modifier: Modifier,
) {
    Box(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().height(TOP_BAR_HEIGHT).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                CrownButton(null, onBack, variant = ButtonVariant.Ghost, size = ButtonSize.Medium, icon = CrownIcons.ChevronLeft)
            } else {
                Spacer(Modifier.height(1.dp))
            }
            Spacer(Modifier.weight(1f))
            actions()
        }
        BasicText(
            title,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 72.dp).alpha(titleAlpha),
            style = CrownType.headline.copy(color = CrownTheme.palette.glass.label),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
