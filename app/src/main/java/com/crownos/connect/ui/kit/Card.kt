package com.crownos.connect.ui.kit

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme

val CARD_RADIUS = 14.dp

@Composable
fun CrownCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(CARD_RADIUS)
    val surface = CrownTheme.palette.surface
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface.raised)
            .border(1.dp, surface.border, shape),
        content = content,
    )
}
