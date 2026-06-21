package com.gabriion.betterme.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sets ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE date >= :sinceEpochMillis ORDER BY date DESC, id DESC")
    fun observeSince(sinceEpochMillis: Long): Flow<List<WorkoutSetEntity>>

    @Insert
    suspend fun insertSet(entity: WorkoutSetEntity): Long

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("SELECT * FROM training_plan WHERE id = 0")
    fun observePlan(): Flow<TrainingPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlan(plan: TrainingPlanEntity)
}
