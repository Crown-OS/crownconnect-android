package com.crownos.connect.unicursor

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CapturedInputSenderTest {
    private val moments = mutableListOf<String>()
    private val sender = CapturedInputSender { moments += it }

    @Test
    fun relativeMotionAndButtonsBecomeOneMoment() {
        sender.motion(captured(MotionEvent.ACTION_MOVE, dx = 3f, dy = -2f, buttons = MotionEvent.BUTTON_PRIMARY))
        val moment = moments.single()
        assertTrue(moment, moment.contains("""{"PointerRelative":{"dx":3.0,"dy":-2.0}}"""))
        assertTrue(moment, moment.contains("""{"PointerButton":{"button":272,"pressed":true}}"""))
        assertTrue(moment, moment.endsWith(""""Frame"]}"""))
    }

    @Test
    fun handingBackReleasesEverythingStillHeld() {
        sender.motion(captured(MotionEvent.ACTION_MOVE, dx = 1f, dy = 0f, buttons = MotionEvent.BUTTON_SECONDARY))
        sender.key(KeyEvent.KEYCODE_A, true)
        sender.key(KeyEvent.KEYCODE_A, true)
        sender.releaseAll()
        assertEquals(3, moments.size)
        val released = moments.last()
        assertTrue(released, released.contains("""{"Key":{"code":30,"pressed":false}}"""))
        assertTrue(released, released.contains("""{"PointerButton":{"button":273,"pressed":false}}"""))
    }

    @Test
    fun wheelNotchesScrollInV120Units() {
        sender.motion(captured(MotionEvent.ACTION_SCROLL, dx = 0f, dy = 0f, buttons = 0, vscroll = 1f))
        assertTrue(moments.single(), moments.single().contains("""{"Scroll":{"horizontal_v120":0,"vertical_v120":-120}}"""))
    }

    private fun captured(action: Int, dx: Float, dy: Float, buttons: Int, vscroll: Float = 0f): MotionEvent {
        val properties = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_MOUSE })
        val coords = arrayOf(MotionEvent.PointerCoords().apply { x = dx; y = dy; setAxisValue(MotionEvent.AXIS_VSCROLL, vscroll) })
        return MotionEvent.obtain(0, 0, action, 1, properties, coords, 0, buttons, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE_RELATIVE, 0)
    }
}
