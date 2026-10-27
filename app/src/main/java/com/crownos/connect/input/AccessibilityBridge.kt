package com.crownos.connect.input

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

class OverlayOwner(val context: Context, val windowType: Int)

@Singleton
class AccessibilityBridge @Inject constructor() {
    @Volatile
    var service: AccessibilityService? = null
        private set

    private val connectedState = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = connectedState.asStateFlow()

    val isConnected: Boolean get() = service != null

    fun attach(connected: AccessibilityService) {
        service = connected
        connectedState.value = true
    }

    fun detach(disconnected: AccessibilityService) {
        if (service !== disconnected) return
        service = null
        connectedState.value = false
    }

    fun performGlobal(action: Int): Boolean = service?.performGlobalAction(action) == true

    /** Accessibility overlays sit above the system bars and need no overlay permission. */
    fun overlayOwner(fallback: Context): OverlayOwner = service
        ?.let { OverlayOwner(it, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY) }
        ?: OverlayOwner(fallback, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
}
