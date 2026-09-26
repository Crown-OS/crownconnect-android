package com.crownos.connect.ui.kit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType
import com.crownos.connect.ui.theme.Gradient
import com.crownos.connect.util.scaleAlpha

enum class ButtonVariant { Primary, Secondary, Ghost, Destructive }

enum class ButtonSize(val height: Dp, val horizontalPadding: Dp, val radius: Dp, val iconSize: Dp) {
    Small(24.dp, 10.dp, 8.dp, 13.dp),
    Medium(30.dp, 14.dp, 10.dp, 15.dp);

    val textStyle: TextStyle get() = if (this == Small) CrownType.buttonSmall else CrownType.buttonMedium
}

private const val HOVER_WASH = 0.7f
private const val PRESS_WASH = 1.0f
private val ICON_GAP = 6.dp

private data class ButtonSkin(val fill: Gradient, val border: Color?, val content: Color)

@Composable
private fun resolveSkin(variant: ButtonVariant, enabled: Boolean): ButtonSkin {
    val palette = CrownTheme.palette
    val skin = when (variant) {
        ButtonVariant.Primary -> ButtonSkin(CrownTheme.accent, null, palette.text.onAccent)
        ButtonVariant.Secondary -> ButtonSkin(
            Gradient.flat(palette.popover.triggerBackground), palette.popover.border, palette.text.body,
        )
        ButtonVariant.Ghost -> ButtonSkin(Gradient.flat(Color.Transparent), null, palette.text.body)
        ButtonVariant.Destructive -> ButtonSkin(
            Gradient.flat(palette.status.dangerBackground), null, palette.status.onDanger,
        )
    }
    if (enabled) return skin
    return ButtonSkin(skin.fill.scaleAlpha(0.4f), skin.border?.scaleAlpha(0.6f), palette.text.disabled)
}

@Composable
fun CrownButton(
    label: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Secondary,
    size: ButtonSize = ButtonSize.Medium,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    val interactions = remember { MutableInteractionSource() }
    val progress = rememberInteractionProgress(interactions, enabled)
    val skin = resolveSkin(variant, enabled)
    val hover = CrownTheme.palette.surface.hover
    val accent = CrownTheme.accent

    Row(
        modifier = modifier
            .height(size.height)
            .drawBehind {
                val radius = size.radius.toPx()
                drawRoundRect(skin.fill.vertical(0f, this.size.height), cornerRadius = CornerRadius(radius))
                if (enabled) {
                    val wash = (HOVER_WASH * progress.hover + PRESS_WASH * progress.press).coerceAtMost(1f)
                    if (wash > 0f) drawRoundRect(hover.scaleAlpha(wash), cornerRadius = CornerRadius(radius))
                    drawOuterFocusRing(accent.start, accent.end, radius, progress.focus)
                }
                skin.border?.let { drawHairlineBorder(it, radius) }
            }
            .clickable(interactions, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = size.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(ICON_GAP, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { LucideIcon(it, size = size.iconSize, color = skin.content) }
        label?.let { BasicText(it, style = size.textStyle.copy(color = skin.content), maxLines = 1) }
    }
}
