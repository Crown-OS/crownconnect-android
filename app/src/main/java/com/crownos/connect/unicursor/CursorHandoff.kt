package com.crownos.connect.unicursor

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.crownos.connect.input.AccessibilityBridge
import com.crownos.connect.input.ConnectedMice
import com.crownos.connect.protocol.Edge
import com.crownos.connect.protocol.Feature
import com.crownos.connect.protocol.PeerCommand
import com.crownos.connect.runtime.DeviceRepository
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.runtime.PeerDevice
import com.crownos.connect.service.ServiceFeature
import com.crownos.connect.settings.AppPreferences
import com.crownos.connect.util.EdgeGeometry
import com.crownos.connect.util.ScreenGeometry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Sends this phone's mouse to a computer when it reaches the screen edge that faces it. */
@Singleton
class CursorHandoff @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accessibility: AccessibilityBridge,
    private val devices: DeviceRepository,
    private val preferences: AppPreferences,
    private val mice: ConnectedMice,
    private val runtime: LltsRuntime,
    private val screen: ScreenGeometry,
) : ServiceFeature, HandoffStrip.Listener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val strips = HashMap<Edge, HandoffStrip>()
    private var job: Job? = null
    private var stripWindowType = 0

    override fun start(scope: CoroutineScope) {
        if (job != null) return
        job = scope.launch(Dispatchers.Main) {
            combine(devices.devices, preferences.computerEdges, mice.present, accessibility.connected) { list, edges, mouse, _ ->
                if (mouse) facingEdges(list, edges) else emptyMap()
            }.collect(::arrange)
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        mainHandler.post { arrange(emptyMap()) }
    }

    fun handedBack(peer: String) = mainHandler.post {
        strips.values.firstOrNull { it.peer == peer }?.handBack()
    }

    override fun onEntered(strip: HandoffStrip, edge: Edge, fraction: Float) {
        runtime.sendCommand(strip.peer, PeerCommand.UnicursorEnter(edge, EdgeGeometry.positionOf(fraction)).toJson())
    }

    override fun onInput(strip: HandoffStrip, moment: String) = runtime.sendInput(strip.peer, moment)

    private fun facingEdges(list: List<PeerDevice>, edges: Map<String, Edge>): Map<Edge, String> =
        list.filter { it.connected && Feature.Unicursor in it.available }
            .mapNotNull { device -> edges[device.id]?.let { it to device.id } }
            .toMap()

    private fun arrange(wanted: Map<Edge, String>) {
        val owner = accessibility.overlayOwner(context)
        val ownerChanged = owner.windowType != stripWindowType
        stripWindowType = owner.windowType
        val stale = strips.filter { (edge, strip) -> ownerChanged || wanted[edge] != strip.peer }
        stale.forEach { (edge, strip) ->
            strip.detach()
            strips.remove(edge)
        }
        wanted.forEach { (edge, peer) ->
            if (edge !in strips) HandoffStrip.attach(owner, peer, edge, screen, this)?.let { strips[edge] = it }
        }
    }
}
