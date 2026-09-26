package com.crownos.connect.discovery

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.crownos.connect.util.toHex
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BleDiscovery"
private val LLTS_SERVICE = ParcelUuid.fromString("0000fcf2-0000-1000-8000-00805f9b34fb")
private const val SIGHTING_EXPIRY_MILLIS = 30_000L
private const val SIGHTING_REFRESH_MILLIS = 10_000L
private const val EXPIRY_SWEEP_MILLIS = 10_000L

@Singleton
class BleDiscovery @Inject constructor(@ApplicationContext private val context: Context) {
    private val adapter get() = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val advertisingSets = mutableListOf<AdvertisingSetCallback>()
    private val sightings = ConcurrentHashMap<String, Sighting>()
    private var sink: PresenceSink? = null
    private var expiryJob: Job? = null
    private var scanning = false

    private class Sighting(val beacon: ByteArray, var lastSeen: Long, var lastReported: Long)

    val canScan: Boolean get() = hasPermission(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN else Manifest.permission.ACCESS_FINE_LOCATION)

    val canAdvertise: Boolean get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    @Synchronized
    fun advertise(beacons: List<ByteArray>) {
        val advertiser = adapter?.takeIf { it.isEnabled }?.bluetoothLeAdvertiser
        advertisingSets.forEach { callback -> runCatching { advertiser?.stopAdvertisingSet(callback) } }
        advertisingSets.clear()
        if (advertiser == null || !canAdvertise) return
        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(true)
            .setConnectable(false)
            .setScannable(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .build()
        beacons.forEach { beacon ->
            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceData(LLTS_SERVICE, beacon)
                .build()
            val callback = AdvertisingLogger()
            advertisingSets += callback
            runCatching { advertiser.startAdvertisingSet(parameters, data, null, null, null, callback) }
                .onFailure { Log.w(TAG, "advertising a beacon failed", it) }
        }
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun startScanning(scope: CoroutineScope, sink: PresenceSink) {
        if (scanning || !canScan) return
        val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner ?: return
        this.sink = sink
        val filter = ScanFilter.Builder().setServiceData(LLTS_SERVICE, ByteArray(0)).build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()
        scanning = runCatching { scanner.startScan(listOf(filter), settings, scanCallback) }
            .onFailure { Log.w(TAG, "scanning failed", it) }
            .isSuccess
        if (scanning) expiryJob = scope.launch { expireSightings() }
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun stop() {
        if (scanning) runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        scanning = false
        expiryJob?.cancel()
        expiryJob = null
        advertise(emptyList())
        sightings.clear()
        sink = null
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val beacon = result.scanRecord?.getServiceData(LLTS_SERVICE) ?: return
            val now = SystemClock.elapsedRealtime()
            val key = beacon.toHex()
            val sighting = sightings[key]
            if (sighting == null) {
                sightings[key] = Sighting(beacon, now, now)
                sink?.onPresence(beacon, "[]", true)
                return
            }
            sighting.lastSeen = now
            if (now - sighting.lastReported >= SIGHTING_REFRESH_MILLIS) {
                sighting.lastReported = now
                sink?.onPresence(beacon, "[]", true)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "scan failed: $errorCode")
        }
    }

    private suspend fun CoroutineScope.expireSightings() {
        while (isActive) {
            delay(EXPIRY_SWEEP_MILLIS)
            val cutoff = SystemClock.elapsedRealtime() - SIGHTING_EXPIRY_MILLIS
            sightings.entries.removeIf { (_, sighting) ->
                val expired = sighting.lastSeen < cutoff
                if (expired) sink?.onPresence(sighting.beacon, "[]", false)
                expired
            }
        }
    }

    private class AdvertisingLogger : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            if (status != ADVERTISE_SUCCESS) Log.w(TAG, "beacon advertising failed: $status")
        }
    }
}
