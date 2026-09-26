package com.crownos.connect.state

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import com.crownos.connect.protocol.CallInfo
import com.crownos.connect.protocol.CallRequest
import com.crownos.connect.protocol.CallStateDocument
import com.crownos.connect.protocol.CallStatus
import com.crownos.connect.protocol.Topic
import com.crownos.connect.protocol.encodeDocument
import com.crownos.connect.service.ServiceFeature
import com.crownos.connect.util.Permissions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CallBridge"
private const val ENDED_LINGER_MILLIS = 3_000L

@Singleton
class CallBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: LocalStateStore,
) : ServiceFeature {
    private val telephony = context.getSystemService(TelephonyManager::class.java)
    private val telecom = context.getSystemService(TelecomManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var listening = false
    private var nextCallId = 1
    private var current: CallInfo? = null

    @Suppress("DEPRECATION")
    private val listener by lazy {
        object : PhoneStateListener() {
            @Deprecated("PhoneStateListener is the only callback that still reports the caller's number")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) = onState(state, phoneNumber)
        }
    }

    override fun start(scope: CoroutineScope) {
        if (listening || !Permissions.granted(context, Manifest.permission.READ_PHONE_STATE) || telephony == null) return
        mainHandler.post {
            @Suppress("DEPRECATION")
            runCatching { telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE) }
                .onSuccess { listening = true }
                .onFailure { Log.w(TAG, "call state is unavailable", it) }
        }
    }

    override fun stop() {
        if (!listening) return
        listening = false
        @Suppress("DEPRECATION")
        mainHandler.post { telephony?.listen(listener, PhoneStateListener.LISTEN_NONE) }
    }

    @SuppressLint("MissingPermission")
    fun execute(request: CallRequest) {
        val call = current
        runCatching {
            when (request) {
                is CallRequest.Pickup -> if (call?.callId == request.callId && canAnswer()) {
                    @Suppress("DEPRECATION")
                    telecom.acceptRingingCall()
                }
                is CallRequest.Decline, is CallRequest.Hangup -> if (canAnswer()) {
                    @Suppress("DEPRECATION")
                    telecom.endCall()
                }
                is CallRequest.Dial -> if (Permissions.granted(context, Manifest.permission.CALL_PHONE)) {
                    telecom.placeCall(Uri.fromParts("tel", request.number, null), null)
                }
            }
        }.onFailure { Log.w(TAG, "the call command failed", it) }
    }

    private fun canAnswer() = Permissions.granted(context, Manifest.permission.ANSWER_PHONE_CALLS)

    private fun onState(state: Int, phoneNumber: String?) {
        val previous = current
        val now = System.currentTimeMillis()
        current = when (state) {
            TelephonyManager.CALL_STATE_RINGING -> CallInfo(newCallId(), phoneNumber.orEmpty(), contactName(phoneNumber), CallStatus.Ringing)
            TelephonyManager.CALL_STATE_OFFHOOK -> previous?.takeIf { it.status != CallStatus.Ended }
                ?.copy(status = CallStatus.Active, answeredUnixMillis = previous.answeredUnixMillis ?: now)
                ?: CallInfo(newCallId(), phoneNumber.orEmpty(), contactName(phoneNumber), CallStatus.Active, now)
            else -> previous?.copy(status = CallStatus.Ended)
        }
        publish()
        if (current?.status == CallStatus.Ended) {
            val ended = current
            mainHandler.postDelayed({
                if (current === ended) {
                    current = null
                    publish()
                }
            }, ENDED_LINGER_MILLIS)
        }
    }

    private fun newCallId(): String = (nextCallId++).toString()

    private fun publish() {
        val document = CallStateDocument(listOfNotNull(current))
        store.publish(Topic.CallState, CallStateDocument.serializer().encodeDocument(document))
    }

    private fun contactName(number: String?): String? {
        if (number.isNullOrBlank() || !Permissions.granted(context, Manifest.permission.READ_CONTACTS)) return null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return runCatching {
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }
}
