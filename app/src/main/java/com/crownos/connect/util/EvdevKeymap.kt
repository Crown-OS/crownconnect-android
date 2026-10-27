package com.crownos.connect.util

import android.view.KeyEvent

data class Keystroke(val code: Int, val shifted: Boolean)

object EvdevKeymap {
    const val KEY_ESC = 1
    const val KEY_BACKSPACE = 14
    const val KEY_TAB = 15
    const val KEY_ENTER = 28
    const val KEY_LEFTCTRL = 29
    const val KEY_LEFTSHIFT = 42
    const val KEY_RIGHTSHIFT = 54
    const val KEY_LEFTALT = 56
    const val KEY_CAPSLOCK = 58
    const val KEY_RIGHTCTRL = 97
    const val KEY_RIGHTALT = 100
    const val KEY_HOME = 102
    const val KEY_LEFTMETA = 125
    const val KEY_RIGHTMETA = 126
    const val KEY_BACK = 158
    const val KEY_HOMEPAGE = 172

    private val keycodes: Map<Int, Int> = buildMap {
        put(KEY_ESC, KeyEvent.KEYCODE_ESCAPE)
        "1234567890".forEachIndexed { index, digit -> put(2 + index, KeyEvent.KEYCODE_0 + (digit - '0')) }
        put(12, KeyEvent.KEYCODE_MINUS)
        put(13, KeyEvent.KEYCODE_EQUALS)
        put(KEY_BACKSPACE, KeyEvent.KEYCODE_DEL)
        put(KEY_TAB, KeyEvent.KEYCODE_TAB)
        letterRow(16, "qwertyuiop")
        put(26, KeyEvent.KEYCODE_LEFT_BRACKET)
        put(27, KeyEvent.KEYCODE_RIGHT_BRACKET)
        put(KEY_ENTER, KeyEvent.KEYCODE_ENTER)
        put(KEY_LEFTCTRL, KeyEvent.KEYCODE_CTRL_LEFT)
        letterRow(30, "asdfghjkl")
        put(39, KeyEvent.KEYCODE_SEMICOLON)
        put(40, KeyEvent.KEYCODE_APOSTROPHE)
        put(41, KeyEvent.KEYCODE_GRAVE)
        put(KEY_LEFTSHIFT, KeyEvent.KEYCODE_SHIFT_LEFT)
        put(43, KeyEvent.KEYCODE_BACKSLASH)
        letterRow(44, "zxcvbnm")
        put(51, KeyEvent.KEYCODE_COMMA)
        put(52, KeyEvent.KEYCODE_PERIOD)
        put(53, KeyEvent.KEYCODE_SLASH)
        put(KEY_RIGHTSHIFT, KeyEvent.KEYCODE_SHIFT_RIGHT)
        put(55, KeyEvent.KEYCODE_NUMPAD_MULTIPLY)
        put(KEY_LEFTALT, KeyEvent.KEYCODE_ALT_LEFT)
        put(57, KeyEvent.KEYCODE_SPACE)
        put(KEY_CAPSLOCK, KeyEvent.KEYCODE_CAPS_LOCK)
        (0 until 10).forEach { put(59 + it, KeyEvent.KEYCODE_F1 + it) }
        put(69, KeyEvent.KEYCODE_NUM_LOCK)
        put(70, KeyEvent.KEYCODE_SCROLL_LOCK)
        listOf(7, 8, 9).forEachIndexed { index, digit -> put(71 + index, KeyEvent.KEYCODE_NUMPAD_0 + digit) }
        put(74, KeyEvent.KEYCODE_NUMPAD_SUBTRACT)
        listOf(4, 5, 6).forEachIndexed { index, digit -> put(75 + index, KeyEvent.KEYCODE_NUMPAD_0 + digit) }
        put(78, KeyEvent.KEYCODE_NUMPAD_ADD)
        listOf(1, 2, 3).forEachIndexed { index, digit -> put(79 + index, KeyEvent.KEYCODE_NUMPAD_0 + digit) }
        put(82, KeyEvent.KEYCODE_NUMPAD_0)
        put(83, KeyEvent.KEYCODE_NUMPAD_DOT)
        put(87, KeyEvent.KEYCODE_F11)
        put(88, KeyEvent.KEYCODE_F12)
        put(96, KeyEvent.KEYCODE_NUMPAD_ENTER)
        put(KEY_RIGHTCTRL, KeyEvent.KEYCODE_CTRL_RIGHT)
        put(98, KeyEvent.KEYCODE_NUMPAD_DIVIDE)
        put(99, KeyEvent.KEYCODE_SYSRQ)
        put(KEY_RIGHTALT, KeyEvent.KEYCODE_ALT_RIGHT)
        put(KEY_HOME, KeyEvent.KEYCODE_MOVE_HOME)
        put(103, KeyEvent.KEYCODE_DPAD_UP)
        put(104, KeyEvent.KEYCODE_PAGE_UP)
        put(105, KeyEvent.KEYCODE_DPAD_LEFT)
        put(106, KeyEvent.KEYCODE_DPAD_RIGHT)
        put(107, KeyEvent.KEYCODE_MOVE_END)
        put(108, KeyEvent.KEYCODE_DPAD_DOWN)
        put(109, KeyEvent.KEYCODE_PAGE_DOWN)
        put(110, KeyEvent.KEYCODE_INSERT)
        put(111, KeyEvent.KEYCODE_FORWARD_DEL)
        put(113, KeyEvent.KEYCODE_VOLUME_MUTE)
        put(114, KeyEvent.KEYCODE_VOLUME_DOWN)
        put(115, KeyEvent.KEYCODE_VOLUME_UP)
        put(116, KeyEvent.KEYCODE_POWER)
        put(119, KeyEvent.KEYCODE_BREAK)
        put(KEY_LEFTMETA, KeyEvent.KEYCODE_META_LEFT)
        put(KEY_RIGHTMETA, KeyEvent.KEYCODE_META_RIGHT)
        put(127, KeyEvent.KEYCODE_MENU)
        put(KEY_BACK, KeyEvent.KEYCODE_BACK)
        put(163, KeyEvent.KEYCODE_MEDIA_NEXT)
        put(164, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        put(165, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        put(166, KeyEvent.KEYCODE_MEDIA_STOP)
        put(KEY_HOMEPAGE, KeyEvent.KEYCODE_HOME)
        put(217, KeyEvent.KEYCODE_SEARCH)
        put(224, KeyEvent.KEYCODE_BRIGHTNESS_DOWN)
        put(225, KeyEvent.KEYCODE_BRIGHTNESS_UP)
    }

    private val unshiftedCharacters = mapOf(
        12 to '-', 13 to '=', 26 to '[', 27 to ']', 39 to ';', 40 to '\'', 41 to '`', 43 to '\\',
        51 to ',', 52 to '.', 53 to '/', 57 to ' ',
    )
    private val shiftedCharacters = mapOf(
        12 to '_', 13 to '+', 26 to '{', 27 to '}', 39 to ':', 40 to '"', 41 to '~', 43 to '|',
        51 to '<', 52 to '>', 53 to '?', 57 to ' ',
    )
    private const val SHIFTED_DIGITS = "!@#$%^&*()"

    private val evdevByAndroid: Map<Int, Int> = keycodes.entries.associate { (evdev, android) -> android to evdev }

    fun androidKeycode(evdev: Int): Int? = keycodes[evdev]

    fun evdevCode(androidKeycode: Int): Int? = evdevByAndroid[androidKeycode]

    private val keystrokes: Map<Char, Keystroke> by lazy {
        buildMap {
            put('\n', Keystroke(KEY_ENTER, false))
            put('\t', Keystroke(KEY_TAB, false))
            for (shifted in listOf(true, false)) {
                keycodes.keys.forEach { evdev -> character(evdev, shifted)?.let { put(it, Keystroke(evdev, shifted)) } }
            }
        }
    }

    fun keystrokeFor(character: Char): Keystroke? = keystrokes[character]

    fun isModifier(evdev: Int) = evdev in modifierMeta

    private val modifierMeta = mapOf(
        KEY_LEFTSHIFT to (KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON),
        KEY_RIGHTSHIFT to (KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_RIGHT_ON),
        KEY_LEFTCTRL to (KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON),
        KEY_RIGHTCTRL to (KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_RIGHT_ON),
        KEY_LEFTALT to (KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON),
        KEY_RIGHTALT to (KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON),
        KEY_LEFTMETA to (KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON),
        KEY_RIGHTMETA to (KeyEvent.META_META_ON or KeyEvent.META_META_RIGHT_ON),
    )

    fun metaState(pressed: Collection<Int>): Int = pressed.fold(0) { meta, code -> meta or (modifierMeta[code] ?: 0) }

    fun character(evdev: Int, shifted: Boolean): Char? {
        val keycode = keycodes[evdev] ?: return null
        return when (keycode) {
            in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> ('a' + (keycode - KeyEvent.KEYCODE_A)).let { if (shifted) it.uppercaseChar() else it }
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> if (evdev in 2..11 && shifted) {
                SHIFTED_DIGITS[(keycode - KeyEvent.KEYCODE_0 + 9) % 10]
            } else {
                '0' + (keycode - KeyEvent.KEYCODE_0)
            }
            else -> (if (shifted) shiftedCharacters else unshiftedCharacters)[evdev]
        }
    }

    private fun MutableMap<Int, Int>.letterRow(firstCode: Int, letters: String) =
        letters.forEachIndexed { index, letter -> put(firstCode + index, KeyEvent.KEYCODE_A + (letter - 'a')) }
}
