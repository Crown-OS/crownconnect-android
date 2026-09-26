package com.crownos.connect.ui.screens.pairing

import androidx.lifecycle.ViewModel
import com.crownos.connect.runtime.PairableComputer
import com.crownos.connect.runtime.PairingController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PairingViewModel @Inject constructor(private val pairing: PairingController) : ViewModel() {
    val pairable = pairing.pairable
    val pending = pairing.pending
    val results = pairing.results

    fun scanned(uri: String) = pairing.scanQr(uri)

    fun pair(computer: PairableComputer) = pairing.pairNearby(computer)
}
