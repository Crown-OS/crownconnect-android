package com.crownos.connect.input

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import kotlinx.coroutines.android.asCoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputThread @Inject constructor() {
    private val thread = HandlerThread("crown-input", Process.THREAD_PRIORITY_URGENT_DISPLAY).apply { start() }
    private val handler = Handler(thread.looper)
    val dispatcher = handler.asCoroutineDispatcher("crown-input")

    fun post(action: () -> Unit) {
        handler.post(action)
    }
}
