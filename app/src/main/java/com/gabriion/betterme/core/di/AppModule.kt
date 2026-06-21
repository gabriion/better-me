package com.gabriion.betterme.core.di

import com.gabriion.betterme.garmin.GarminClient
import com.gabriion.betterme.garmin.GarminClientStub
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideGarminClient(): GarminClient = GarminClientStub()
}
