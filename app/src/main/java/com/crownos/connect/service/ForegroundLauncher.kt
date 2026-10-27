package com.crownos.connect.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val LAUNCH_NOTIFICATION_ID = 4

@Singleton
class ForegroundLauncher @Inject constructor(@ApplicationContext private val context: Context) {
    fun launch(intent: Intent, title: String, text: String) {
        val activityIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (AppVisibility.isForeground || Settings.canDrawOverlays(context)) {
            if (runCatching { context.startActivity(activityIntent) }.isSuccess) return
        }
        val pending = PendingIntent.getActivity(context, LAUNCH_NOTIFICATION_ID, activityIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, ServiceNotifications.ALERT_CHANNEL)
            .setSmallIcon(ServiceNotifications.smallIcon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setFullScreenIntent(pending, true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(LAUNCH_NOTIFICATION_ID, notification)
    }

    fun dismissPrompt() = context.getSystemService(NotificationManager::class.java).cancel(LAUNCH_NOTIFICATION_ID)
}
