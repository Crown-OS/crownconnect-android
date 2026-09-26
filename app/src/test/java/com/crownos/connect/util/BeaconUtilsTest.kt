package com.crownos.connect.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BeaconUtilsTest {
    @Test
    fun splitsLengthPrefixedBeaconsAndDropsATruncatedTail() {
        val encoded = byteArrayOf(2, 1, 2, 3, 7, 8, 9, 5, 1)
        val beacons = splitLengthPrefixed(encoded)
        assertEquals(2, beacons.size)
        assertArrayEquals(byteArrayOf(1, 2), beacons[0])
        assertArrayEquals(byteArrayOf(7, 8, 9), beacons[1])
        assertEquals(emptyList<ByteArray>(), splitLengthPrefixed(null))
    }

    @Test
    fun hexRoundTripsAndRejectsNonHex() {
        val bytes = byteArrayOf(0, 15, -1, 16)
        assertEquals("000fff10", bytes.toHex())
        assertArrayEquals(bytes, "000FFF10".hexToBytesOrNull())
        assertNull("0g".hexToBytesOrNull())
        assertNull("abc".hexToBytesOrNull())
    }
}
