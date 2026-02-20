package com.fenn.callguard.di

import com.fenn.callguard.network.CircuitBreaker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCircuitBreaker(): CircuitBreaker = CircuitBreaker(
        windowSize = 10,
        failureThreshold = 0.5,
        reopenAfterMs = 60_000L,
    )
}
