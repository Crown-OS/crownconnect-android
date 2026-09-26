package com.crownos.connect.protocol

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeerCommandTest {
    private fun json(text: String) = Json.parseToJsonElement(text)

    @Test
    fun parsesTheCommandsTheBridgeEmits() {
        assertEquals(PeerCommand.Call(CallRequest.Pickup("7")), PeerCommand.parse(json("""{"Call":{"Pickup":{"call_id":"7"}}}""")))
        assertEquals(PeerCommand.Media(MediaCommand.Play), PeerCommand.parse(json("""{"Media":"Play"}""")))
        assertEquals(PeerCommand.Media(MediaCommand.Seek(63)), PeerCommand.parse(json("""{"Media":{"Seek":{"percent":63}}}""")))
        assertEquals(PeerCommand.Lock, PeerCommand.parse(json(""""Lock"""")))
        assertEquals(
            PeerCommand.Reply("c", "on my \"way\""),
            PeerCommand.parse(json("""{"Reply":{"conversation_id":"c","text":"on my \"way\""}}""")),
        )
        assertEquals(PeerCommand.UnicursorEnter(Edge.Left, 32768), PeerCommand.parse(json("""{"UnicursorEnter":{"edge":"Left","position":32768}}""")))
        assertEquals(PeerCommand.FileOffer(4_294_967_295), PeerCommand.parse(json("""{"FileOffer":{"ffsp_transfer_id":4294967295}}""")))
        assertNull(PeerCommand.parse(json("""{"Explode":{}}""")))
    }

    @Test
    fun encodesCommandsInTheSameShape() {
        val commands = listOf(
            """{"Call":{"Dial":{"number":"+44 20 7946 0000"}}}""" to PeerCommand.Call(CallRequest.Dial("+44 20 7946 0000")),
            """{"Media":"Pause"}""" to PeerCommand.Media(MediaCommand.Pause),
            """{"SetVolume":{"percent":35,"muted":false}}""" to PeerCommand.SetVolume(35, false),
            """"Lock"""" to PeerCommand.Lock,
            """{"SetHotspot":{"enabled":true}}""" to PeerCommand.SetHotspot(true),
            """{"UnicursorLeave":{"edge":"Top","position":65535}}""" to PeerCommand.UnicursorLeave(Edge.Top, 70_000),
        )
        commands.forEach { (expected, command) -> assertEquals(json(expected), command.toJson()) }
    }

    @Test
    fun notificationsMatchTheBridgeRoundTrip() {
        val document = NotificationsDocument(
            listOf(NotificationEntry("n", "Chat", "t", "b", listOf(NotificationAction("r", "Reply", acceptsText = true)))),
        )
        assertEquals(
            json("""{"active":[{"id":"n","app":"Chat","title":"t","body":"b","actions":[{"id":"r","label":"Reply","accepts_text":true}]}]}"""),
            NotificationsDocument.serializer().encodeDocument(document),
        )
    }
}
