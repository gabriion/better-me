package com.gabriion.betterme.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    fun observeGoal(id: Long): Flow<GoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET archived = 1 WHERE id = :id")
    suspend fun archiveGoal(id: Long)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Insert
    suspend fun insertProgress(event: GoalProgressEntity): Long

    @Query("SELECT * FROM goal_progress WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun observeProgressForGoal(goalId: Long): Flow<List<GoalProgressEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM goal_progress WHERE goalId = :goalId")
    fun observeTotalProgress(goalId: Long): Flow<Double>
}
