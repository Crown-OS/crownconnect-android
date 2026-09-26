package com.crownos.connect.state

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.crownos.connect.di.ApplicationScope
import com.crownos.connect.protocol.NotificationAction
import com.crownos.connect.protocol.NotificationEntry
import com.crownos.connect.protocol.NotificationsDocument
import com.crownos.connect.protocol.Topic
import com.crownos.connect.protocol.encodeDocument
import com.crownos.connect.util.labelOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationBridge"
private const val PUBLISH_DEBOUNCE_MILLIS = 250L
private const val MAX_NOTIFICATIONS = 24
private const val MAX_TITLE_CHARS = 120
private const val MAX_BODY_CHARS = 600

@Singleton
class NotificationBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: LocalStateStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    @Volatile
    private var listener: NotificationListenerService? = null
    private var pending: Job? = null
    private val appLabels = ConcurrentHashMap<String, String>()

    val isConnected: Boolean get() = listener != null

    fun attach(service: NotificationListenerService) {
        listener = service
        onChanged()
    }

    fun detach(service: NotificationListenerService) {
        if (listener === service) listener = null
    }

    fun onChanged() {
        pending?.cancel()
        pending = scope.launch {
            delay(PUBLISH_DEBOUNCE_MILLIS)
            publish()
        }
    }

    fun reply(key: String, text: String): Boolean {
        val notification = findActive(key)?.notification ?: return false
        val action = notification.actions?.firstOrNull { it.freeFormInput() != null } ?: return false
        val inputs = action.remoteInputs.orEmpty()
        val results = Bundle().apply { inputs.filter { it.allowFreeFormInput }.forEach { putCharSequence(it.resultKey, text) } }
        val fillIn = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        RemoteInput.addResultsToIntent(inputs, fillIn, results)
        return send(action.actionIntent, fillIn)
    }

    private fun send(intent: PendingIntent?, fillIn: Intent): Boolean = try {
        intent?.send(context, 0, fillIn)
        intent != null
    } catch (failure: PendingIntent.CanceledException) {
        Log.w(TAG, "the reply action was cancelled", failure)
        false
    }

    private fun findActive(key: String): StatusBarNotification? =
        runCatching { listener?.activeNotifications?.firstOrNull { it.key == key } }.getOrNull()

    private fun publish() {
        val active = runCatching { listener?.activeNotifications }.getOrNull() ?: return
        val entries = active.asSequence()
            .filter { it.isShareable() }
            .sortedByDescending { it.postTime }
            .take(MAX_NOTIFICATIONS)
            .map { it.toEntry() }
            .toList()
        store.publish(Topic.Notifications, NotificationsDocument.serializer().encodeDocument(NotificationsDocument(entries)))
    }

    private fun StatusBarNotification.isShareable(): Boolean {
        val flags = notification.flags
        return packageName != context.packageName &&
            isClearable &&
            flags and Notification.FLAG_ONGOING_EVENT == 0 &&
            flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
            notification.extras.getCharSequence(Notification.EXTRA_TITLE) != null
    }

    private fun StatusBarNotification.toEntry(): NotificationEntry {
        val extras = notification.extras
        val body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT)
        return NotificationEntry(
            id = key,
            app = appLabel(packageName),
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().take(MAX_TITLE_CHARS),
            body = body?.toString().orEmpty().take(MAX_BODY_CHARS),
            actions = notification.actions.orEmpty().mapIndexed { index, action ->
                NotificationAction(index.toString(), action.title?.toString().orEmpty(), action.freeFormInput() != null)
            },
        )
    }

    private fun Notification.Action.freeFormInput(): RemoteInput? = remoteInputs?.firstOrNull { it.allowFreeFormInput }

    private fun appLabel(packageName: String): String =
        appLabels.getOrPut(packageName) { context.packageManager.labelOf(packageName) }
}
