package com.crownos.connect.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType

@Composable
fun CrownStatus(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val dot = tone.color
    val label = if (tone == Tone.Neutral) CrownTheme.palette.text.muted else dot
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(7.dp)) { drawCircle(dot) }
        BasicText(text, style = CrownType.small.copy(color = label), maxLines = 1)
    }
}
