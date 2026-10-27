package com.crownos.connect.util

import android.view.Choreographer
import java.util.concurrent.atomic.AtomicBoolean

class VsyncCoalescer(private val choreographer: Choreographer, private val onFrame: () -> Unit) {
    private val pending = AtomicBoolean(false)
    private val callback = Choreographer.FrameCallback {
        pending.set(false)
        onFrame()
    }

    fun request() {
        if (pending.compareAndSet(false, true)) choreographer.postFrameCallback(callback)
    }

    fun cancel() {
        choreographer.removeFrameCallback(callback)
        pending.set(false)
    }
}
