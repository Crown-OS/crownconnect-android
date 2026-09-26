package com.crownos.connect.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.crownos.connect.state.MediaSessionBridge
import com.crownos.connect.state.NotificationBridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CrownNotificationListener : NotificationListenerService() {
    @Inject lateinit var notifications: NotificationBridge
    @Inject lateinit var media: MediaSessionBridge

    override fun onListenerConnected() {
        notifications.attach(this)
        media.onListenerAccessChanged()
    }

    override fun onListenerDisconnected() {
        notifications.detach(this)
    }

    override fun onNotificationPosted(notification: StatusBarNotification) = notifications.onChanged()

    override fun onNotificationRemoved(notification: StatusBarNotification) = notifications.onChanged()
}
