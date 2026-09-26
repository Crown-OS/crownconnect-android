package com.crownos.connect.input

import android.accessibilityservice.AccessibilityService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityBridge @Inject constructor() {
    @Volatile
    var service: AccessibilityService? = null
        private set

    val isConnected: Boolean get() = service != null

    fun attach(connected: AccessibilityService) {
        service = connected
    }

    fun detach(disconnected: AccessibilityService) {
        if (service === disconnected) service = null
    }

    fun performGlobal(action: Int): Boolean = service?.performGlobalAction(action) == true
}
