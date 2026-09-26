package com.crownos.connect.util

private const val HEX_DIGITS = "0123456789abcdef"

fun ByteArray.toHex(): String {
    val out = CharArray(size * 2)
    forEachIndexed { index, byte ->
        val value = byte.toInt() and 0xFF
        out[index * 2] = HEX_DIGITS[value ushr 4]
        out[index * 2 + 1] = HEX_DIGITS[value and 0x0F]
    }
    return String(out)
}

fun String.hexToBytesOrNull(): ByteArray? {
    if (length % 2 != 0) return null
    val out = ByteArray(length / 2)
    for (index in out.indices) {
        val high = Character.digit(this[index * 2], 16)
        val low = Character.digit(this[index * 2 + 1], 16)
        if (high < 0 || low < 0) return null
        out[index] = ((high shl 4) or low).toByte()
    }
    return out
}
