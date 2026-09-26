package com.crownos.connect.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.crownos.connect.input.AccessibilityBridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CrownAccessibilityService : AccessibilityService() {
    @Inject lateinit var bridge: AccessibilityBridge

    override fun onServiceConnected() {
        bridge.attach(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bridge.detach(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        bridge.detach(this)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
