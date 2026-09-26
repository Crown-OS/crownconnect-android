package com.crownos.connect.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.crownos.connect.protocol.BatteryDocument
import com.crownos.connect.protocol.DeviceClass
import com.crownos.connect.protocol.Feature
import com.crownos.connect.runtime.PairingCodeRequest
import com.crownos.connect.runtime.PeerDevice
import com.crownos.connect.ui.glass.LocalRootBackdrop
import com.crownos.connect.ui.glass.glassSource
import com.crownos.connect.ui.glass.rememberGlassBackdrop
import com.crownos.connect.ui.layout.LayoutClassProvider
import com.crownos.connect.ui.layout.crownWindowBackground
import com.crownos.connect.ui.screens.home.HomeScreen
import com.crownos.connect.ui.screens.pairing.PairingCodeSheet
import com.crownos.connect.ui.theme.CrownAccent
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.ThemeMode
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreensScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val devices = listOf(
        PeerDevice("a".repeat(64), "CrownBook", DeviceClass.Computer, true, Feature.entries.toSet(), Feature.entries.toSet(), setOf(Feature.Camera)),
        PeerDevice("b".repeat(64), "Workstation", DeviceClass.Computer, false, Feature.entries.toSet(), emptySet(), emptySet()),
    )

    private fun capture(name: String, mode: ThemeMode, request: PairingCodeRequest?) {
        compose.setContent {
            CrownTheme(mode, CrownAccent.Purple) {
                val backdrop = rememberGlassBackdrop()
                CompositionLocalProvider(LocalRootBackdrop provides backdrop) {
                    LayoutClassProvider {
                        Box(Modifier.fillMaxSize()) {
                            Box(Modifier.fillMaxSize().glassSource(backdrop).crownWindowBackground()) {
                                HomeScreen(devices, { if (it == devices[0].id) BatteryDocument(72, true) else null }) {}
                            }
                            PairingCodeSheet(request) {}
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Test
    fun homeDark() = capture("home_dark", ThemeMode.Dark, null)

    @Test
    fun pairingSheetLight() = capture("pairing_light", ThemeMode.Light, PairingCodeRequest("c".repeat(64), "CrownBook", "042917"))
}
