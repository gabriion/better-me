package com.gabriion.betterme.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        GoalEntity::class,
        GoalProgressEntity::class,
        WeightEntity::class,
        FoodEntryEntity::class,
        FoodCacheEntity::class,
        WorkoutSetEntity::class,
        TrainingPlanEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun weightDao(): WeightDao
    abstract fun foodDao(): FoodDao
    abstract fun workoutDao(): WorkoutDao
}
