package com.crownos.connect.state

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.crownos.connect.protocol.Topic
import com.crownos.connect.protocol.VolumeDocument
import com.crownos.connect.protocol.encodeDocument
import com.crownos.connect.service.ServiceFeature
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

private const val MEDIA_STREAM = AudioManager.STREAM_MUSIC

@Singleton
class VolumeBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: LocalStateStore,
) : ServiceFeature {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = publish()
    }
    private var observing = false

    override fun start(scope: CoroutineScope) {
        if (observing) return
        observing = true
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        publish()
    }

    override fun stop() {
        if (!observing) return
        observing = false
        context.contentResolver.unregisterContentObserver(observer)
    }

    fun apply(percent: Int, muted: Boolean) {
        val max = audio.getStreamMaxVolume(MEDIA_STREAM)
        val target = (percent.coerceIn(0, 100) / 100f * max).roundToInt()
        runCatching {
            audio.setStreamVolume(MEDIA_STREAM, target, 0)
            audio.adjustStreamVolume(MEDIA_STREAM, if (muted) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0)
        }
        publish()
    }

    private fun publish() {
        val max = audio.getStreamMaxVolume(MEDIA_STREAM).coerceAtLeast(1)
        val percent = (audio.getStreamVolume(MEDIA_STREAM) * 100f / max).roundToInt()
        val document = VolumeDocument(percent.coerceIn(0, 100), audio.isStreamMute(MEDIA_STREAM))
        store.publish(Topic.Volume, VolumeDocument.serializer().encodeDocument(document))
    }
}
