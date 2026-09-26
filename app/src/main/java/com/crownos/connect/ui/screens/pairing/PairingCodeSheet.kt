package com.crownos.connect.ui.screens.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crownos.connect.runtime.PairingCodeRequest
import com.crownos.connect.ui.kit.ButtonVariant
import com.crownos.connect.ui.kit.CrownButton
import com.crownos.connect.ui.kit.CrownSheet
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType

@Composable
fun PairingCodeSheet(request: PairingCodeRequest?, onAnswer: (Boolean) -> Unit) {
    var shown by remember { mutableStateOf(request) }
    if (request != null) shown = request
    val palette = CrownTheme.palette
    CrownSheet(visible = request != null, onDismiss = { onAnswer(false) }) {
        val current = shown ?: return@CrownSheet
        BasicText("Pair with ${current.name}?", style = CrownType.headline.copy(color = palette.text.primary))
        Spacer(Modifier.height(6.dp))
        BasicText(
            "Make sure ${current.name} shows the same code.",
            style = CrownType.paragraph.copy(color = palette.text.muted),
        )
        Spacer(Modifier.height(20.dp))
        BasicText(
            current.code.chunked(3).joinToString(" "),
            modifier = Modifier.fillMaxWidth(),
            style = CrownType.pairingCode.copy(color = palette.text.primary, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CrownButton("Reject", { onAnswer(false) }, Modifier.weight(1f))
            CrownButton("Pair", { onAnswer(true) }, Modifier.weight(1f), variant = ButtonVariant.Primary)
        }
    }
}
