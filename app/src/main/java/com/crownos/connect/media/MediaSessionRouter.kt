package com.crownos.connect.media

import android.content.Context
import com.crownos.connect.llts.LltsEvent
import com.crownos.connect.protocol.Feature
import com.crownos.connect.protocol.MediaRole
import com.crownos.connect.runtime.DeviceRepository
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.service.ForegroundLauncher
import com.crownos.connect.service.ServiceFeature
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSessionRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runtime: LltsRuntime,
    private val streams: MediaStreams,
    private val devices: DeviceRepository,
    private val capture: ScreenCapture,
    private val camera: CameraStreamer,
    private val launcher: ForegroundLauncher,
) : ServiceFeature {
    private var job: Job? = null

    override fun start(scope: CoroutineScope) {
        if (job != null) return
        job = scope.launch {
            runtime.events.collect { event ->
                when (event) {
                    is LltsEvent.MediaStart -> MediaStream.from(event)?.let(::onStart)
                    is LltsEvent.MediaStop -> streams.stopped(event.peer, event.stream.index)?.let(::onStop)
                    is LltsEvent.DeviceConnected -> if (!event.connected) streams.clearPeer(event.peer).forEach(::onStop)
                    else -> Unit
                }
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        capture.stop()
        camera.stop()
    }

    private fun onStart(stream: MediaStream) {
        streams.started(stream)
        if (!stream.isVideo) return
        when (stream.role) {
            MediaRole.Encoder -> when (stream.feature) {
                Feature.Mirror -> capture.start(stream)
                Feature.Camera -> camera.start(stream)
                else -> Unit
            }
            MediaRole.Decoder -> showRemoteDisplay(stream)
        }
    }

    private fun onStop(stream: MediaStream) {
        if (stream.role != MediaRole.Encoder) return
        when (stream.feature) {
            Feature.Mirror -> capture.stop(stream.peer)
            Feature.Camera -> camera.stop(stream.peer)
            else -> Unit
        }
    }

    private fun showRemoteDisplay(stream: MediaStream) {
        val name = devices.devices.value.firstOrNull { it.id == stream.peer }?.name ?: "Your computer"
        val what = if (stream.feature == Feature.Monitor) "is using this phone as a display" else "is sharing its screen"
        launcher.launch(RemoteDisplayActivity.intent(context, stream), "$name $what", "Tap to view it.")
    }
}
