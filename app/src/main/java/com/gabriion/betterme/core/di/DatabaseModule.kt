package com.gabriion.betterme.core.di

import android.content.Context
import androidx.room.Room
import com.gabriion.betterme.core.db.AppDatabase
import com.gabriion.betterme.core.db.GoalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "betterme.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()
}
