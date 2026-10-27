package com.crownos.connect.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.crownos.connect.runtime.DeviceRepository
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.service.ForegroundLauncher
import com.crownos.connect.settings.AppPreferences
import com.crownos.connect.ui.theme.CrownTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RemoteDisplayActivity : ComponentActivity() {
    @Inject lateinit var runtime: LltsRuntime
    @Inject lateinit var streams: MediaStreams
    @Inject lateinit var devices: DeviceRepository
    @Inject lateinit var preferences: AppPreferences
    @Inject lateinit var launcher: ForegroundLauncher

    private var streamKey by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        launcher.dismissPrompt()
        streamKey = keyOf(intent)
        watchForStreamEnd()
        setContent {
            val themeMode by preferences.themeMode.collectAsStateWithLifecycle()
            val accent by preferences.accent.collectAsStateWithLifecycle()
            val all by streams.streams.collectAsStateWithLifecycle()
            val names by devices.devices.collectAsStateWithLifecycle()
            val stream = all[streamKey] ?: return@setContent
            CrownTheme(themeMode, accent) {
                RemoteDisplayView(stream, names.firstOrNull { it.id == stream.peer }?.name ?: "Computer", runtime) {
                    runtime.stopFeature(stream.peer, stream.feature)
                    finish()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        streamKey = keyOf(intent)
    }

    private fun watchForStreamEnd() = lifecycleScope.launch {
        streams.streams.first { streamKey in it }
        streams.streams.first { streamKey !in it }
        finish()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun keyOf(intent: Intent) = "${intent.getStringExtra(EXTRA_PEER)}/${intent.getIntExtra(EXTRA_INDEX, -1)}"

    companion object {
        private const val EXTRA_PEER = "peer"
        private const val EXTRA_INDEX = "index"

        fun intent(context: Context, stream: MediaStream) = Intent(context, RemoteDisplayActivity::class.java)
            .putExtra(EXTRA_PEER, stream.peer)
            .putExtra(EXTRA_INDEX, stream.index)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
