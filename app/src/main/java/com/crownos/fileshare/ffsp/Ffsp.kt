package com.crownos.fileshare.ffsp

/**
 * The ffsp engine in `libffsp_android.so`, built from `fileshare/rust`. The package is fixed by
 * the library's JNI symbol names.
 */
object Ffsp {
    init {
        System.loadLibrary("ffsp_android")
    }

    /** An opaque handle, or 0 when the engine could not start. */
    @JvmStatic external fun nativeStart(name: String, destination: String, state: String): Long

    @JvmStatic external fun nativeStop(handle: Long)

    /** Blocks until the engine has an event as JSON; `null` once it has stopped. */
    @JvmStatic external fun nativeNextEvent(handle: Long): String?

    /** Opens a session's first lane to a computer's Wi-Fi listener; `null` on success. */
    @JvmStatic external fun nativeDialLan(handle: Long, endpoint: String): String?

    @JvmStatic external fun nativeRespond(handle: Long, device: String, transfer: Int, accept: Boolean): String?

    @JvmStatic external fun nativeCancel(handle: Long, device: String, transfer: Int): String?
}
