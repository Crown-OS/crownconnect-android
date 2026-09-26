package com.crownos.connect.state

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import com.crownos.connect.protocol.MediaCommand
import com.crownos.connect.protocol.MediaSessionDocument
import com.crownos.connect.protocol.Topic
import com.crownos.connect.protocol.encodeDocument
import com.crownos.connect.service.CrownNotificationListener
import com.crownos.connect.service.ServiceFeature
import com.crownos.connect.util.labelOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSessionBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: LocalStateStore,
) : ServiceFeature {
    private val sessions = context.getSystemService(MediaSessionManager::class.java)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val listenerComponent = ComponentName(context, CrownNotificationListener::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var controllers: List<MediaController> = emptyList()
    private var active: MediaController? = null
    private var started = false
    private var watchingSessions = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { updated ->
        controllers = updated.orEmpty()
        chooseActive()
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = chooseActive()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = chooseActive()
    }

    override fun start(scope: CoroutineScope) {
        started = true
        watchSessions()
    }

    override fun stop() {
        started = false
        if (watchingSessions) runCatching { sessions.removeOnActiveSessionsChangedListener(sessionsListener) }
        watchingSessions = false
        active?.unregisterCallback(controllerCallback)
        active = null
    }

    fun onListenerAccessChanged() {
        if (started) mainHandler.post(::watchSessions)
    }

    fun execute(command: MediaCommand) {
        val controls = active?.transportControls
        if (controls == null) {
            dispatchKey(command)
            return
        }
        when (command) {
            MediaCommand.Play -> controls.play()
            MediaCommand.Pause -> controls.pause()
            MediaCommand.Next -> controls.skipToNext()
            MediaCommand.Previous -> controls.skipToPrevious()
            is MediaCommand.Seek -> active?.durationMillis()?.takeIf { it > 0 }?.let { controls.seekTo(it * command.percent.coerceIn(0, 100) / 100) }
        }
    }

    private fun watchSessions() {
        if (watchingSessions) return
        watchingSessions = runCatching {
            sessions.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, mainHandler)
            controllers = sessions.getActiveSessions(listenerComponent)
        }.isSuccess
        if (watchingSessions) chooseActive()
    }

    private fun chooseActive() {
        val chosen = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: controllers.firstOrNull()
        if (chosen?.sessionToken != active?.sessionToken) {
            active?.unregisterCallback(controllerCallback)
            chosen?.registerCallback(controllerCallback, mainHandler)
            active = chosen
        }
        publish()
    }

    private fun publish() {
        val controller = active
        val metadata = controller?.metadata
        if (controller == null || metadata == null) {
            store.publish(Topic.MediaSession, JsonNull)
            return
        }
        val state = controller.playbackState
        val document = MediaSessionDocument(
            app = context.packageManager.labelOf(controller.packageName),
            title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            positionMillis = state?.currentPosition() ?: 0,
            durationMillis = controller.durationMillis().coerceAtLeast(0),
            playing = state?.state == PlaybackState.STATE_PLAYING,
        )
        store.publish(Topic.MediaSession, MediaSessionDocument.serializer().encodeDocument(document))
    }

    private fun MediaController.durationMillis(): Long = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0

    private fun PlaybackState.currentPosition(): Long {
        if (state != PlaybackState.STATE_PLAYING || lastPositionUpdateTime <= 0) return position.coerceAtLeast(0)
        val elapsed = SystemClock.elapsedRealtime() - lastPositionUpdateTime
        return (position + (elapsed * playbackSpeed).toLong()).coerceAtLeast(0)
    }

    private fun dispatchKey(command: MediaCommand) {
        val code = when (command) {
            MediaCommand.Play -> KeyEvent.KEYCODE_MEDIA_PLAY
            MediaCommand.Pause -> KeyEvent.KEYCODE_MEDIA_PAUSE
            MediaCommand.Next -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaCommand.Previous -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            is MediaCommand.Seek -> return
        }
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}
