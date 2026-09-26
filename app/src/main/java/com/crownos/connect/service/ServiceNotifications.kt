package com.crownos.connect.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.crownos.connect.MainActivity
import com.crownos.connect.ui.kit.CrownIcons

object ServiceNotifications {
    const val CONNECTION_ID = 1
    const val PAIRING_ID = 2
    private const val CONNECTION_CHANNEL = "connection"
    const val ALERT_CHANNEL = "alerts"

    val smallIcon: Int get() = CrownIcons.MonitorSmartphone

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CONNECTION_CHANNEL, "Connection", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Keeps CrownConnect reachable by your computers"
                    setShowBadge(false)
                },
                NotificationChannel(ALERT_CHANNEL, "Requests", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Pairing codes, screen sharing and other requests from your computers"
                },
            ),
        )
    }

    fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun connection(context: Context, connectedNames: List<String>): Notification {
        val text = when (connectedNames.size) {
            0 -> "Waiting for your computers"
            1 -> "Connected to ${connectedNames.first()}"
            else -> "Connected to ${connectedNames.size} devices"
        }
        return NotificationCompat.Builder(context, CONNECTION_CHANNEL)
            .setSmallIcon(smallIcon)
            .setContentTitle("CrownConnect")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent(context))
            .build()
    }

    fun pairingCode(context: Context, name: String, code: String): Notification =
        NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setSmallIcon(smallIcon)
            .setContentTitle("Pair with $name?")
            .setContentText("Check that $name shows ${code.chunked(3).joinToString(" ")}")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setFullScreenIntent(openAppIntent(context), true)
            .setContentIntent(openAppIntent(context))
            .build()
}
