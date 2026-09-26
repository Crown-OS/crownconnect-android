package com.crownos.connect.ui.screens

import androidx.lifecycle.ViewModel
import com.crownos.connect.protocol.BatteryDocument
import com.crownos.connect.protocol.Topic
import com.crownos.connect.protocol.decodeDocument
import com.crownos.connect.runtime.DeviceRepository
import com.crownos.connect.runtime.PairingController
import com.crownos.connect.settings.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val repository: DeviceRepository,
    private val pairing: PairingController,
) : ViewModel() {
    val themeMode = preferences.themeMode
    val accent = preferences.accent
    val onboarded = preferences.onboarded
    val devices = repository.devices
    val remoteStates = repository.remoteStates
    val pairingRequest = pairing.request

    fun battery(states: Map<Topic, JsonElement>?): BatteryDocument? =
        BatteryDocument.serializer().decodeDocument(states?.get(Topic.Battery))

    fun confirmPairing(accept: Boolean) = pairing.confirm(accept)

    fun finishOnboarding() = preferences.setOnboarded()
}
