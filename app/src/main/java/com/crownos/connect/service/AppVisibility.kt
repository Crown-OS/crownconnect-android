package com.crownos.connect.service

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppVisibility : DefaultLifecycleObserver {
    private val foregroundState = MutableStateFlow(false)
    val foreground: StateFlow<Boolean> = foregroundState.asStateFlow()

    val isForeground: Boolean get() = foregroundState.value

    fun install() = ProcessLifecycleOwner.get().lifecycle.addObserver(this)

    override fun onStart(owner: LifecycleOwner) {
        foregroundState.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        foregroundState.value = false
    }
}
