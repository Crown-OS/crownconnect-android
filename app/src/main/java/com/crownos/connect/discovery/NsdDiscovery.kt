package com.crownos.connect.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.crownos.connect.util.hexToBytesOrNull
import com.crownos.connect.util.toHex
import com.crownos.connect.util.toSocketAddressString
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "NsdDiscovery"
private const val SERVICE_TYPE = "_llts._udp"
private const val TXT_BEACON = "b"
private const val TXT_PORT = "p"
private const val INSTANCE_NAME_BYTES = 8
private const val RESOLVE_TIMEOUT_MILLIS = 5_000L

fun interface PresenceSink {
    fun onPresence(beacon: ByteArray, addressesJson: String, seen: Boolean)
}

@Singleton
class NsdDiscovery @Inject constructor(@ApplicationContext context: Context) {
    private val nsd = context.getSystemService(NsdManager::class.java)
    private val multicastLock = context.getSystemService(WifiManager::class.java)
        .createMulticastLock("crownconnect-nsd").apply { setReferenceCounted(false) }
    private val random = SecureRandom()
    private val resolverExecutor = Executors.newSingleThreadExecutor()

    private val registrations = mutableListOf<NsdManager.RegistrationListener>()
    private val ownInstanceNames = ConcurrentHashMap.newKeySet<String>()
    private val ownBeaconHex = ConcurrentHashMap.newKeySet<String>()
    private val beaconsByService = ConcurrentHashMap<String, ByteArray>()
    private val resolveQueue = Channel<NsdServiceInfo>(Channel.UNLIMITED)
    private var resolveJob: Job? = null
    private var browseListener: NsdManager.DiscoveryListener? = null
    private var sink: PresenceSink? = null

    @Synchronized
    fun advertise(beacons: List<ByteArray>, port: Int) {
        registrations.forEach { runCatching { nsd.unregisterService(it) } }
        registrations.clear()
        ownInstanceNames.clear()
        ownBeaconHex.clear()
        beacons.forEach { beacon ->
            val hex = beacon.toHex()
            ownBeaconHex += hex
            val info = NsdServiceInfo().apply {
                serviceName = randomInstanceName().also { ownInstanceNames += it }
                serviceType = SERVICE_TYPE
                setPort(port)
                setAttribute(TXT_BEACON, hex)
                setAttribute(TXT_PORT, port.toString())
            }
            val listener = RegistrationLogger()
            registrations += listener
            runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
                .onFailure { Log.w(TAG, "registering a beacon failed", it) }
        }
    }

    @Synchronized
    fun startBrowsing(scope: CoroutineScope, sink: PresenceSink) {
        if (browseListener != null) return
        this.sink = sink
        multicastLock.acquire()
        resolveJob = scope.launch { for (info in resolveQueue) resolve(info)?.let(::report) }
        val listener = BrowseListener()
        browseListener = listener
        runCatching { nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { Log.w(TAG, "browsing failed", it) }
    }

    @Synchronized
    fun stop() {
        browseListener?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        browseListener = null
        resolveJob?.cancel()
        resolveJob = null
        advertise(emptyList(), 0)
        beaconsByService.clear()
        sink = null
        if (multicastLock.isHeld) multicastLock.release()
    }

    private fun report(info: NsdServiceInfo) {
        val beacon = info.attributes[TXT_BEACON]?.toString(Charsets.US_ASCII)?.hexToBytesOrNull() ?: return
        if (beacon.toHex() in ownBeaconHex) return
        val port = info.attributes[TXT_PORT]?.toString(Charsets.US_ASCII)?.toIntOrNull() ?: info.port
        val addresses = info.addresses().map { it.toSocketAddressString(port) }
        if (addresses.isEmpty()) return
        beaconsByService[info.serviceName] = beacon
        sink?.onPresence(beacon, addresses.joinToString(",", "[", "]") { "\"$it\"" }, true)
    }

    private fun NsdServiceInfo.addresses(): List<InetAddress> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) hostAddresses else listOfNotNull(legacyHost())

    @Suppress("DEPRECATION")
    private fun NsdServiceInfo.legacyHost(): InetAddress? = host

    private suspend fun resolve(info: NsdServiceInfo): NsdServiceInfo? = withTimeoutOrNull(RESOLVE_TIMEOUT_MILLIS) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) resolveModern(info) else resolveLegacy(info)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun resolveModern(info: NsdServiceInfo): NsdServiceInfo? = suspendCancellableCoroutine { continuation ->
        val callback = object : NsdManager.ServiceInfoCallback {
            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                if (continuation.isActive) continuation.resume(null)
            }

            override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
                if (continuation.isActive) continuation.resume(serviceInfo)
                runCatching { nsd.unregisterServiceInfoCallback(this) }
            }

            override fun onServiceLost() = Unit
            override fun onServiceInfoCallbackUnregistered() = Unit
        }
        runCatching { nsd.registerServiceInfoCallback(info, resolverExecutor, callback) }
            .onFailure { if (continuation.isActive) continuation.resume(null) }
        continuation.invokeOnCancellation { runCatching { nsd.unregisterServiceInfoCallback(callback) } }
    }

    @Suppress("DEPRECATION")
    private suspend fun resolveLegacy(info: NsdServiceInfo): NsdServiceInfo? = suspendCancellableCoroutine { continuation ->
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                if (continuation.isActive) continuation.resume(null)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                if (continuation.isActive) continuation.resume(serviceInfo)
            }
        }
        runCatching { nsd.resolveService(info, listener) }.onFailure { if (continuation.isActive) continuation.resume(null) }
    }

    private fun randomInstanceName(): String = ByteArray(INSTANCE_NAME_BYTES).also(random::nextBytes).toHex()

    private inner class BrowseListener : NsdManager.DiscoveryListener {
        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            if (serviceInfo.serviceName in ownInstanceNames) return
            resolveQueue.trySend(serviceInfo)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            val beacon = beaconsByService.remove(serviceInfo.serviceName) ?: return
            sink?.onPresence(beacon, "[]", false)
        }

        override fun onDiscoveryStarted(serviceType: String) = Unit
        override fun onDiscoveryStopped(serviceType: String) = Unit
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.w(TAG, "browsing failed to start: $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
    }

    private class RegistrationLogger : NsdManager.RegistrationListener {
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.w(TAG, "beacon registration failed: $errorCode")
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
    }
}
