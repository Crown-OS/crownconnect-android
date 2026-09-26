package com.crownos.connect.ui.kit

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType
import com.crownos.connect.util.scaleAlpha

private val Tone.calloutIcon: Int
    get() = when (this) {
        Tone.Success -> CrownIcons.CircleCheck
        Tone.Warning -> CrownIcons.TriangleAlert
        Tone.Danger -> CrownIcons.OctagonAlert
        Tone.Neutral, Tone.Accent -> CrownIcons.Info
    }

@Composable
fun CrownCallout(title: String, tone: Tone, modifier: Modifier = Modifier, body: String? = null) {
    val color = tone.color
    val shape = RoundedCornerShape(8.dp)
    val palette = CrownTheme.palette
    Row(
        modifier
            .fillMaxWidth()
            .background(color.scaleAlpha(0.10f), shape)
            .border(1.dp, color.scaleAlpha(0.32f), shape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        LucideIcon(tone.calloutIcon, size = 16.dp, color = color)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(title, style = CrownType.calloutTitle.copy(color = palette.text.primary))
            body?.let { BasicText(it, style = CrownType.calloutBody.copy(color = palette.text.muted)) }
        }
    }
}
