package com.crownos.connect.runtime

import android.os.SystemClock
import androidx.compose.runtime.Immutable
import com.crownos.connect.di.ApplicationScope
import com.crownos.connect.llts.LltsEvent
import com.crownos.connect.protocol.DeviceClass
import com.crownos.connect.protocol.enumByName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val PAIRABLE_EXPIRY_MILLIS = 30_000L
private const val EXPIRY_SWEEP_MILLIS = 5_000L

@Immutable
data class PairableComputer(
    val address: String,
    val deviceClass: DeviceClass,
    val nearbyCode: Boolean,
    val qr: Boolean,
    val lastSeenMillis: Long,
)

@Immutable
data class PairingCodeRequest(val peer: String, val name: String, val code: String)

@Immutable
data class PairingOutcome(val peer: String?, val accepted: Boolean)

@Singleton
class PairingController @Inject constructor(
    private val runtime: LltsRuntime,
    @ApplicationScope scope: CoroutineScope,
) {
    private val pairableState = MutableStateFlow<List<PairableComputer>>(emptyList())
    val pairable: StateFlow<List<PairableComputer>> = pairableState.asStateFlow()

    private val requestState = MutableStateFlow<PairingCodeRequest?>(null)
    val request: StateFlow<PairingCodeRequest?> = requestState.asStateFlow()

    private val pendingState = MutableStateFlow(false)
    val pending: StateFlow<Boolean> = pendingState.asStateFlow()

    private val outcomes = MutableSharedFlow<PairingOutcome>(extraBufferCapacity = 4)
    val results: SharedFlow<PairingOutcome> = outcomes.asSharedFlow()

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { runtime.events.collect(::handle) }
        scope.launch {
            while (isActive) {
                delay(EXPIRY_SWEEP_MILLIS)
                val cutoff = SystemClock.elapsedRealtime() - PAIRABLE_EXPIRY_MILLIS
                pairableState.update { list -> list.filter { it.lastSeenMillis >= cutoff } }
            }
        }
    }

    fun scanQr(uri: String) {
        pendingState.value = true
        runtime.scanQr(uri.trim())
    }

    fun pairNearby(computer: PairableComputer) {
        pendingState.value = true
        runtime.pairNearby(computer.address)
    }

    fun confirm(accept: Boolean) {
        val current = requestState.value ?: return
        requestState.value = null
        runtime.confirmPairing(current.peer, accept)
    }

    private fun handle(event: LltsEvent) {
        when (event) {
            is LltsEvent.PairableDeviceSeen -> event.address?.let { address -> rememberPairable(event, address) }
            is LltsEvent.PairingRequest -> requestState.value = PairingCodeRequest(event.peer, event.name, event.code)
            is LltsEvent.PairingResult -> {
                pendingState.value = false
                requestState.value = null
                if (event.accepted) pairableState.value = emptyList()
                outcomes.tryEmit(PairingOutcome(event.peer, event.accepted))
            }
            else -> Unit
        }
    }

    private fun rememberPairable(event: LltsEvent.PairableDeviceSeen, address: String) {
        val seen = PairableComputer(
            address = address,
            deviceClass = enumByName<DeviceClass>(event.deviceClass) ?: DeviceClass.Computer,
            nearbyCode = event.nearbyCode,
            qr = event.qr,
            lastSeenMillis = SystemClock.elapsedRealtime(),
        )
        pairableState.update { list -> list.filterNot { it.address == address } + seen }
    }
}
