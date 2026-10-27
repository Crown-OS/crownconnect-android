package com.crownos.connect.files

import com.crownos.connect.llts.LltsEvent
import com.crownos.connect.llts.LltsEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FfspEventTest {
    @Test
    fun engineEventsParseIntoTheirKinds() {
        assertEquals(
            FfspEvent.Offered("d", 3, listOf(OfferedFile("report.pdf", 2048))),
            FfspEvent.parse("""{"type":"transferOffered","device":"d","transfer":3,"entries":[{"name":"report.pdf","len":2048}]}"""),
        )
        assertEquals(
            FfspEvent.Progress("d", 512, 2048),
            FfspEvent.parse("""{"type":"transferProgress","device":"d","deliveredBytes":512,"totalBytes":2048,"lanes":[]}"""),
        )
        val finished = FfspEvent.parse("""{"type":"transferFinished","device":"d","outcome":"Completed"}""")
        assertTrue((finished as FfspEvent.Finished).completed)
        assertEquals(FfspEvent.Failed("no route"), FfspEvent.parse("""{"type":"laneFailed","medium":2,"endpoint":"x","reason":"no route"}"""))
        assertNull(FfspEvent.parse("""{"type":"laneOpened","device":"d","lane":1,"medium":2}"""))
        assertNull(FfspEvent.parse("not json"))
    }

    @Test
    fun theRuntimeFileOfferCarriesTheHostToDial() {
        assertEquals(
            LltsEvent.FileOffer("ab", 7, "192.168.1.4"),
            LltsEvents.decode("""{"type":"fileOffer","peer":"ab","transferId":7,"host":"192.168.1.4"}"""),
        )
        assertEquals(LltsEvent.FileOffer("ab", 7, null), LltsEvents.decode("""{"type":"fileOffer","peer":"ab","transferId":7,"host":null}"""))
    }
}
