package com.crownos.connect.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver

sealed interface Screen {
    val key: String

    data object Home : Screen { override val key = "home" }
    data object AddDevice : Screen { override val key = "add" }
    data object Settings : Screen { override val key = "settings" }
    data object Permissions : Screen { override val key = "permissions" }
    data object Gallery : Screen { override val key = "gallery" }
    data class Device(val id: String) : Screen { override val key = "device:$id" }

    companion object {
        fun fromKey(key: String): Screen = when {
            key.startsWith("device:") -> Device(key.removePrefix("device:"))
            key == AddDevice.key -> AddDevice
            key == Settings.key -> Settings
            key == Permissions.key -> Permissions
            key == Gallery.key -> Gallery
            else -> Home
        }
    }
}

@Stable
class AppNavigator(initial: List<Screen>) {
    private val stack = mutableStateListOf<Screen>().apply { addAll(initial.ifEmpty { listOf(Screen.Home) }) }

    val current: Screen get() = stack.last()
    val depth: Int get() = stack.size - 1
    val canGoBack: Boolean get() = stack.size > 1
    val keys: List<String> get() = stack.map { it.key }

    fun push(screen: Screen) {
        if (current != screen) stack += screen
    }

    fun select(screen: Screen) {
        stack.clear()
        stack += Screen.Home
        if (screen != Screen.Home) stack += screen
    }

    fun pop(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun depthOf(screen: Screen): Int = stack.indexOf(screen).takeIf { it >= 0 } ?: stack.size
}

@Composable
fun rememberAppNavigator(): AppNavigator = rememberSaveable(
    saver = listSaver(save = { it.keys }, restore = { keys -> AppNavigator(keys.map(Screen::fromKey)) }),
) { AppNavigator(listOf(Screen.Home)) }
