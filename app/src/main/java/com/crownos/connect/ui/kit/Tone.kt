package com.crownos.connect.ui.kit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.crownos.connect.ui.theme.CrownTheme

enum class Tone { Success, Warning, Danger, Neutral, Accent }

val Tone.color: Color
    @Composable @ReadOnlyComposable get() = when (this) {
        Tone.Success -> CrownTheme.palette.status.success
        Tone.Warning -> CrownTheme.palette.status.warning
        Tone.Danger -> CrownTheme.palette.status.danger
        Tone.Neutral -> CrownTheme.palette.status.neutral
        Tone.Accent -> CrownTheme.accent.end
    }
