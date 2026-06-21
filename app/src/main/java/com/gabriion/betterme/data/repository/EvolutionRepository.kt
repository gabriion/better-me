package com.gabriion.betterme.data.repository

import com.gabriion.betterme.core.db.WeightDao
import com.gabriion.betterme.core.db.WeightEntity
import com.gabriion.betterme.core.db.WorkoutDao
import com.gabriion.betterme.domain.evolution.HighlightsEngine
import com.gabriion.betterme.domain.model.Highlight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvolutionRepository @Inject constructor(
    private val weightDao: WeightDao,
    private val workoutDao: WorkoutDao
) {
    fun observeWeights(): Flow<List<WeightEntity>> = weightDao.observeAll()

    fun observeWeightsSince(days: Int): Flow<List<WeightEntity>> =
        weightDao.observeSince(LocalDate.now().minusDays(days.toLong()))

    suspend fun addWeight(date: LocalDate, kg: Double, note: String?) {
        weightDao.insert(WeightEntity(date = date, kg = kg, note = note))
    }

    suspend fun deleteWeight(id: Long) {
        weightDao.deleteById(id)
    }

    fun observeHighlights(): Flow<List<Highlight>> =
        combine(
            weightDao.observeAll(),
            workoutDao.observeRecent(limit = 500)
        ) { weights, sets ->
            HighlightsEngine.compute(weights, sets, LocalDate.now())
        }
}
