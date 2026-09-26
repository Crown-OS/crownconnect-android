package com.crownos.connect.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProtocolJsonTest {
    private fun json(text: String): JsonElement = Json.parseToJsonElement(text)

    @Test
    fun startConfigUsesTheBridgeFieldNames() {
        val config = StartConfig("Pixel", "9", DeviceClass.Phone, videoCodecs = listOf(VideoCodec.Hevc, VideoCodec.H264), features = listOf(Feature.Battery))
        assertEquals(
            json("""{"name":"Pixel","model":"9","class":"Phone","port":47470,"videoCodecs":["Hevc","H264"],"audioCodecs":["Opus"],"features":["Battery"]}"""),
            json(config.toJson()),
        )
    }

    @Test
    fun featureRequestsMatchTheSignallingShapes() {
        assertEquals(
            json("""{"Mirror":{"direction":"ToPeer","limits":{"max_width":1920,"max_height":1080,"max_fps":60},"remote_input":true}}"""),
            FeatureRequest.Mirror(Direction.ToPeer, VideoLimits(1920, 1080, 60), remoteInput = true).toJson(),
        )
        assertEquals(json(""""Mic""""), FeatureRequest.Mic.toJson())
        assertEquals(
            json("""{"Monitor":{"width":2560,"height":1600,"refresh_mhz":60000,"scale_120":180,"placement":"Right"}}"""),
            FeatureRequest.Monitor(2560, 1600, 60_000, 180, Edge.Right).toJson(),
        )
    }

    @Test
    fun featureRequestsRoundTripFromMediaStartEvents() {
        val camera = json("""{"Camera":{"lens":"Back","limits":{"max_width":1280,"max_height":720,"max_fps":30}}}""")
        assertEquals(FeatureRequest.Camera(CameraLens.Back, VideoLimits(1280, 720, 30)), FeatureRequest.parse(camera))
        assertEquals(FeatureRequest.Mic, FeatureRequest.parse(json(""""Mic"""")))
        assertNull(FeatureRequest.parse(json("""{"Mirror":{}}""")))
    }

    @Test
    fun stateDocumentsMatchTheBridgeRoundTrips() {
        assertEquals(
            json("""{"percent":58,"charging":true,"time_to_empty_min":null}"""),
            BatteryDocument.serializer().encodeDocument(BatteryDocument(58, true)),
        )
        assertEquals(
            json("""{"calls":[{"call_id":"1","number":"+1","contact_name":null,"status":"Ringing","answered_unix_ms":null}]}"""),
            CallStateDocument.serializer().encodeDocument(CallStateDocument(listOf(CallInfo("1", "+1", null, CallStatus.Ringing)))),
        )
        assertEquals(
            json("""{"mime":"text/plain","content":{"Inline":"héllo"}}"""),
            ClipboardDocument("text/plain", "héllo").toJson(),
        )
    }

    @Test
    fun remoteDocumentsDecodeAndRejectWrongShapes() {
        val incoming = json("""{"mime":"text/plain","content":{"Inline":"hi"},"stamp":{"wall_ms":7,"counter":0}}""")
        assertEquals(ClipboardDocument("text/plain", "hi"), ClipboardDocument.parse(incoming))
        assertNull(BatteryDocument.serializer().decodeDocument(json("""{"percent":"full"}""")))
        assertEquals(VolumeDocument(30, false), VolumeDocument.serializer().decodeDocument(json("""{"percent":30,"muted":false}""")))
    }
}
