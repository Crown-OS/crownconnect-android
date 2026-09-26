package com.crownos.connect.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.crownos.connect.util.scaleAlpha

enum class ThemeMode { System, Light, Dark }

private val LocalPalette = staticCompositionLocalOf { CrownPalette.Dark }
private val LocalAccent = staticCompositionLocalOf { CrownAccent.Purple.gradient }

object CrownTheme {
    val palette: CrownPalette
        @Composable @ReadOnlyComposable get() = LocalPalette.current

    val accent: Gradient
        @Composable @ReadOnlyComposable get() = LocalAccent.current
}

@Composable
fun CrownTheme(
    mode: ThemeMode = ThemeMode.System,
    accent: CrownAccent = CrownAccent.Purple,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val targetPalette = if (dark) CrownPalette.Dark else CrownPalette.Light
    val palette = rememberCrossFade(targetPalette) { from, to, t -> from.lerp(to, t) }
    val accentGradient = rememberCrossFade(accent.gradient) { from, to, t -> from.lerp(to, t) }
    val selection = TextSelectionColors(accentGradient.end, accentGradient.end.scaleAlpha(0.35f))

    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalAccent provides accentGradient,
        LocalTextSelectionColors provides selection,
        content = content,
    )
}

@Composable
private fun <T> rememberCrossFade(target: T, lerp: (T, T, Float) -> T): T {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect
        from = lerp(from, to, progress.value)
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, CrownMotion.ThemeFade)
    }
    return if (progress.value >= 1f) to else lerp(from, to, progress.value)
}
