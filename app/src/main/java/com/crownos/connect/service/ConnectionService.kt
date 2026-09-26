package com.crownos.connect.service

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
import com.crownos.connect.runtime.DeviceRepository
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.runtime.PairingController
import com.crownos.connect.util.Permissions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConnectionService : LifecycleService() {
    @Inject lateinit var runtime: LltsRuntime
    @Inject lateinit var discovery: DiscoveryCoordinator
    @Inject lateinit var devices: DeviceRepository
    @Inject lateinit var pairing: PairingController
    @Inject lateinit var features: ServiceFeatures

    override fun onCreate() {
        super.onCreate()
        ServiceNotifications.createChannels(this)
        promoteToForeground()
        discovery.start()
        features.start(lifecycleScope)
        runtime.start()
        observeConnections()
        observePairingRequests()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_REFRESH_TYPES) promoteToForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        features.stop()
        discovery.stop()
        runtime.stop()
        super.onDestroy()
    }

    private fun observeConnections() = lifecycleScope.launch {
        devices.devices
            .map { list -> list.filter { it.connected }.map { it.name } }
            .distinctUntilChanged()
            .collect { names ->
                notificationManager().notify(ServiceNotifications.CONNECTION_ID, ServiceNotifications.connection(this@ConnectionService, names))
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

    private fun promoteToForeground() {
        val names = devices.devices.value.filter { it.connected }.map { it.name }
        ServiceCompat.startForeground(
            this,
            ServiceNotifications.CONNECTION_ID,
            ServiceNotifications.connection(this, names),
            foregroundTypes(),
        )
    }

    private fun notificationManager() = getSystemService(NotificationManager::class.java)

    private fun foregroundTypes(): Int {
        var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Permissions.granted(this, android.Manifest.permission.CAMERA)) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            if (Permissions.granted(this, android.Manifest.permission.RECORD_AUDIO)) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        return types
    }

    companion object {
        private const val ACTION_REFRESH_TYPES = "com.crownos.connect.REFRESH_FOREGROUND_TYPES"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ConnectionService::class.java))
        }

        fun refreshForegroundTypes(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ConnectionService::class.java).setAction(ACTION_REFRESH_TYPES),
            )
        }
    }
}
