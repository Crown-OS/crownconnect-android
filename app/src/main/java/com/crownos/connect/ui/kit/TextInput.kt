package com.crownos.connect.ui.kit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType
import com.crownos.connect.ui.theme.Gradient

private val FIELD_HEIGHT = 34.dp
private val FIELD_RADIUS = 10.dp
private val FIELD_PADDING = 10.dp
private val LEADING_ICON_SIZE = 17.dp
private const val LEADING_ICON_STROKE = 1.9f
private val LEADING_TEXT_START = 36.dp

@Composable
fun CrownTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    @DrawableRes leadingIcon: Int? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val palette = CrownTheme.palette
    val accent = CrownTheme.accent
    val interactions = remember { MutableInteractionSource() }
    val progress = rememberInteractionProgress(interactions, enabled)
    val textStyle = CrownType.body.copy(color = if (enabled) palette.popover.text else palette.popover.mutedText)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(FIELD_HEIGHT)
            .drawBehind {
                val radius = FIELD_RADIUS.toPx()
                drawRoundRect(palette.popover.triggerBackground, cornerRadius = CornerRadius(radius))
                val border = Gradient.flat(palette.popover.border).lerp(accent, progress.focus)
                drawHairlineBorder(border.vertical(0f, size.height), radius)
                drawInnerFocusRing(accent.start, accent.end, radius, progress.focus)
            },
        enabled = enabled,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(accent.end),
        interactionSource = interactions,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { field ->
            Row(Modifier.padding(horizontal = FIELD_PADDING), verticalAlignment = Alignment.CenterVertically) {
                leadingIcon?.let {
                    LucideIcon(it, size = LEADING_ICON_SIZE, strokeWidth = LEADING_ICON_STROKE, color = palette.popover.mutedText)
                    Spacer(Modifier.width(LEADING_TEXT_START - FIELD_PADDING - LEADING_ICON_SIZE))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        BasicText(placeholder, style = textStyle.copy(color = palette.popover.mutedText), maxLines = 1)
                    }
                    field()
                }
            }
        },
    )
}

@Composable
fun CrownSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) =
    CrownTextInput(value, onValueChange, modifier, placeholder = "Search…", leadingIcon = CrownIcons.Search)
