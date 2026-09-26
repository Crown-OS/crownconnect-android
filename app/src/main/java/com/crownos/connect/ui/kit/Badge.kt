package com.crownos.connect.ui.kit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownType
import com.crownos.connect.util.scaleAlpha

@Composable
fun CrownBadge(text: String, tone: Tone, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null) {
    val color = tone.color
    Row(
        modifier
            .background(color.scaleAlpha(0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { LucideIcon(it, size = 12.dp, color = color) }
        BasicText(text, style = CrownType.small.copy(color = color), maxLines = 1)
    }
}
