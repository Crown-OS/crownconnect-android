package com.crownos.connect.commands

import android.util.Log
import com.crownos.connect.llts.LltsEvent
import com.crownos.connect.protocol.PeerCommand
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.service.ServiceFeature
import com.crownos.connect.state.CallBridge
import com.crownos.connect.state.HotspotBridge
import com.crownos.connect.state.MediaSessionBridge
import com.crownos.connect.state.NotificationBridge
import com.crownos.connect.state.VolumeBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RemoteCommands"

fun interface PeerCommandHandler {
    fun handle(peer: String, command: PeerCommand): Boolean
}

@Singleton
class RemoteCommandDispatcher @Inject constructor(
    private val runtime: LltsRuntime,
    private val media: MediaSessionBridge,
    private val volume: VolumeBridge,
    private val calls: CallBridge,
    private val notifications: NotificationBridge,
    private val hotspot: HotspotBridge,
    private val locker: ScreenLocker,
    private val extraHandlers: Set<@JvmSuppressWildcards PeerCommandHandler>,
) : ServiceFeature {
    private var job: Job? = null

    override fun start(scope: CoroutineScope) {
        if (job != null) return
        job = scope.launch {
            runtime.events.filterIsInstance<LltsEvent.RemoteCommand>().collect { event ->
                PeerCommand.parse(event.command)?.let { dispatch(event.peer, it) }
                    ?: Log.w(TAG, "ignored an unknown command: ${event.command}")
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    private fun dispatch(peer: String, command: PeerCommand) {
        when (command) {
            is PeerCommand.Media -> media.execute(command.command)
            is PeerCommand.SetVolume -> volume.apply(command.percent, command.muted)
            is PeerCommand.Call -> calls.execute(command.request)
            is PeerCommand.Reply -> if (!notifications.reply(command.conversationId, command.text)) {
                Log.w(TAG, "no reply action for ${command.conversationId}")
            }
            is PeerCommand.SetHotspot -> hotspot.setEnabled(command.enabled)
            PeerCommand.Lock -> locker.lock()
            else -> if (extraHandlers.none { it.handle(peer, command) }) Log.w(TAG, "nothing handles $command")
        }
    }
}
