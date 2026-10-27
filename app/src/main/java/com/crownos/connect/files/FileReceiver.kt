package com.crownos.connect.files

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import com.crownos.connect.commands.PeerCommandHandler
import com.crownos.connect.llts.LltsEvent
import com.crownos.connect.protocol.PeerCommand
import com.crownos.connect.runtime.DeviceRepository
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.service.ServiceFeature
import com.crownos.connect.settings.AppPreferences
import com.crownos.fileshare.ffsp.Ffsp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

private const val TAG = "FileReceiver"
private const val FFSP_PORT = 7423
private const val OFFER_TIMEOUT_MILLIS = 30_000L
private const val RECENT_LIMIT = 20

@Immutable
data class IncomingTransfer(
    val peer: String,
    val peerName: String,
    val fileName: String?,
    val deliveredBytes: Long,
    val totalBytes: Long,
    val failure: String? = null,
) {
    val fraction: Float get() = if (totalBytes > 0) deliveredBytes.toFloat() / totalBytes else 0f
}

@Immutable
data class ReceivedFile(val peer: String, val name: String, val uri: Uri, val bytes: Long)

@Singleton
class FileReceiver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runtime: LltsRuntime,
    private val devices: DeviceRepository,
    private val preferences: AppPreferences,
    private val notifications: TransferNotifications,
) : ServiceFeature, PeerCommandHandler {
    private val currentState = MutableStateFlow<IncomingTransfer?>(null)
    val current: StateFlow<IncomingTransfer?> = currentState.asStateFlow()

    private val recentState = MutableStateFlow<List<ReceivedFile>>(emptyList())
    val recent: StateFlow<List<ReceivedFile>> = recentState.asStateFlow()

    private val stateDirectory = File(context.filesDir, "ffsp")
    private val inbox = File(stateDirectory, "inbox")
    private var scope: CoroutineScope? = null
    private var offers: Job? = null
    private var watchdog: Job? = null
    @Volatile
    private var handle = 0L
    @Volatile
    private var offered: List<OfferedFile> = emptyList()

    override fun start(scope: CoroutineScope) {
        if (offers != null) return
        this.scope = scope
        offers = scope.launch { runtime.events.filterIsInstance<LltsEvent.FileOffer>().collect(::receive) }
    }

    override fun handle(peer: String, command: PeerCommand): Boolean = command is PeerCommand.FileOffer

    fun dismissFailure() = currentState.update { it?.takeIf { transfer -> transfer.failure == null } }

    override fun stop() {
        offers?.cancel()
        offers = null
        finish()
    }

    private fun receive(offer: LltsEvent.FileOffer) {
        if (currentState.value?.failure == null && currentState.value != null) return
        val name = devices.devices.value.firstOrNull { it.id == offer.peer }?.name ?: "Your computer"
        currentState.value = IncomingTransfer(offer.peer, name, null, 0, 0)
        val host = offer.host ?: return fail("$name did not say where to fetch the file from")
        scope?.launch(Dispatchers.IO) { connect(host) }
    }

    private fun connect(host: String) {
        inbox.mkdirs()
        val started = Ffsp.nativeStart(preferences.deviceName.value, inbox.absolutePath, stateDirectory.absolutePath)
        if (started == 0L) return fail("The file service did not start")
        handle = started
        thread(name = "ffsp-events", isDaemon = true) {
            while (true) {
                val text = Ffsp.nativeNextEvent(started) ?: break
                FfspEvent.parse(text)?.let(::onEvent)
            }
        }
        val endpoint = if (host.contains(':')) "[$host]:$FFSP_PORT" else "$host:$FFSP_PORT"
        Ffsp.nativeDialLan(started, endpoint)?.let { return fail(it) }
        watchdog = scope?.launch {
            delay(OFFER_TIMEOUT_MILLIS)
            if (offered.isEmpty()) fail("The computer never sent the file")
        }
    }

    private fun onEvent(event: FfspEvent) {
        when (event) {
            is FfspEvent.LinkSettled -> if (!event.accepted) fail("The computer is not trusted for files; pair it again")
            is FfspEvent.Offered -> {
                offered = event.files
                Ffsp.nativeRespond(handle, event.device, event.transfer, true)?.let { return fail(it) }
                currentState.update { it?.copy(fileName = event.files.firstOrNull()?.name, totalBytes = event.files.sumOf(OfferedFile::length)) }
                currentState.value?.let(notifications::progress)
            }
            is FfspEvent.Progress -> {
                currentState.update { it?.copy(deliveredBytes = event.deliveredBytes, totalBytes = event.totalBytes) }
                currentState.value?.let(notifications::progress)
            }
            is FfspEvent.Finished -> if (event.completed) publish() else fail("The transfer ${event.outcome.lowercase()}")
            is FfspEvent.Failed -> fail(event.reason)
        }
    }

    private fun publish() {
        val transfer = currentState.value ?: return
        val saved = offered.mapNotNull { file ->
            val source = File(inbox, file.name).takeIf(File::isFile) ?: return@mapNotNull null
            DownloadsPublisher.publish(context, source, file.name)?.let { ReceivedFile(transfer.peer, file.name, it, file.length) }
        }
        recentState.update { (saved + it).take(RECENT_LIMIT) }
        saved.forEach { notifications.received(transfer.peerName, it) }
        currentState.value = null
        notifications.clearProgress()
        finish()
    }

    private fun fail(reason: String) {
        Log.w(TAG, reason)
        currentState.update { it?.copy(failure = reason) }
        notifications.failed(reason)
        finish()
    }

    private fun finish() {
        watchdog?.cancel()
        watchdog = null
        offered = emptyList()
        val running = handle
        handle = 0L
        if (running != 0L) Ffsp.nativeStop(running)
    }
}
