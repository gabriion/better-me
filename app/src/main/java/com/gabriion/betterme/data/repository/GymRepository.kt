package com.gabriion.betterme.data.repository

import android.content.Context
import com.gabriion.betterme.core.db.TrainingPlanEntity
import com.gabriion.betterme.core.db.WorkoutDao
import com.gabriion.betterme.core.db.WorkoutSetEntity
import com.gabriion.betterme.domain.model.Exercise
import com.gabriion.betterme.domain.model.WorkoutTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GymRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutDao: WorkoutDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val exercisesCache: List<Exercise> by lazy {
        runCatching {
            val raw = context.assets.open("content/exercises.json")
                .bufferedReader().use { it.readText() }
            json.decodeFromString<List<Exercise>>(raw)
        }.getOrDefault(emptyList())
    }

    private val templatesCache: List<WorkoutTemplate> by lazy {
        runCatching {
            val raw = context.assets.open("content/workout_templates.json")
                .bufferedReader().use { it.readText() }
            json.decodeFromString<List<WorkoutTemplate>>(raw)
        }.getOrDefault(emptyList())
    }

    fun exercises(): List<Exercise> = exercisesCache
    fun templates(): List<WorkoutTemplate> = templatesCache

    fun observePlan(): Flow<TrainingPlanEntity?> = workoutDao.observePlan()
    suspend fun savePlan(plan: TrainingPlanEntity) = workoutDao.savePlan(plan)

    fun observeRecentSets(): Flow<List<WorkoutSetEntity>> = workoutDao.observeRecent()
    suspend fun logSet(set: WorkoutSetEntity) {
        workoutDao.insertSet(set)
    }
    suspend fun deleteSet(id: Long) = workoutDao.deleteSet(id)
}
