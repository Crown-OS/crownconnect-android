package com.crownos.connect.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectedMice @Inject constructor(@ApplicationContext context: Context) {
    private val inputs = context.getSystemService(InputManager::class.java)

    val present: Flow<Boolean> = callbackFlow {
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                trySend(anyMouse())
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                trySend(anyMouse())
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                trySend(anyMouse())
            }
        }
        trySend(anyMouse())
        inputs.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
        awaitClose { inputs.unregisterInputDeviceListener(listener) }
    }.distinctUntilChanged()

    private fun anyMouse() = inputs.inputDeviceIds.any { id ->
        inputs.getInputDevice(id)?.let { !it.isVirtual && it.supportsSource(InputDevice.SOURCE_MOUSE) } == true
    }
}
