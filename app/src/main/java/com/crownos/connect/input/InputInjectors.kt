package com.crownos.connect.input

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputInjectors @Inject constructor(
    private val shizuku: ShizukuInputInjector,
    private val accessibility: AccessibilityInputInjector,
) {
    val active: InputInjector?
        get() = when {
            shizuku.isAvailable -> shizuku
            accessibility.isAvailable -> accessibility
            else -> null
        }

    val canInject: Boolean get() = active != null
}
