package com.gabriion.betterme.health

import com.gabriion.betterme.core.db.WeightDao
import com.gabriion.betterme.core.db.WorkoutDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Computes app-internal wellbeing signals from our own DAOs. Always available
 * — no permissions, no network. Drives the meaningful chunk of Daily Tips
 * when Health Connect is absent or unauthorised.
 */
@Singleton
class AppSignalsSource @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val weightDao: WeightDao
) {
    suspend fun fetch(today: LocalDate = LocalDate.now()): AppSignals {
        val zone = ZoneId.systemDefault()
        val fortnightAgoMillis = today.minusDays(14).atStartOfDay(zone).toInstant().toEpochMilli()
        val recentSets = runCatching { workoutDao.observeSince(fortnightAgoMillis).first() }.getOrDefault(emptyList())
        val weights = runCatching { weightDao.observeAll().firstOrNull() }.getOrDefault(null) ?: emptyList()

        val lastWorkoutInstant = recentSets.firstOrNull()?.date?.let { Instant.ofEpochMilli(it) }
        val daysSinceLastWorkout = lastWorkoutInstant
            ?.atZone(zone)?.toLocalDate()
            ?.let { ChronoUnit.DAYS.between(it, today).toInt() }

        val datesWithWorkout = recentSets
            .map { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
            .toSet()
        var streak = 0
        var cursor = today
        while (cursor in datesWithWorkout) {
            streak++
            cursor = cursor.minusDays(1)
        }

        val recentWeights = weights.filter { it.date.isAfter(today.minusDays(28).minusDays(1)) }
            .sortedBy { it.date }
        val weightTrend = if (recentWeights.size >= 2) {
            val first = recentWeights.first()
            val last = recentWeights.last()
            val days = ChronoUnit.DAYS.between(first.date, last.date).coerceAtLeast(1)
            ((last.kg - first.kg) / days) * 7.0
        } else null

        val daysSinceLastWeighIn = weights.firstOrNull()?.date
            ?.let { ChronoUnit.DAYS.between(it, today).toInt() }

        return AppSignals(
            daysSinceLastWorkout = daysSinceLastWorkout,
            workoutStreakDays = streak,
            weightTrendKgPerWeek = weightTrend,
            daysSinceLastWeighIn = daysSinceLastWeighIn,
            caloriesLoggedToday = false,
            goalsBehindPaceCount = 0
        )
    }
}
