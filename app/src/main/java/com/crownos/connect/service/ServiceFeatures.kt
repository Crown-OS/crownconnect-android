package com.crownos.connect.service

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
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

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceFeatureModule {
    @Multibinds
    abstract fun serviceFeatures(): Set<ServiceFeature>
}
