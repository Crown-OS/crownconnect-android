package com.crownos.connect.unicursor

import com.crownos.connect.input.InputInjectors
import com.crownos.connect.input.KeyStateTracker
import com.crownos.connect.protocol.Edge
import com.crownos.connect.protocol.PeerCommand
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.util.EdgeGeometry
import com.crownos.connect.util.ScreenGeometry
import com.crownos.connect.util.ScreenPoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedCursor @Inject constructor(
    private val runtime: LltsRuntime,
    private val overlay: CursorOverlay,
    private val screen: ScreenGeometry,
    private val injectors: InputInjectors,
) {
    private var peer: String? = null
    private var entryEdge = Edge.Left
    private var position = ScreenPoint(0f, 0f)
    val keys = KeyStateTracker()

    val point: ScreenPoint get() = position

    fun isHeldBy(candidate: String) = peer == candidate

    fun enter(from: String, laptopEdge: Edge, edgePosition: Int) {
        val size = screen.size
        peer = from
        entryEdge = laptopEdge.facing
        position = EdgeGeometry.entryPoint(entryEdge, EdgeGeometry.fractionOf(edgePosition), size.width, size.height)
        if (overlay.canShow) overlay.show(position.x, position.y)
        injectors.active?.pointerMove(position.x, position.y)
    }

    fun moveBy(dx: Float, dy: Float) {
        val holder = peer ?: return
        val size = screen.size
        val moved = ScreenPoint(position.x + dx * size.density, position.y + dy * size.density)
        if (EdgeGeometry.crossed(entryEdge, moved, size.width, size.height)) {
            val fraction = EdgeGeometry.fractionAlong(entryEdge, EdgeGeometry.clamp(moved, size.width, size.height), size.width, size.height)
            runtime.sendCommand(holder, PeerCommand.UnicursorLeave(entryEdge, EdgeGeometry.positionOf(fraction)).toJson())
            release()
            return
        }
        position = EdgeGeometry.clamp(moved, size.width, size.height)
        overlay.moveTo(position.x, position.y)
        injectors.active?.pointerMove(position.x, position.y)
    }

    fun release() {
        val injector = injectors.active
        keys.drain().forEach { injector?.key(it, false, keys.held) }
        peer = null
        overlay.hide()
    }

    fun releaseIfHeldBy(candidate: String) {
        if (peer == candidate) release()
    }
}
