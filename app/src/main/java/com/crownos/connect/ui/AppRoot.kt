package com.crownos.connect.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crownos.connect.protocol.BatteryDocument
import com.crownos.connect.runtime.PeerDevice
import com.crownos.connect.ui.glass.LocalRootBackdrop
import com.crownos.connect.ui.glass.glassSource
import com.crownos.connect.ui.glass.rememberGlassBackdrop
import com.crownos.connect.ui.layout.ExpandedShell
import com.crownos.connect.ui.layout.LayoutClass
import com.crownos.connect.ui.layout.LayoutClassProvider
import com.crownos.connect.ui.layout.SlideTransition
import com.crownos.connect.ui.layout.crownWindowBackground
import com.crownos.connect.ui.navigation.AppNavigator
import com.crownos.connect.ui.navigation.Screen
import com.crownos.connect.ui.navigation.rememberAppNavigator
import com.crownos.connect.ui.screens.AppViewModel
import com.crownos.connect.ui.screens.device.DeviceScreen
import com.crownos.connect.ui.screens.gallery.GalleryScreen
import com.crownos.connect.ui.screens.home.AppSidebar
import com.crownos.connect.ui.screens.home.HomeScreen
import com.crownos.connect.ui.screens.onboarding.OnboardingScreen
import com.crownos.connect.ui.screens.pairing.AddDeviceScreen
import com.crownos.connect.ui.screens.pairing.PairingCodeSheet
import com.crownos.connect.ui.screens.permissions.PermissionsScreen
import com.crownos.connect.ui.screens.settings.SettingsScreen
import com.crownos.connect.ui.screens.settings.SettingsViewModel
import com.crownos.connect.ui.theme.CrownTheme

@Composable
fun AppRoot() {
    val viewModel = hiltViewModel<AppViewModel>()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accent by viewModel.accent.collectAsStateWithLifecycle()
    val onboarded by viewModel.onboarded.collectAsStateWithLifecycle()
    val pairingRequest by viewModel.pairingRequest.collectAsStateWithLifecycle()
    val navigator = rememberAppNavigator()
    val backdrop = rememberGlassBackdrop()

    CrownTheme(themeMode, accent) {
        CompositionLocalProvider(LocalRootBackdrop provides backdrop) {
            LayoutClassProvider { layoutClass ->
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().glassSource(backdrop).crownWindowBackground()) {
                        if (onboarded) {
                            BackHandler(navigator.canGoBack) { navigator.pop() }
                            AppContent(viewModel, navigator, layoutClass)
                        } else {
                            OnboardingScreen { pairNow ->
                                viewModel.finishOnboarding()
                                if (pairNow) navigator.select(Screen.AddDevice)
                            }
                        }
                    }
                    PairingCodeSheet(pairingRequest, viewModel::confirmPairing)
                }
            }
        }
    }
}

@Composable
private fun AppContent(viewModel: AppViewModel, navigator: AppNavigator, layoutClass: LayoutClass) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val states by viewModel.remoteStates.collectAsStateWithLifecycle()
    val content: @Composable (Screen, (() -> Unit)?) -> Unit = { screen, onBack ->
        ScreenContent(screen, devices, { viewModel.battery(states[it]) }, navigator, onBack)
    }

    if (layoutClass == LayoutClass.Expanded) {
        val order = sidebarOrder(devices)
        val pane = navigator.current.takeIf { it != Screen.Home } ?: order.first()
        ExpandedShell(
            sidebar = { AppSidebar(devices, pane, navigator::select) },
            pane = { SlideTransition(pane, { order.indexOf(it).coerceAtLeast(0) }) { content(it, null) } },
        )
    } else {
        SlideTransition(navigator.current, navigator::depthOf) { screen ->
            content(screen, if (screen == Screen.Home) null else ({ navigator.pop() }))
        }
    }
}

private fun sidebarOrder(devices: List<PeerDevice>): List<Screen> =
    devices.map { Screen.Device(it.id) } + listOf(Screen.AddDevice, Screen.Permissions, Screen.Settings, Screen.Gallery)

@Composable
private fun ScreenContent(
    screen: Screen,
    devices: List<PeerDevice>,
    battery: (String) -> BatteryDocument?,
    navigator: AppNavigator,
    onBack: (() -> Unit)?,
) {
    when (screen) {
        Screen.Home -> HomeScreen(devices, battery, navigator::push)
        Screen.AddDevice -> AddDeviceScreen(onBack) { peer -> navigator.select(Screen.Device(peer)) }
        Screen.Settings -> SettingsScreen(onBack)
        Screen.Permissions -> PermissionsScreen(onBack)
        Screen.Gallery -> GalleryPage(onBack)
        is Screen.Device -> DeviceScreen(screen.id, onBack) { navigator.select(Screen.Home) }
    }
}

@Composable
private fun GalleryPage(onBack: (() -> Unit)?) {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accent by viewModel.accent.collectAsStateWithLifecycle()
    GalleryScreen(themeMode, viewModel::setThemeMode, accent, viewModel::setAccent, onBack)
}
