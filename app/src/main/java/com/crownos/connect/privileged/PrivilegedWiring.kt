package com.crownos.connect.privileged

import android.view.KeyEvent
import com.crownos.connect.commands.ScreenLocker
import com.crownos.connect.input.ShizukuInputInjector
import com.crownos.connect.service.ServiceFeature
import com.crownos.connect.state.ClipboardBridge
import com.crownos.connect.state.HotspotBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivilegedWiring @Inject constructor(
    private val shizuku: ShizukuAccess,
    private val clipboard: ClipboardBridge,
    private val shizukuClipboard: ShizukuClipboard,
    private val hotspot: HotspotBridge,
    private val shizukuHotspot: ShizukuHotspot,
    private val locker: ScreenLocker,
    private val input: ShizukuInputInjector,
) : ServiceFeature {
    private var job: Job? = null

    override fun start(scope: CoroutineScope) {
        if (job != null) return
        shizuku.refresh()
        job = scope.launch { shizuku.status.collect { status -> if (status is ShizukuStatus.Ready) install() else uninstall() } }
    }

    override fun stop() {
        job?.cancel()
        job = null
        uninstall()
    }

    private fun install() {
        clipboard.privilegedReader = shizukuClipboard
        shizukuClipboard.watch(clipboard::captureLocal)
        hotspot.privilegedController = shizukuHotspot
        hotspot.publish()
        locker.privilegedLock = { input.sendKeyPress(KeyEvent.KEYCODE_SLEEP) }
    }

    private fun uninstall() {
        shizukuClipboard.unwatch()
        shizukuClipboard.reset()
        clipboard.privilegedReader = null
        hotspot.privilegedController = null
        locker.privilegedLock = null
    }
}
