package com.crownos.connect.state

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.crownos.connect.protocol.HotspotDocument
import com.crownos.connect.protocol.Topic
import com.crownos.connect.protocol.encodeDocument
import com.crownos.connect.service.ServiceFeature
import com.crownos.connect.service.ServiceNotifications
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

private const val WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
private const val EXTRA_WIFI_AP_STATE = "wifi_state"
private const val WIFI_AP_STATE_ENABLED = 13

interface HotspotController {
    fun setEnabled(enabled: Boolean): Boolean
    fun ssid(): String?
}

@Singleton
class HotspotBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: LocalStateStore,
) : ServiceFeature {
    @Volatile
    var privilegedController: HotspotController? = null
    private var registered = false
    private var enabled = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = onState(intent)
    }

    override fun start(scope: CoroutineScope) {
        if (registered) return
        registered = true
        ContextCompat.registerReceiver(context, receiver, IntentFilter(WIFI_AP_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
            ?.let(::onState)
            ?: publish()
    }

    override fun stop() {
        if (!registered) return
        registered = false
        runCatching { context.unregisterReceiver(receiver) }
    }

    fun setEnabled(target: Boolean) {
        if (privilegedController?.setEnabled(target) == true) return
        ServiceNotifications.prompt(
            context,
            if (target) "Turn on the hotspot" else "Turn off the hotspot",
            "Your computer asked for this. Tap to open hotspot settings.",
            tetherSettingsIntent(),
        )
    }

    private fun onState(intent: Intent) {
        enabled = intent.getIntExtra(EXTRA_WIFI_AP_STATE, 0) == WIFI_AP_STATE_ENABLED
        publish()
    }

    fun publish() {
        val ssid = if (enabled) privilegedController?.ssid() else null
        store.publish(Topic.Hotspot, HotspotDocument.serializer().encodeDocument(HotspotDocument(enabled, ssid)))
    }

    private fun tetherSettingsIntent(): Intent {
        val tether = Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.TetherSettings"))
        val resolvable = tether.resolveActivity(context.packageManager) != null
        return (if (resolvable) tether else Intent(Settings.ACTION_WIRELESS_SETTINGS)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
