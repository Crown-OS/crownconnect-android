package com.crownos.connect.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.settings.AppPreferences
import com.crownos.connect.ui.theme.CrownAccent
import com.crownos.connect.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val runtime: LltsRuntime,
) : ViewModel() {
    val deviceName = preferences.deviceName
    val themeMode = preferences.themeMode
    val accent = preferences.accent
    val status = runtime.status

    fun rename(name: String) {
        if (name.trim() == deviceName.value) return
        preferences.setDeviceName(name)
        runtime.restart()
    }

    fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)

    fun setAccent(accent: CrownAccent) = preferences.setAccent(accent)
}
