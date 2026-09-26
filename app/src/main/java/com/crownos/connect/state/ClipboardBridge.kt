package com.crownos.connect.state

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.crownos.connect.llts.LltsEvent
import com.crownos.connect.protocol.ClipboardDocument
import com.crownos.connect.protocol.Topic
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.service.AppVisibility
import com.crownos.connect.service.ServiceFeature
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val PLAIN_TEXT = "text/plain"

fun interface ClipboardReader {
    fun readText(): String?
}

@Singleton
class ClipboardBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: LocalStateStore,
    private val runtime: LltsRuntime,
) : ServiceFeature {
    private val clipboard = context.getSystemService(ClipboardManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val jobs = mutableListOf<Job>()
    @Volatile
    private var lastSeenText: String? = null
    @Volatile
    var privilegedReader: ClipboardReader? = null

    private val changeListener = ClipboardManager.OnPrimaryClipChangedListener { captureLocal() }

    override fun start(scope: CoroutineScope) {
        mainHandler.post { clipboard.addPrimaryClipChangedListener(changeListener) }
        jobs += scope.launch { AppVisibility.foreground.filter { it }.collect { mainHandler.post(::captureLocal) } }
        jobs += scope.launch {
            runtime.events.filterIsInstance<LltsEvent.RemoteState>()
                .filter { it.topic == Topic.Clipboard.name }
                .collect { event -> ClipboardDocument.parse(event.state)?.text?.let(::applyRemote) }
        }
    }

    override fun stop() {
        mainHandler.post { clipboard.removePrimaryClipChangedListener(changeListener) }
        jobs.forEach(Job::cancel)
        jobs.clear()
    }

    fun captureLocal() {
        val text = privilegedReader?.readText() ?: readForegroundText() ?: return
        if (text == lastSeenText || text.isEmpty()) return
        lastSeenText = text
        if (text.toByteArray(Charsets.UTF_8).size > ClipboardDocument.MAX_INLINE_BYTES) return
        store.publish(Topic.Clipboard, ClipboardDocument(PLAIN_TEXT, text).toJson())
    }

    private fun readForegroundText(): String? = runCatching {
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0 || clip.description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == false && clip.getItemAt(0).text == null) {
            return null
        }
        clip.getItemAt(0).coerceToText(context)?.toString()
    }.getOrNull()

    private fun applyRemote(text: String) {
        if (text == lastSeenText) return
        lastSeenText = text
        mainHandler.post {
            runCatching { clipboard.setPrimaryClip(ClipData.newPlainText("CrownConnect", text)) }
        }
    }
}
