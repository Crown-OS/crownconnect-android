package com.crownos.connect.input

import com.crownos.connect.commands.PeerCommandHandler
import com.crownos.connect.llts.LltsEvent
import com.crownos.connect.protocol.Feature
import com.crownos.connect.protocol.FeatureRequest
import com.crownos.connect.protocol.InputEvent
import com.crownos.connect.protocol.MediaRole
import com.crownos.connect.protocol.PeerCommand
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.service.ServiceFeature
import com.crownos.connect.unicursor.CursorHandoff
import com.crownos.connect.unicursor.SharedCursor
import com.crownos.connect.util.Letterbox
import com.crownos.connect.util.ScreenGeometry
import com.crownos.connect.util.ScreenPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputRouter @Inject constructor(
    private val runtime: LltsRuntime,
    private val injectors: InputInjectors,
    private val cursor: SharedCursor,
    private val screen: ScreenGeometry,
    private val thread: InputThread,
    private val handoff: CursorHandoff,
) : ServiceFeature, PeerCommandHandler {
    private class MirrorControl(val frameWidth: Int, val frameHeight: Int) {
        val keys = KeyStateTracker()
        var pointer = ScreenPoint(0f, 0f)
    }

    private val mirrors = HashMap<String, MirrorControl>()
    private var job: Job? = null

    override fun start(scope: CoroutineScope) {
        if (job != null) return
        job = scope.launch(thread.dispatcher) {
            launch {
                runtime.inputEvents.collect { event ->
                    when (event) {
                        is LltsEvent.Input -> route(event.peer, event.events.mapNotNull(InputEvent::parse))
                        is LltsEvent.InputKeys -> releaseStaleKeys(event.peer, event.pressed)
                        else -> Unit
                    }
                }
            }
            runtime.events.collect { event ->
                when (event) {
                    is LltsEvent.MediaStart -> trackMirror(event)
                    is LltsEvent.MediaStop -> if (event.feature == Feature.Mirror.name) mirrors.remove(event.peer)
                    is LltsEvent.DeviceConnected -> if (!event.connected) forget(event.peer)
                    else -> Unit
                }
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        thread.post {
            mirrors.clear()
            cursor.release()
        }
    }

    override fun handle(peer: String, command: PeerCommand): Boolean = when (command) {
        is PeerCommand.UnicursorEnter -> {
            thread.post { cursor.enter(peer, command.edge, command.position) }
            true
        }
        is PeerCommand.UnicursorLeave -> {
            thread.post { cursor.releaseIfHeldBy(peer) }
            handoff.handedBack(peer)
            true
        }
        else -> false
    }

    private fun trackMirror(event: LltsEvent.MediaStart) {
        if (event.feature != Feature.Mirror.name || event.role != MediaRole.Encoder.name) return
        val request = FeatureRequest.parse(event.request) as? FeatureRequest.Mirror ?: return
        if (request.remoteInput) mirrors[event.peer] = MirrorControl(event.params.width, event.params.height)
    }

    private fun forget(peer: String) {
        mirrors.remove(peer)
        cursor.releaseIfHeldBy(peer)
    }

    private fun route(peer: String, events: List<InputEvent>) {
        val injector = injectors.active ?: return
        when {
            cursor.isHeldBy(peer) -> events.forEach { routeToCursor(it, injector) }
            else -> mirrors[peer]?.let { control -> events.forEach { routeToMirror(it, control, injector) } }
        }
    }

    private fun routeToCursor(event: InputEvent, injector: InputInjector) {
        val at = cursor.point
        when (event) {
            is InputEvent.PointerRelative -> cursor.moveBy(event.dx, event.dy)
            is InputEvent.PointerButton -> injector.pointerButton(at.x, at.y, event.button, event.pressed)
            is InputEvent.Scroll -> injector.scroll(at.x, at.y, event.horizontalV120, event.verticalV120)
            is InputEvent.Key -> if (cursor.keys.update(event.code, event.pressed)) {
                injector.key(event.code, event.pressed, cursor.keys.held)
            }
            else -> Unit
        }
    }

    private fun routeToMirror(event: InputEvent, control: MirrorControl, injector: InputInjector) {
        when (event) {
            is InputEvent.PointerMotion -> {
                control.pointer = toScreen(event.x, event.y, control)
                injector.pointerMove(control.pointer.x, control.pointer.y)
            }
            is InputEvent.PointerRelative -> {
                val size = screen.size
                control.pointer = ScreenPoint(
                    (control.pointer.x + event.dx * size.density).coerceIn(0f, size.width - 1f),
                    (control.pointer.y + event.dy * size.density).coerceIn(0f, size.height - 1f),
                )
                injector.pointerMove(control.pointer.x, control.pointer.y)
            }
            is InputEvent.PointerButton -> injector.pointerButton(control.pointer.x, control.pointer.y, event.button, event.pressed)
            is InputEvent.Scroll -> injector.scroll(control.pointer.x, control.pointer.y, event.horizontalV120, event.verticalV120)
            is InputEvent.Key -> if (control.keys.update(event.code, event.pressed)) {
                injector.key(event.code, event.pressed, control.keys.held)
            }
            is InputEvent.TouchDown -> toScreen(event.x, event.y, control).let { injector.touch(event.id, TouchPhase.Down, it.x, it.y) }
            is InputEvent.TouchMotion -> toScreen(event.x, event.y, control).let { injector.touch(event.id, TouchPhase.Move, it.x, it.y) }
            is InputEvent.TouchUp -> injector.touch(event.id, TouchPhase.Up)
            InputEvent.TouchCancel -> injector.cancelTouches()
            InputEvent.Frame -> Unit
        }
    }

    private fun toScreen(x: Float, y: Float, control: MirrorControl): ScreenPoint {
        val size = screen.size
        if (control.frameWidth <= 0 || control.frameHeight <= 0) return ScreenPoint(x * size.width, y * size.height)
        val fraction = Letterbox.fit(size.width, size.height, control.frameWidth, control.frameHeight)
            .toScreen(x, y, control.frameWidth, control.frameHeight)
        return ScreenPoint(fraction.x * (size.width - 1), fraction.y * (size.height - 1))
    }

    private fun releaseStaleKeys(peer: String, pressed: List<Int>) {
        val injector = injectors.active ?: return
        val tracker = when {
            cursor.isHeldBy(peer) -> cursor.keys
            else -> mirrors[peer]?.keys ?: return
        }
        tracker.releasesFor(pressed).forEach { code ->
            tracker.update(code, false)
            injector.key(code, false, tracker.held)
        }
    }
}
