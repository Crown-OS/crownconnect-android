package com.crownos.connect.runtime

import com.crownos.connect.di.ApplicationScope
import com.crownos.connect.llts.LltsEvent
import com.crownos.connect.protocol.Feature
import com.crownos.connect.protocol.Topic
import com.crownos.connect.protocol.enumByName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    runtime: LltsRuntime,
    @ApplicationScope scope: CoroutineScope,
) {
    private val devicesState = MutableStateFlow<List<PeerDevice>>(emptyList())
    val devices: StateFlow<List<PeerDevice>> = devicesState.asStateFlow()

    private val remoteStatesState = MutableStateFlow<Map<String, Map<Topic, JsonElement>>>(emptyMap())
    val remoteStates: StateFlow<Map<String, Map<Topic, JsonElement>>> = remoteStatesState.asStateFlow()

    private val failures = MutableSharedFlow<FeatureFailure>(extraBufferCapacity = 16)
    val featureFailures: SharedFlow<FeatureFailure> = failures.asSharedFlow()

    private val errorMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errors: SharedFlow<String> = errorMessages.asSharedFlow()

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { runtime.events.collect(::handle) }
    }

    fun device(id: String): Flow<PeerDevice?> = devices.map { list -> list.firstOrNull { it.id == id } }.distinctUntilChanged()

    fun remoteState(peer: String, topic: Topic): Flow<JsonElement?> =
        remoteStates.map { it[peer]?.get(topic) }.distinctUntilChanged()

    val connectedDevices: List<PeerDevice> get() = devices.value.filter { it.connected }

    private fun handle(event: LltsEvent) {
        when (event) {
            is LltsEvent.DevicesChanged -> devicesState.value = event.devices.map(PeerDevice::from)
            is LltsEvent.DeviceConnected -> devicesState.update { list ->
                list.map { if (it.id == event.peer) it.copy(connected = event.connected) else it }
            }
            is LltsEvent.RemoteState -> storeRemoteState(event)
            is LltsEvent.FeatureFailed -> enumByName<Feature>(event.feature)?.let {
                failures.tryEmit(FeatureFailure(event.peer, it, event.reason))
            }
            is LltsEvent.Error -> errorMessages.tryEmit(event.message)
            else -> Unit
        }
    }

    private fun storeRemoteState(event: LltsEvent.RemoteState) {
        val topic = enumByName<Topic>(event.topic) ?: return
        remoteStatesState.update { all ->
            val peerStates = all[event.peer].orEmpty().toMutableMap()
            if (event.state is JsonNull) peerStates.remove(topic) else peerStates[topic] = event.state
            all + (event.peer to peerStates)
        }
    }
}
