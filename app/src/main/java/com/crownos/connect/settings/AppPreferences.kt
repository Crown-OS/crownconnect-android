package com.crownos.connect.settings

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import com.crownos.connect.protocol.enumByName
import com.crownos.connect.ui.theme.CrownAccent
import com.crownos.connect.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val FILE = "crownconnect"
private const val KEY_DEVICE_NAME = "device_name"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_ACCENT = "accent"
private const val KEY_ONBOARDED = "onboarded"
private const val MAX_NAME_LENGTH = 64

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val systemName = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        ?.takeIf { it.isNotBlank() } ?: Build.MODEL

    private val deviceNameState = MutableStateFlow(preferences.getString(KEY_DEVICE_NAME, null) ?: systemName)
    val deviceName: StateFlow<String> = deviceNameState.asStateFlow()

    private val themeModeState = MutableStateFlow(
        enumByName<ThemeMode>(preferences.getString(KEY_THEME_MODE, null)) ?: ThemeMode.System,
    )
    val themeMode: StateFlow<ThemeMode> = themeModeState.asStateFlow()

    private val accentState = MutableStateFlow(
        enumByName<CrownAccent>(preferences.getString(KEY_ACCENT, null)) ?: CrownAccent.Purple,
    )
    val accent: StateFlow<CrownAccent> = accentState.asStateFlow()

    private val onboardedState = MutableStateFlow(preferences.getBoolean(KEY_ONBOARDED, false))
    val onboarded: StateFlow<Boolean> = onboardedState.asStateFlow()

    fun setDeviceName(name: String) {
        val trimmed = name.trim().take(MAX_NAME_LENGTH).ifEmpty { systemName }
        deviceNameState.value = trimmed
        preferences.edit { putString(KEY_DEVICE_NAME, trimmed) }
    }

    fun setThemeMode(mode: ThemeMode) {
        themeModeState.value = mode
        preferences.edit { putString(KEY_THEME_MODE, mode.name) }
    }

    fun setAccent(accent: CrownAccent) {
        accentState.value = accent
        preferences.edit { putString(KEY_ACCENT, accent.name) }
    }

    fun setOnboarded() {
        onboardedState.value = true
        preferences.edit { putBoolean(KEY_ONBOARDED, true) }
    }
}
