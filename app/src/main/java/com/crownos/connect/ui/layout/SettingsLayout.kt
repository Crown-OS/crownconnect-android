package com.crownos.connect.ui.layout

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.kit.CrownCard
import com.crownos.connect.ui.kit.CrownDivider
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.LucideIcon
import com.crownos.connect.ui.kit.rememberInteractionProgress
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType
import com.crownos.connect.util.scaleAlpha

val PAGE_TITLE_GAP = 24.dp
val CARD_GAP = 24.dp
private val ICON_CHIP_SIZE = 34.dp
private val ICON_CHIP_RADIUS = 10.dp
private const val ICON_CHIP_TINT = 0.14f
private val ROW_PADDING_VERTICAL = 14.dp
private val ROW_PADDING_HORIZONTAL = 18.dp

@Composable
fun PageHeader(title: String, @DrawableRes icon: Int, modifier: Modifier = Modifier) {
    val accent = CrownTheme.accent.end
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(ICON_CHIP_SIZE).background(accent.scaleAlpha(ICON_CHIP_TINT), RoundedCornerShape(ICON_CHIP_RADIUS)),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(icon, size = 18.dp, color = accent)
        }
        BasicText(title, style = CrownType.pageTitle.copy(color = CrownTheme.palette.text.primary))
    }
}

@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    CrownCard(modifier, content)

@Composable
fun SettingsCardTitled(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(title.uppercase(), style = CrownType.sectionLabel.copy(color = CrownTheme.palette.text.muted))
        CrownCard(content = content)
    }
}

@Composable
fun SettingsDivider() = CrownDivider()

@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    @DrawableRes icon: Int? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val palette = CrownTheme.palette
    val interactions = remember { MutableInteractionSource() }
    val progress = rememberInteractionProgress(interactions, enabled = onClick != null)
    val wash = (0.7f * progress.hover + progress.press).coerceAtMost(1f)
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind { if (wash > 0f) drawRect(palette.surface.hover.scaleAlpha(wash)) }
            .then(
                if (onClick != null) Modifier.clickable(interactions, null, role = Role.Button, onClick = onClick) else Modifier,
            )
            .padding(vertical = ROW_PADDING_VERTICAL, horizontal = ROW_PADDING_HORIZONTAL),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            LucideIcon(it, size = 18.dp, color = palette.text.icon)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(title, style = CrownType.rowTitle.copy(color = palette.text.body))
            description?.let { BasicText(it, style = CrownType.rowDescription.copy(color = palette.text.muted)) }
        }
        Spacer(Modifier.width(12.dp))
        trailing()
        if (showChevron) {
            LucideIcon(
                CrownIcons.ChevronRight,
                Modifier.padding(start = 6.dp),
                size = 16.dp,
                strokeWidth = 1.6f,
                color = palette.text.iconMuted,
            )
        }
    }
}

@Composable
fun SettingRowContent(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().padding(vertical = ROW_PADDING_VERTICAL, horizontal = ROW_PADDING_HORIZONTAL),
        content = content,
    )
}

@Composable
fun ValueText(text: String) {
    BasicText(text, style = CrownType.value.copy(color = CrownTheme.palette.text.muted), maxLines = 1)
}
