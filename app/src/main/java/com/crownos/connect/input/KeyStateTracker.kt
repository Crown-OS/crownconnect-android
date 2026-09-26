package com.crownos.connect.input

class KeyStateTracker {
    private val pressed = LinkedHashSet<Int>()

    val held: Set<Int> get() = pressed

    fun update(code: Int, down: Boolean): Boolean = if (down) pressed.add(code) else pressed.remove(code)

    fun releasesFor(snapshot: Collection<Int>): List<Int> = pressed.filter { it !in snapshot }

    fun drain(): List<Int> = pressed.toList().also { pressed.clear() }
}
