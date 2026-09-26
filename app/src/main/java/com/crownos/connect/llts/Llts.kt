package com.crownos.connect.llts

import android.view.Surface

/**
 * The CrownConnect llts runtime in `libllts_android.so`.
 *
 * [nativeStart] returns a handle every other call takes. One dedicated thread should loop on
 * [nativeNextEvent], which blocks until the runtime has a JSON event (see [LltsEvent]) and
 * returns `null` once [nativeStop] ran. Malformed arguments throw [IllegalArgumentException];
 * a stopped handle or a runtime failure throws [IllegalStateException].
 *
 * Names such as features (`"Camera"`) and topics (`"Battery"`) are the Rust variant names, and
 * JSON payloads keep the signalling types' own shapes, the same ones the events carry.
 * Devices are the 64-hex-digit form of their static key.
 */
object Llts {
    init {
        System.loadLibrary("llts_android")
    }

    /** `configJson`: `{"name": …}` plus optional `model`, `class`, `port`, `videoCodecs`, `audioCodecs`, `features`. */
    @JvmStatic external fun nativeStart(configJson: String, filesDir: String): Long

    @JvmStatic external fun nativeStop(handle: Long)

    @JvmStatic external fun nativeNextEvent(handle: Long): String?

    @JvmStatic external fun nativeScanQr(handle: Long, qr: String)

    /** Opens a pairing window; `seconds <= 0` keeps the default of two minutes. */
    @JvmStatic external fun nativeBeginPairing(handle: Long, seconds: Int)

    @JvmStatic external fun nativeCancelPairing(handle: Long)

    /** `address` as a `pairableDeviceSeen` event reported it, `"host:port"`. */
    @JvmStatic external fun nativePairNearby(handle: Long, address: String)

    @JvmStatic external fun nativeConfirmPairing(handle: Long, peer: String, accept: Boolean)

    @JvmStatic external fun nativeForget(handle: Long, peer: String)

    @JvmStatic external fun nativeSetFeature(handle: Long, peer: String, feature: String, enabled: Boolean)

    /** `requestJson`: `{"Mirror":{…}}`, `{"Camera":{…}}`, `"Mic"` or `{"Monitor":{…}}`. */
    @JvmStatic external fun nativeStartFeature(handle: Long, peer: String, requestJson: String)

    @JvmStatic external fun nativeStopFeature(handle: Long, peer: String, feature: String)

    /** `commandJson`: e.g. `{"Media":"Pause"}` or `{"Call":{"Pickup":{"call_id":"7"}}}`. */
    @JvmStatic external fun nativeSendCommand(handle: Long, peer: String, commandJson: String)

    /** Publishes this device's document for `topic` in the JSON shape `remoteState` events carry. */
    @JvmStatic external fun nativePublishStateJson(handle: Long, topic: String, json: String)

    /** A raw BLE or NSD beacon appeared (`seen`) or went away; `addressesJson` is `["host:port", …]`. */
    @JvmStatic external fun nativePresence(handle: Long, beacon: ByteArray, addressesJson: String, seen: Boolean)

    /** This device's IP addresses as a JSON array, without link-local ones. */
    @JvmStatic external fun nativeNetworkChanged(handle: Long, addressesJson: String)

    /** Beacons to advertise over BLE and NSD, each behind a one-byte length; `null` for none. */
    @JvmStatic external fun nativeAdvertisements(handle: Long): ByteArray?

    /**
     * One moment of input for `peer`, sent on the session's input stream:
     * `{"timeMs": …, "events": [...]}` in the shape [LltsEvent.Input] carries, such as
     * `{"PointerRelative":{"dx":3.0,"dy":-1.0}}`. A closing `"Frame"` is added when missing.
     */
    @JvmStatic external fun nativeSendInput(handle: Long, peer: String, momentJson: String)

    /**
     * The encoder input surface for the video stream a `mediaStart` event with the `Encoder` role
     * announced; render into it with MediaProjection's virtual display or the camera.
     */
    @JvmStatic external fun nativeCreateEncoderSurface(handle: Long, peer: String, index: Int): Surface?

    /** Renders the video stream a `mediaStart` event with the `Decoder` role announced into `surface`. */
    @JvmStatic external fun nativeAttachDecoderSurface(handle: Long, peer: String, index: Int, surface: Surface)

    /** Stops a stream's pipeline but keeps it negotiated, so a new surface can take over. */
    @JvmStatic external fun nativeDetachSurface(handle: Long, peer: String, index: Int)
}
