package com.gabriion.betterme.core.di

import com.gabriion.betterme.health.CombinedHealthSource
import com.gabriion.betterme.health.HealthDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindHealthDataSource(impl: CombinedHealthSource): HealthDataSource
}
