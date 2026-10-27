package com.crownos.connect.files

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.crownos.connect.service.ServiceNotifications
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL = "transfers"
private const val PROGRESS_ID = 6
private const val PROGRESS_INTERVAL_MILLIS = 500L
private const val PROGRESS_SCALE = 1000

@Singleton
class TransferNotifications @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private var lastProgress = 0L

    init {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Files", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Files your computers send to this phone"
            },
        )
    }

    fun progress(transfer: IncomingTransfer) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastProgress < PROGRESS_INTERVAL_MILLIS && transfer.deliveredBytes < transfer.totalBytes) return
        lastProgress = now
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(ServiceNotifications.smallIcon)
            .setContentTitle("Receiving ${transfer.fileName ?: "a file"}")
            .setContentText("From ${transfer.peerName}")
            .setProgress(PROGRESS_SCALE, (transfer.fraction * PROGRESS_SCALE).toInt(), transfer.totalBytes <= 0)
            .setOngoing(true)
            .setSilent(true)
            .build()
        manager.notify(PROGRESS_ID, notification)
    }

    fun clearProgress() = manager.cancel(PROGRESS_ID)

    fun received(peerName: String, file: ReceivedFile) {
        val open = Intent(Intent.ACTION_VIEW)
            .setDataAndType(file.uri, DownloadsPublisher.mimeOf(file.name))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(context, file.uri.hashCode(), open, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(ServiceNotifications.smallIcon)
            .setContentTitle(file.name)
            .setContentText("Received from $peerName · saved to Downloads")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(file.uri.hashCode(), notification)
    }

    fun failed(reason: String) {
        clearProgress()
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(ServiceNotifications.smallIcon)
            .setContentTitle("A file did not arrive")
            .setContentText(reason)
            .setAutoCancel(true)
            .build()
        manager.notify(PROGRESS_ID + 1, notification)
    }
}
