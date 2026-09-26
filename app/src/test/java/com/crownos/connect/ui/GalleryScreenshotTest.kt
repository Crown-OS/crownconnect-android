package com.crownos.connect.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.crownos.connect.ui.layout.LayoutClassProvider
import com.crownos.connect.ui.layout.crownWindowBackground
import com.crownos.connect.ui.screens.gallery.GalleryScreen
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
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h2200dp-xxhdpi")
class GalleryScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture(mode: ThemeMode, name: String) {
        compose.setContent {
            CrownTheme(mode, CrownAccent.Purple) {
                LayoutClassProvider {
                    Box(Modifier.fillMaxSize().crownWindowBackground()) { GalleryScreen(mode, {}, CrownAccent.Purple, {}) }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Test
    fun galleryDark() = capture(ThemeMode.Dark, "gallery_dark")

    @Test
    fun galleryLight() = capture(ThemeMode.Light, "gallery_light")
}
