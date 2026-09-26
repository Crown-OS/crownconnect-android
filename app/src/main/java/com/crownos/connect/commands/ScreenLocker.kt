package com.crownos.connect.commands

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.crownos.connect.input.AccessibilityBridge
import com.crownos.connect.service.ServiceNotifications
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenLocker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accessibility: AccessibilityBridge,
) {
    @Volatile
    var privilegedLock: (() -> Boolean)? = null

    fun lock() {
        if (privilegedLock?.invoke() == true) return
        if (accessibility.performGlobal(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)) return
        ServiceNotifications.prompt(
            context,
            "Allow remote locking",
            "Turn on CrownConnect in Accessibility so your computer can lock this phone.",
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
