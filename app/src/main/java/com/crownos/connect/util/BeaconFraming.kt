package com.crownos.connect.util

fun splitLengthPrefixed(encoded: ByteArray?): List<ByteArray> {
    if (encoded == null) return emptyList()
    val beacons = ArrayList<ByteArray>(2)
    var cursor = 0
    while (cursor < encoded.size) {
        val length = encoded[cursor].toInt() and 0xFF
        val end = cursor + 1 + length
        if (end > encoded.size) break
        beacons += encoded.copyOfRange(cursor + 1, end)
        cursor = end
    }
    return beacons
}
