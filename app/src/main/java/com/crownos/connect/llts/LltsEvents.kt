package com.crownos.connect.llts

import kotlin.concurrent.thread
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** One event from [Llts.nativeNextEvent], told apart by its `type`. */
@Serializable
sealed interface LltsEvent {
    /** The first event of every run: this device's id and the UDP port to advertise over NSD. */
    @Serializable
    @SerialName("ready")
    data class Ready(val deviceId: String, val port: Int) : LltsEvent

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : LltsEvent

    /** Fetch the bytes with [Llts.nativeAdvertisements]; `beacons` is the same set in hex. */
    @Serializable
    @SerialName("advertisingChanged")
    data class AdvertisingChanged(val beacons: List<String>) : LltsEvent

    @Serializable
    @SerialName("devicesChanged")
    data class DevicesChanged(val devices: List<LltsDevice>) : LltsEvent

    @Serializable
    @SerialName("deviceConnected")
    data class DeviceConnected(val peer: String, val name: String? = null, val connected: Boolean) : LltsEvent

    /** Show `uri` as a QR code until `expiresUnixMs`. It holds a one-time secret: never log it. */
    @Serializable
    @SerialName("pairingOffer")
    data class PairingOffer(val uri: String, val expiresUnixMs: Long) : LltsEvent

    /** Show the six-digit `code`, then answer with [Llts.nativeConfirmPairing]. */
    @Serializable
    @SerialName("pairingRequest")
    data class PairingRequest(val peer: String, val name: String, val code: String) : LltsEvent

    @Serializable
    @SerialName("pairingResult")
    data class PairingResult(val peer: String? = null, val accepted: Boolean) : LltsEvent

    @Serializable
    @SerialName("pairingWindowClosed")
    data object PairingWindowClosed : LltsEvent

    @Serializable
    @SerialName("pairableDeviceSeen")
    data class PairableDeviceSeen(
        @SerialName("class") val deviceClass: String,
        val nearbyCode: Boolean,
        val qr: Boolean,
        val address: String? = null,
    ) : LltsEvent

    /** `state` is the peer's document for `topic`, or `null` when it has none. */
    @Serializable
    @SerialName("remoteState")
    data class RemoteState(val peer: String, val topic: String, val state: JsonElement) : LltsEvent

    /** `state` is `Disabled`, `Enabled` or `Active`. */
    @Serializable
    @SerialName("featureState")
    data class FeatureState(val peer: String, val feature: String, val state: String) : LltsEvent

    /** `reason` is `Unavailable`, `NoCommonCodec`, `Refused`, `AlreadyActive` or `NotConnected`. */
    @Serializable
    @SerialName("featureFailed")
    data class FeatureFailed(val peer: String, val feature: String, val reason: String) : LltsEvent

    /** A command the peer wants carried out here, in the shape [Llts.nativeSendCommand] takes. */
    @Serializable
    @SerialName("remoteCommand")
    data class RemoteCommand(val peer: String, val command: JsonElement) : LltsEvent

    /**
     * A negotiated media stream. For a video `Encoder` call [Llts.nativeCreateEncoderSurface];
     * for a video `Decoder`, [Llts.nativeAttachDecoderSurface]. The microphone starts by itself.
     */
    @Serializable
    @SerialName("mediaStart")
    data class MediaStart(
        val peer: String,
        val feature: String,
        val role: String,
        val stream: LltsStreamRef,
        val params: LltsStreamParams,
        val request: JsonElement,
    ) : LltsEvent

    @Serializable
    @SerialName("mediaStop")
    data class MediaStop(val peer: String, val feature: String, val stream: LltsStreamRef) : LltsEvent

    /**
     * One moment of the peer's input for this device to inject. Each event is externally tagged,
     * such as `{"PointerRelative":{"dx":3.0,"dy":-1.0}}`, `{"Key":{"code":30,"pressed":true}}`
     * (Linux evdev codes) or `"Frame"`, which ends the moment. Positions are 0..1 fractions of the
     * screen, scroll is in 1/120ths of a detent.
     */
    @Serializable
    @SerialName("input")
    data class Input(val peer: String, val timeMs: Long, val events: List<JsonElement>) : LltsEvent

    /** Every key the peer holds down right now: release any other key it pressed here. */
    @Serializable
    @SerialName("inputKeys")
    data class InputKeys(val peer: String, val pressed: List<Int>) : LltsEvent
}

@Serializable
data class LltsDevice(
    val id: String,
    val name: String,
    @SerialName("class") val deviceClass: String,
    val connected: Boolean,
    val enabled: List<String>,
    val available: List<String>,
    val active: List<String>,
)

@Serializable
data class LltsStreamRef(@SerialName("class") val muxClass: String, val index: Int)

/** `kind` is `video` (codec, size, rate, bitrate) or `audio` (codec, sample rate, channels, frame). */
@Serializable
data class LltsStreamParams(
    val kind: String,
    val codec: String,
    val width: Int = 0,
    val height: Int = 0,
    val fpsMhz: Long = 0,
    val bitrateKbps: Long = 0,
    val sampleRate: Int = 0,
    val channels: Int = 0,
    val frameMs: Int = 0,
)

object LltsEvents {
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    /** `null` for an event type this build does not know yet. */
    fun decode(text: String): LltsEvent? =
        try {
            json.decodeFromString(LltsEvent.serializer(), text)
        } catch (_: SerializationException) {
            null
        }

    /** Pulls every event of `handle` on its own thread until the runtime stops. */
    fun pump(handle: Long, onEvent: (LltsEvent) -> Unit): Thread =
        thread(name = "llts-events", isDaemon = true) {
            while (true) {
                val text = Llts.nativeNextEvent(handle) ?: break
                decode(text)?.let(onEvent)
            }
        }
}
