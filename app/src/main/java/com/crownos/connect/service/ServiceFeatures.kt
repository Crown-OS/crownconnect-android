package com.crownos.connect.service

import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

interface ServiceFeature {
    fun start(scope: CoroutineScope)
    fun stop()
}

@Singleton
class ServiceFeatures @Inject constructor(
    private val features: Set<@JvmSuppressWildcards ServiceFeature>,
) {
    fun start(scope: CoroutineScope) = features.forEach { it.start(scope) }

    fun stop() = features.forEach { it.stop() }
}
