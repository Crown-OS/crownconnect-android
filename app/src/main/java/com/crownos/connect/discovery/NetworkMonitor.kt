package com.crownos.connect.discovery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import androidx.core.content.ContextCompat
import com.crownos.connect.util.isRoutableForPeers
import com.crownos.connect.util.plainHost
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

private const val TETHER_STATE_CHANGED = "android.net.conn.TETHER_STATE_CHANGED"
private const val WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"

@Singleton
class NetworkMonitor @Inject constructor(@ApplicationContext private val context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private var listener: ((String) -> Unit)? = null
    private var lastReported: String? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = report()
        override fun onLost(network: Network) = report()
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) = report()
    }

    private val tetherReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = report()
    }

    fun start(onAddressesChanged: (String) -> Unit) {
        if (listener != null) return
        listener = onAddressesChanged
        connectivity.registerDefaultNetworkCallback(networkCallback)
        ContextCompat.registerReceiver(
            context,
            tetherReceiver,
            IntentFilter().apply {
                addAction(TETHER_STATE_CHANGED)
                addAction(WIFI_AP_STATE_CHANGED)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
        report(force = true)
    }

    fun stop() {
        if (listener == null) return
        runCatching { connectivity.unregisterNetworkCallback(networkCallback) }
        runCatching { context.unregisterReceiver(tetherReceiver) }
        listener = null
        lastReported = null
    }

    fun refresh() = report(force = true)

    private fun report(force: Boolean = false) {
        val addresses = JsonArray(currentAddresses().map(::JsonPrimitive)).toString()
        synchronized(this) {
            if (!force && addresses == lastReported) return
            lastReported = addresses
        }
        listener?.invoke(addresses)
    }

    private fun currentAddresses(): List<String> = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filter { it.isRoutableForPeers() }
            .mapNotNull { it.plainHost() }
            .distinct()
    }.getOrDefault(emptyList())
}
