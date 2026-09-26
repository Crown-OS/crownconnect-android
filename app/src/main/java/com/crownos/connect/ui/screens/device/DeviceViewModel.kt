package com.crownos.connect.ui.screens.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crownos.connect.protocol.BatteryDocument
import com.crownos.connect.protocol.Feature
import com.crownos.connect.protocol.FeatureRequest
import com.crownos.connect.protocol.Topic
import com.crownos.connect.protocol.decodeDocument
import com.crownos.connect.runtime.DeviceRepository
import com.crownos.connect.runtime.FeatureFailure
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.runtime.PeerDevice
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.JsonElement

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel(assistedFactory = DeviceViewModel.Factory::class)
class DeviceViewModel @AssistedInject constructor(
    @Assisted val deviceId: String,
    private val repository: DeviceRepository,
    private val runtime: LltsRuntime,
) : ViewModel() {
    val device: StateFlow<PeerDevice?> = repository.device(deviceId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        repository.devices.value.firstOrNull { it.id == deviceId },
    )

    val states: StateFlow<Map<Topic, JsonElement>> = repository.remoteStates
        .map { it[deviceId].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    val failures: Flow<FeatureFailure> = repository.featureFailures.filter { it.peer == deviceId }

    fun battery(states: Map<Topic, JsonElement>): BatteryDocument? =
        BatteryDocument.serializer().decodeDocument(states[Topic.Battery])

    fun setFeature(feature: Feature, enabled: Boolean) = runtime.setFeature(deviceId, feature, enabled)

    fun startFeature(request: FeatureRequest) = runtime.startFeature(deviceId, request)

    fun stopFeature(feature: Feature) = runtime.stopFeature(deviceId, feature)

    fun sendCommand(command: JsonElement) = runtime.sendCommand(deviceId, command)

    fun forget() = runtime.forget(deviceId)

    @AssistedFactory
    interface Factory {
        fun create(deviceId: String): DeviceViewModel
    }
}
