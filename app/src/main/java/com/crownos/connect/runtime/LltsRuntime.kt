package com.crownos.connect.runtime

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Surface
import com.crownos.connect.llts.Llts
import com.crownos.connect.llts.LltsEvent
import com.crownos.connect.llts.LltsEvents
import com.crownos.connect.media.CodecProbe
import com.crownos.connect.protocol.DeviceClass
import com.crownos.connect.protocol.Feature
import com.crownos.connect.protocol.FeatureRequest
import com.crownos.connect.protocol.StartConfig
import com.crownos.connect.protocol.Topic
import com.crownos.connect.settings.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LltsRuntime"
private const val EVENT_BUFFER = 1024

sealed interface RuntimeStatus {
    data object Stopped : RuntimeStatus
    data object Starting : RuntimeStatus
    data class Running(val deviceId: String, val port: Int) : RuntimeStatus
    data class Failed(val message: String) : RuntimeStatus
}

@Singleton
class LltsRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
) {
    @Volatile
    private var handle = 0L
    private var pump: Thread? = null

    private val eventFlow = MutableSharedFlow<LltsEvent>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<LltsEvent> = eventFlow.asSharedFlow()

    private val statusFlow = MutableStateFlow<RuntimeStatus>(RuntimeStatus.Stopped)
    val status: StateFlow<RuntimeStatus> = statusFlow.asStateFlow()

    val isRunning: Boolean get() = handle != 0L

    @Synchronized
    fun start() {
        if (handle != 0L) return
        statusFlow.value = RuntimeStatus.Starting
        val started = try {
            Llts.nativeStart(startConfig().toJson(), context.filesDir.absolutePath)
        } catch (failure: RuntimeException) {
            return fail(failure)
        } catch (failure: LinkageError) {
            return fail(failure)
        }
        handle = started
        pump = LltsEvents.pump(started, ::dispatch)
    }

    @Synchronized
    fun stop() {
        val running = handle
        if (running == 0L) return
        handle = 0L
        runCatching { Llts.nativeStop(running) }
        pump?.join(PUMP_JOIN_MILLIS)
        pump = null
        statusFlow.value = RuntimeStatus.Stopped
    }

    private fun fail(failure: Throwable) {
        Log.e(TAG, "the llts runtime did not start", failure)
        statusFlow.value = RuntimeStatus.Failed(failure.message ?: "The runtime did not start")
    }

    fun restart() {
        stop()
        start()
    }

    private fun dispatch(event: LltsEvent) {
        when (event) {
            is LltsEvent.Ready -> statusFlow.value = RuntimeStatus.Running(event.deviceId, event.port)
            is LltsEvent.Error -> Log.w(TAG, event.message)
            else -> Unit
        }
        if (!eventFlow.tryEmit(event)) Log.w(TAG, "dropped ${event::class.simpleName}: event buffer full")
    }

    private fun startConfig() = StartConfig(
        name = preferences.deviceName.value,
        model = Build.MODEL,
        `class` = if (context.resources.configuration.smallestScreenWidthDp >= TABLET_MIN_WIDTH_DP) DeviceClass.Tablet else DeviceClass.Phone,
        videoCodecs = CodecProbe.hardwareVideoCodecs(),
        features = Feature.entries,
    )

    private inline fun <T> call(operation: String, fallback: T, block: (Long) -> T): T {
        val running = handle
        if (running == 0L) return fallback
        return try {
            block(running)
        } catch (failure: IllegalArgumentException) {
            report(operation, failure)
            fallback
        } catch (failure: IllegalStateException) {
            report(operation, failure)
            fallback
        }
    }

    private fun report(operation: String, failure: RuntimeException) {
        Log.w(TAG, "$operation failed", failure)
        eventFlow.tryEmit(LltsEvent.Error("$operation: ${failure.message}"))
    }

    fun scanQr(uri: String) = call("scanQr", Unit) { Llts.nativeScanQr(it, uri) }

    fun beginPairing(seconds: Int = 0) = call("beginPairing", Unit) { Llts.nativeBeginPairing(it, seconds) }

    fun cancelPairing() = call("cancelPairing", Unit) { Llts.nativeCancelPairing(it) }

    fun pairNearby(address: String) = call("pairNearby", Unit) { Llts.nativePairNearby(it, address) }

    fun confirmPairing(peer: String, accept: Boolean) =
        call("confirmPairing", Unit) { Llts.nativeConfirmPairing(it, peer, accept) }

    fun forget(peer: String) = call("forget", Unit) { Llts.nativeForget(it, peer) }

    fun setFeature(peer: String, feature: Feature, enabled: Boolean) =
        call("setFeature", Unit) { Llts.nativeSetFeature(it, peer, feature.name, enabled) }

    fun startFeature(peer: String, request: FeatureRequest) =
        call("startFeature", Unit) { Llts.nativeStartFeature(it, peer, request.toJson().toString()) }

    fun stopFeature(peer: String, feature: Feature) =
        call("stopFeature", Unit) { Llts.nativeStopFeature(it, peer, feature.name) }

    fun sendCommand(peer: String, command: JsonElement) =
        call("sendCommand", Unit) { Llts.nativeSendCommand(it, peer, command.toString()) }

    fun publishState(topic: Topic, document: JsonElement) =
        call("publishState", Unit) { Llts.nativePublishStateJson(it, topic.name, document.toString()) }

    fun presence(beacon: ByteArray, addressesJson: String, seen: Boolean) =
        call("presence", Unit) { Llts.nativePresence(it, beacon, addressesJson, seen) }

    fun networkChanged(addressesJson: String) =
        call("networkChanged", Unit) { Llts.nativeNetworkChanged(it, addressesJson) }

    fun advertisements(): ByteArray? = call("advertisements", null) { Llts.nativeAdvertisements(it) }

    fun sendInput(peer: String, momentJson: String) =
        call("sendInput", Unit) { Llts.nativeSendInput(it, peer, momentJson) }

    fun createEncoderSurface(peer: String, index: Int): Surface? =
        call("createEncoderSurface", null) { Llts.nativeCreateEncoderSurface(it, peer, index) }

    fun attachDecoderSurface(peer: String, index: Int, surface: Surface) =
        call("attachDecoderSurface", Unit) { Llts.nativeAttachDecoderSurface(it, peer, index, surface) }

    fun detachSurface(peer: String, index: Int) = call("detachSurface", Unit) { Llts.nativeDetachSurface(it, peer, index) }

    private companion object {
        const val PUMP_JOIN_MILLIS = 2_000L
        const val TABLET_MIN_WIDTH_DP = 600
    }
}
