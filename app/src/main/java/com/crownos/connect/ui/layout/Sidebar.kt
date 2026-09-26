package com.crownos.connect.ui.layout

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.LucideIcon
import com.crownos.connect.ui.kit.animateSnappy
import com.crownos.connect.ui.kit.drawHairlineBorder
import com.crownos.connect.ui.kit.rememberInteractionProgress
import com.crownos.connect.ui.kit.toProgress
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType
import com.crownos.connect.util.lerpPremultiplied
import com.crownos.connect.util.scaleAlpha

private val ITEM_HEIGHT = 36.dp
private val ITEM_RADIUS = 16.dp
private val ICON_START = 10.dp
private val ICON_SIZE = 18.dp
private val LABEL_START = 38.dp
private val SUBITEM_LABEL_START = 30.dp
private val BRAND_HEIGHT = 44.dp
private const val CHEVRON_STROKE = 1.6f

@Composable
fun SidebarItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    trailing: @Composable (() -> Unit)? = null,
    showChevron: Boolean = false,
) {
    val colors = CrownTheme.palette.sidebar
    val interactions = remember { MutableInteractionSource() }
    val hover = rememberInteractionProgress(interactions).let { (it.hover + it.press).coerceAtMost(1f) }
    val selection = animateSnappy(selected.toProgress()).value

    Row(
        modifier
            .fillMaxWidth()
            .height(ITEM_HEIGHT)
            .drawBehind {
                val inset = 0.5.dp.toPx()
                val radius = ITEM_RADIUS.toPx()
                val hovered = lerpPremultiplied(Color.Transparent, colors.hoverBackground, hover)
                val fill = lerpPremultiplied(hovered, colors.selectedBackground, selection)
                val rectSize = Size(size.width - inset * 2, size.height - inset * 2)
                drawRoundRect(fill, Offset(inset, inset), rectSize, CornerRadius(radius))
                drawHairlineBorder(colors.selectedBorder.scaleAlpha(selection), radius, Offset(inset, inset), rectSize)
            }
            .clickable(interactions, null, role = Role.Tab, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Spacer(Modifier.width(ICON_START))
            LucideIcon(icon, size = ICON_SIZE, color = lerpPremultiplied(colors.icon, colors.selectedIcon, selection))
            Spacer(Modifier.width(LABEL_START - ICON_START - ICON_SIZE))
        } else {
            Spacer(Modifier.width(SUBITEM_LABEL_START))
        }
        BasicText(
            label,
            modifier = Modifier.weight(1f),
            style = CrownType.sidebarItem.copy(color = if (selected) colors.selectedText else colors.text),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.invoke()
        if (showChevron) {
            LucideIcon(
                CrownIcons.ChevronRight,
                Modifier.padding(start = 6.dp, end = 10.dp),
                size = 16.dp,
                strokeWidth = CHEVRON_STROKE,
                color = colors.icon,
            )
        } else if (trailing != null) {
            Spacer(Modifier.width(10.dp))
        }
    }
}

@Composable
fun SidebarBrand(label: String, @DrawableRes icon: Int, modifier: Modifier = Modifier) {
    val colors = CrownTheme.palette.sidebar
    Row(modifier.fillMaxWidth().height(BRAND_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(ICON_START))
        LucideIcon(icon, size = 22.dp, color = colors.brandText)
        Spacer(Modifier.width(42.dp - ICON_START - 22.dp))
        BasicText(label, style = CrownType.sidebarBrand.copy(color = colors.brandText), maxLines = 1)
    }
}

@Composable
fun SidebarSection(header: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        header?.let {
            Box(Modifier.fillMaxWidth().height(26.dp).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                BasicText(it, style = CrownType.sidebarSection.copy(color = CrownTheme.palette.sidebar.subitemText))
            }
            Spacer(Modifier.height(2.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
fun SidebarSeparator(modifier: Modifier = Modifier) {
    val color = CrownTheme.palette.sidebar.separator
    Canvas(modifier.fillMaxWidth().height(12.dp)) {
        val y = size.height / 2
        drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
    }
}
