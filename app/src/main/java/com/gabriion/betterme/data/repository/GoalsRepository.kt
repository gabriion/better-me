package com.gabriion.betterme.data.repository

import android.content.Context
import com.gabriion.betterme.core.db.GoalDao
import com.gabriion.betterme.core.db.GoalEntity
import com.gabriion.betterme.core.db.GoalProgressEntity
import com.gabriion.betterme.domain.model.Goal
import com.gabriion.betterme.domain.model.GoalProgressEvent
import com.gabriion.betterme.domain.model.GoalType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GoalTemplate(
    val title: String,
    val type: String,
    val target: Double,
    val unit: String,
    val isSportTracked: Boolean = false
) {
    fun goalType(): GoalType = runCatching { GoalType.valueOf(type) }.getOrDefault(GoalType.COUNT)
}

@Singleton
class GoalsRepository @Inject constructor(
    private val dao: GoalDao,
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun activeGoals(): Flow<List<Goal>> =
        dao.observeActiveGoals().map { list -> list.map { it.toDomain() } }

    fun goal(id: Long): Flow<Goal?> =
        dao.observeGoal(id).map { it?.toDomain() }

    suspend fun addGoal(goal: Goal): Long =
        dao.insertGoal(GoalEntity.fromDomain(goal))

    suspend fun archiveGoal(id: Long) = dao.archiveGoal(id)

    suspend fun logProgress(
        goalId: Long,
        amount: Double,
        note: String? = null,
        source: String = "manual"
    ): Long {
        val event = GoalProgressEvent(
            goalId = goalId,
            amount = amount,
            note = note,
            source = source
        )
        return dao.insertProgress(GoalProgressEntity.fromDomain(event))
    }

    fun eventsForGoal(id: Long): Flow<List<GoalProgressEvent>> =
        dao.observeProgressForGoal(id).map { list -> list.map { it.toDomain() } }

    fun totalProgress(goalId: Long): Flow<Double> = dao.observeTotalProgress(goalId)

    fun loadTemplates(): List<GoalTemplate> = runCatching {
        val raw = context.assets.open("content/goal_templates.json")
            .bufferedReader().use { it.readText() }
        json.decodeFromString<List<GoalTemplate>>(raw)
    }.getOrDefault(emptyList())
}
