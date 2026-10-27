package com.crownos.connect.service

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.crownos.connect.discovery.DiscoveryCoordinator
import com.crownos.connect.media.MediaStreams
import com.crownos.connect.media.ScreenCapture
import com.crownos.connect.protocol.MediaRole
import com.crownos.connect.runtime.DeviceRepository
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.runtime.PairingController
import com.crownos.connect.util.Permissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConnectionService : LifecycleService() {
    @Inject lateinit var runtime: LltsRuntime
    @Inject lateinit var discovery: DiscoveryCoordinator
    @Inject lateinit var devices: DeviceRepository
    @Inject lateinit var pairing: PairingController
    @Inject lateinit var features: ServiceFeatures
    @Inject lateinit var streams: MediaStreams
    @Inject lateinit var capture: ScreenCapture

    private var status = ConnectionStatus(emptyList(), emptyList())
    private var whileInUseGranted = false

    override fun onCreate() {
        super.onCreate()
        ServiceNotifications.createChannels(this)
        promoteToForeground()
        discovery.start()
        features.start(lifecycleScope)
        runtime.start()
        observeStatus()
        observePairingRequests()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_REFRESH_TYPES -> {
                promoteToForeground()
                features.stop()
                features.start(lifecycleScope)
            }
            ACTION_TYPES_ONLY -> promoteToForeground()
            ACTION_APP_OPENED -> {
                whileInUseGranted = true
                promoteToForeground()
            }
            ACTION_PROJECTION -> {
                promoteToForeground()
                capture.onForegroundReady()
            }
            ACTION_STOP_SHARING -> stopOutgoingStreams()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        features.stop()
        discovery.stop()
        runtime.stop()
        super.onDestroy()
    }

    private fun observeStatus() = lifecycleScope.launch {
        combine(devices.devices, streams.streams) { list, active ->
            val names = list.associate { it.id to it.name }
            ConnectionStatus(
                connected = list.filter { it.connected }.map { it.name },
                sharing = active.values.filter { it.role == MediaRole.Encoder }.map { SharedStream(it.feature, names[it.peer] ?: "your computer") },
            )
        }.distinctUntilChanged().collect { current ->
            status = current
            notificationManager().notify(ServiceNotifications.CONNECTION_ID, ServiceNotifications.connection(this@ConnectionService, current))
        }
    }

    private fun observePairingRequests() = lifecycleScope.launch {
        pairing.request.collect { request ->
            val manager = notificationManager()
            if (request == null) {
                manager.cancel(ServiceNotifications.PAIRING_ID)
            } else if (!AppVisibility.isForeground) {
                manager.notify(ServiceNotifications.PAIRING_ID, ServiceNotifications.pairingCode(this@ConnectionService, request.name, request.code))
            }
        }
    }

    private fun stopOutgoingStreams() {
        streams.streams.value.values
            .filter { it.role == MediaRole.Encoder }
            .forEach { runtime.stopFeature(it.peer, it.feature) }
    }

    private fun promoteToForeground() {
        ServiceCompat.startForeground(
            this,
            ServiceNotifications.CONNECTION_ID,
            ServiceNotifications.connection(this, status),
            foregroundTypes(),
        )
    }

    private fun notificationManager() = getSystemService(NotificationManager::class.java)

    private fun foregroundTypes(): Int {
        var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        if (capture.needsForegroundType) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        whileInUseGranted = whileInUseGranted || AppVisibility.isForeground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && whileInUseGranted) {
            if (Permissions.granted(this, Manifest.permission.CAMERA)) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            if (Permissions.granted(this, Manifest.permission.RECORD_AUDIO)) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        return types
    }

    companion object {
        private const val ACTION_REFRESH_TYPES = "com.crownos.connect.REFRESH_FOREGROUND_TYPES"
        private const val ACTION_TYPES_ONLY = "com.crownos.connect.FOREGROUND_TYPES_ONLY"
        private const val ACTION_PROJECTION = "com.crownos.connect.PROMOTE_FOR_PROJECTION"
        private const val ACTION_APP_OPENED = "com.crownos.connect.APP_OPENED"
        const val ACTION_STOP_SHARING = "com.crownos.connect.STOP_SHARING"

        fun start(context: Context) = send(context, null)

        fun appOpened(context: Context) = send(context, ACTION_APP_OPENED)

        fun refreshForegroundTypes(context: Context) = send(context, ACTION_REFRESH_TYPES)

        fun refreshTypesOnly(context: Context) = send(context, ACTION_TYPES_ONLY)

        fun promoteForProjection(context: Context) = send(context, ACTION_PROJECTION)

        private fun send(context: Context, action: String?) {
            ContextCompat.startForegroundService(context, Intent(context, ConnectionService::class.java).setAction(action))
        }
    }
}
