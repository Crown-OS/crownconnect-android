package com.crownos.connect.discovery

import com.crownos.connect.di.ApplicationScope
import com.crownos.connect.llts.LltsEvent
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.util.hexToBytesOrNull
import com.crownos.connect.util.splitLengthPrefixed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscoveryCoordinator @Inject constructor(
    private val runtime: LltsRuntime,
    private val network: NetworkMonitor,
    private val nsd: NsdDiscovery,
    private val ble: BleDiscovery,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var events: Job? = null
    private var port = 0
    private var beacons: List<ByteArray> = emptyList()
    private val presence = PresenceSink { beacon, addresses, seen -> runtime.presence(beacon, addresses, seen) }

    @Synchronized
    fun start() {
        if (events != null) return
        events = scope.launch(start = CoroutineStart.UNDISPATCHED) { runtime.events.collect(::handle) }
    }

    @Synchronized
    fun stop() {
        events?.cancel()
        events = null
        network.stop()
        nsd.stop()
        ble.stop()
    }

    fun onPermissionsChanged() {
        if (port == 0) return
        ble.startScanning(scope, presence)
        ble.advertise(beacons)
    }

    private fun handle(event: LltsEvent) {
        when (event) {
            is LltsEvent.Ready -> onReady(event.port)
            is LltsEvent.AdvertisingChanged -> advertise(event.beacons.mapNotNull { it.hexToBytesOrNull() })
            else -> Unit
        }
    }

    private fun onReady(readyPort: Int) {
        port = readyPort
        network.stop()
        nsd.stop()
        ble.stop()
        network.start(runtime::networkChanged)
        nsd.startBrowsing(scope, presence)
        ble.startScanning(scope, presence)
        advertise(splitLengthPrefixed(runtime.advertisements()))
    }

    private fun advertise(current: List<ByteArray>) {
        beacons = current
        nsd.advertise(current, port)
        ble.advertise(current)
    }
}
