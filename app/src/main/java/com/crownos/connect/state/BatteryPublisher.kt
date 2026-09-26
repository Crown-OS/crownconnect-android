package com.crownos.connect.state

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.crownos.connect.protocol.BatteryDocument
import com.crownos.connect.protocol.Topic
import com.crownos.connect.protocol.encodeDocument
import com.crownos.connect.service.ServiceFeature
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: LocalStateStore,
) : ServiceFeature {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = publish(intent)
    }
    private var registered = false

    override fun start(scope: CoroutineScope) {
        if (registered) return
        registered = true
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )?.let(::publish)
    }

    override fun stop() {
        if (!registered) return
        registered = false
        runCatching { context.unregisterReceiver(receiver) }
    }

    private fun publish(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val document = BatteryDocument((level * 100 / scale).coerceIn(0, 100), charging)
        store.publish(Topic.Battery, BatteryDocument.serializer().encodeDocument(document))
    }
}
