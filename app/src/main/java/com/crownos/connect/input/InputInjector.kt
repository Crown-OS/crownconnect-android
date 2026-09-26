package com.crownos.connect.input

enum class TouchPhase { Down, Move, Up }

interface InputInjector {
    val isAvailable: Boolean

    fun touch(id: Int, phase: TouchPhase, x: Float = 0f, y: Float = 0f)

    fun cancelTouches()

    fun pointerMove(x: Float, y: Float)

    fun pointerButton(x: Float, y: Float, button: Int, pressed: Boolean)

    fun scroll(x: Float, y: Float, horizontalV120: Int, verticalV120: Int)

    fun key(evdevCode: Int, pressed: Boolean, heldKeys: Set<Int>)
}
