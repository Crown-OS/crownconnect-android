package com.crownos.connect.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType

private val ROW_PITCH = 30.dp
private val DISC_SIZE = 20.dp
private val LABEL_OFFSET = 35.dp

@Composable
fun <T> CrownRadioGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier.selectableGroup().alpha(if (enabled) 1f else 0.4f)) {
        options.forEach { option ->
            RadioRow(label(option), option == selected, enabled) { onSelect(option) }
        }
    }
}

@Composable
private fun RadioRow(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val progress = rememberSelectionProgress(selected)
    val colors = CrownTheme.palette.radio
    val accent = CrownTheme.accent
    Row(
        Modifier
            .height(ROW_PITCH)
            .selectable(selected, remember { MutableInteractionSource() }, null, enabled, Role.RadioButton, onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(DISC_SIZE)) {
            drawRadioDisc(center, size.minDimension / 2, progress.value, colors, accent)
        }
        Spacer(Modifier.width(LABEL_OFFSET - DISC_SIZE))
        BasicText(text, style = CrownType.body.copy(color = colors.text))
    }
}
