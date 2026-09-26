package com.crownos.connect.input

import android.view.KeyEvent
import com.crownos.connect.protocol.InputEvent
import com.crownos.connect.protocol.InputMomentWriter
import com.crownos.connect.util.EvdevKeymap
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputModelTest {
    private fun json(text: String) = Json.parseToJsonElement(text)

    @Test
    fun theMomentWriterProducesTheBridgeShape() {
        val moment = InputMomentWriter()
            .begin(42)
            .add(InputEvent.PointerRelative(3f, -1.5f))
            .add(InputEvent.PointerButton(272, true))
            .add(InputEvent.TouchDown(3, 1.4f, -0.2f))
            .add(InputEvent.Frame)
            .finish()
        assertEquals(
            json("""{"timeMs":42,"events":[{"PointerRelative":{"dx":3.0,"dy":-1.5}},{"PointerButton":{"button":272,"pressed":true}},{"TouchDown":{"id":3,"x":1.0,"y":0.0}},"Frame"]}"""),
            json(moment),
        )
    }

    @Test
    fun incomingEventsParse() {
        assertEquals(InputEvent.Scroll(0, -120), InputEvent.parse(json("""{"Scroll":{"horizontal_v120":0,"vertical_v120":-120}}""")))
        assertEquals(InputEvent.Key(30, true), InputEvent.parse(json("""{"Key":{"code":30,"pressed":true}}""")))
        assertEquals(InputEvent.TouchCancel, InputEvent.parse(json(""""TouchCancel"""")))
        assertNull(InputEvent.parse(json(""""Wiggle"""")))
    }

    @Test
    fun evdevCodesMapToAndroidKeysAndCharacters() {
        assertEquals(KeyEvent.KEYCODE_A, EvdevKeymap.androidKeycode(30))
        assertEquals(KeyEvent.KEYCODE_ENTER, EvdevKeymap.androidKeycode(28))
        assertEquals('A', EvdevKeymap.character(30, shifted = true))
        assertEquals('!', EvdevKeymap.character(2, shifted = true))
        assertEquals('0', EvdevKeymap.character(11, shifted = false))
        assertEquals(')', EvdevKeymap.character(11, shifted = true))
        assertEquals('?', EvdevKeymap.character(53, shifted = true))
        val meta = EvdevKeymap.metaState(listOf(EvdevKeymap.KEY_LEFTCTRL, EvdevKeymap.KEY_LEFTSHIFT))
        assertEquals(KeyEvent.META_CTRL_ON, meta and KeyEvent.META_CTRL_ON)
        assertEquals(KeyEvent.META_SHIFT_ON, meta and KeyEvent.META_SHIFT_ON)
    }

    @Test
    fun keySnapshotsReleaseEverythingNoLongerHeld() {
        val tracker = KeyStateTracker()
        listOf(29, 42, 30).forEach { tracker.update(it, true) }
        assertEquals(listOf(42, 30), tracker.releasesFor(listOf(29)))
        assertEquals(listOf(29, 42, 30), tracker.drain())
        assertEquals(emptySet<Int>(), tracker.held)
    }
}
