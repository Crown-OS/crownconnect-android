package com.crownos.connect.ui.screens.device

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crownos.connect.files.FileReceiver
import com.crownos.connect.files.IncomingTransfer
import com.crownos.connect.files.ReceivedFile
import com.crownos.connect.media.CodecProbe
import com.crownos.connect.media.MediaStream
import com.crownos.connect.media.MediaStreams
import com.crownos.connect.media.RemoteDisplayActivity
import com.crownos.connect.media.StreamLimits
import com.crownos.connect.protocol.BatteryDocument
import com.crownos.connect.protocol.Direction
import com.crownos.connect.protocol.Edge
import com.crownos.connect.protocol.Feature
import com.crownos.connect.protocol.FeatureRequest
import com.crownos.connect.protocol.HotspotDocument
import com.crownos.connect.protocol.MediaSessionDocument
import com.crownos.connect.protocol.PeerCommand
import com.crownos.connect.protocol.VolumeDocument
import com.crownos.connect.protocol.Topic
import com.crownos.connect.protocol.decodeDocument
import com.crownos.connect.runtime.DeviceRepository
import com.crownos.connect.runtime.FeatureFailure
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.runtime.PeerDevice
import com.crownos.connect.settings.AppPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import com.crownos.connect.util.ScreenGeometry
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val repository: DeviceRepository,
    private val runtime: LltsRuntime,
    private val screen: ScreenGeometry,
    private val files: FileReceiver,
    private val preferences: AppPreferences,
    mediaStreams: MediaStreams,
) : ViewModel() {
    val computerEdge: StateFlow<Edge?> = preferences.computerEdges
        .map { it[deviceId] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), preferences.computerEdges.value[deviceId])

    fun setComputerEdge(edge: Edge) = preferences.setComputerEdge(deviceId, edge)

    val transfer: StateFlow<IncomingTransfer?> = files.current
        .map { it?.takeIf { transfer -> transfer.peer == deviceId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val receivedFiles: StateFlow<List<ReceivedFile>> = files.recent
        .map { all -> all.filter { it.peer == deviceId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun dismissTransferFailure() = files.dismissFailure()

    val streams: StateFlow<List<MediaStream>> = mediaStreams.streams
        .map { all -> all.values.filter { it.peer == deviceId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), mediaStreams.forPeer(deviceId))

    fun shareScreen(allowControl: Boolean) = runtime.startFeature(
        deviceId,
        FeatureRequest.Mirror(Direction.ToPeer, StreamLimits.forScreen(screen.size, CodecProbe::encodesSize), allowControl),
    )

    fun viewScreen() = runtime.startFeature(deviceId, FeatureRequest.Mirror(Direction.FromPeer, StreamLimits.forViewing, remoteInput = true))

    fun listen() = runtime.startFeature(deviceId, FeatureRequest.Mic)

    fun open(stream: MediaStream) = context.startActivity(RemoteDisplayActivity.intent(context, stream).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

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

    fun media(states: Map<Topic, JsonElement>): MediaSessionDocument? =
        MediaSessionDocument.serializer().decodeDocument(states[Topic.MediaSession])

    fun volume(states: Map<Topic, JsonElement>): VolumeDocument? =
        VolumeDocument.serializer().decodeDocument(states[Topic.Volume])

    fun hotspot(states: Map<Topic, JsonElement>): HotspotDocument? =
        HotspotDocument.serializer().decodeDocument(states[Topic.Hotspot])

    fun send(command: PeerCommand) = runtime.sendCommand(deviceId, command.toJson())

    fun setFeature(feature: Feature, enabled: Boolean) = runtime.setFeature(deviceId, feature, enabled)

    fun startFeature(request: FeatureRequest) = runtime.startFeature(deviceId, request)

    fun stopFeature(feature: Feature) = runtime.stopFeature(deviceId, feature)

    fun forget() {
        runtime.forget(deviceId)
        preferences.forgetComputer(deviceId)
    }

    @AssistedFactory
    interface Factory {
        fun create(deviceId: String): DeviceViewModel
    }
}
