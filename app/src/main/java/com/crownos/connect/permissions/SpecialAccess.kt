package com.crownos.connect.permissions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.crownos.connect.service.CrownAccessibilityService
import com.crownos.connect.service.CrownNotificationListener
import com.crownos.connect.ui.kit.CrownIcons

enum class SpecialAccess(val title: String, val description: String, val icon: Int) {
    NotificationAccess("Notification access", "Mirror notifications, reply to them, and show what's playing", CrownIcons.Bell),
    Accessibility("Accessibility", "Lock the phone and control it from your computer without Shizuku", CrownIcons.Hand),
    DisplayOverApps("Display over other apps", "Draw the shared cursor and open shared screens", CrownIcons.Layers),
    UnrestrictedBattery("Unrestricted battery", "Stay connected while the phone sleeps", CrownIcons.BatteryFull);

    fun isGranted(context: Context): Boolean = when (this) {
        NotificationAccess -> context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
        Accessibility -> accessibilityEnabled(context)
        DisplayOverApps -> Settings.canDrawOverlays(context)
        UnrestrictedBattery -> context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }

    fun settingsIntent(context: Context): Intent {
        val packageUri = Uri.fromParts("package", context.packageName, null)
        val intent = when (this) {
            NotificationAccess -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, CrownNotificationListener::class.java).flattenToString(),
                )
            } else {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            }
            Accessibility -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            DisplayOverApps -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
            UnrestrictedBattery -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    companion object {
        fun accessibilityComponent(context: Context) = ComponentName(context, CrownAccessibilityService::class.java)

        private fun accessibilityEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            val component = accessibilityComponent(context)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == component }
        }
    }
}
