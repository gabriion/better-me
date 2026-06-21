package com.gabriion.betterme.domain.evolution

import com.gabriion.betterme.core.db.WeightEntity
import com.gabriion.betterme.core.db.WorkoutSetEntity
import com.gabriion.betterme.domain.model.Highlight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Pure-Kotlin computation of "highlight" cards shown on the Evolution screen.
 *
 * No Android / Hilt / Room dependencies — fully unit-testable.
 */
object HighlightsEngine {

    private const val STEADY_THRESHOLD_KG = 0.3

    fun compute(
        weights: List<WeightEntity>,
        workoutSets: List<WorkoutSetEntity>,
        today: LocalDate = LocalDate.now()
    ): List<Highlight> {
        val out = mutableListOf<Highlight>()
        weightDeltaThisMonth(weights, today)?.let(out::add)
        workoutStreak(workoutSets, today)?.let(out::add)
        // Sleep / mindfulness placeholders are intentionally null until Garmin sync ships.
        if (out.isEmpty()) {
            out.add(
                Highlight(
                    icon = "wave",
                    title = "Welcome back",
                    body = "Log a weight or a workout to start seeing your trends here."
                )
            )
        }
        return out
    }

    internal fun weightDeltaThisMonth(
        weights: List<WeightEntity>,
        today: LocalDate
    ): Highlight? {
        if (weights.isEmpty()) return null
        val latest = weights.maxByOrNull { it.date } ?: return null
        val monthAgo = today.minusDays(30)
        val baseline = weights
            .filter { it.id != latest.id }
            .minByOrNull { abs(java.time.temporal.ChronoUnit.DAYS.between(it.date, monthAgo)) }
            ?: return null
        val delta = latest.kg - baseline.kg
        val body = when {
            abs(delta) < STEADY_THRESHOLD_KG -> "Steady this month"
            delta < 0 -> "Lost ${formatKg(-delta)} kg this month"
            else -> "Gained ${formatKg(delta)} kg this month"
        }
        return Highlight(
            icon = "scale",
            title = "${formatKg(latest.kg)} kg",
            body = body
        )
    }

    internal fun workoutStreak(
        sets: List<WorkoutSetEntity>,
        today: LocalDate
    ): Highlight? {
        if (sets.isEmpty()) return null
        val zone = ZoneId.systemDefault()
        val daysWithSets: Set<LocalDate> = sets
            .map { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
            .toSet()
        var streak = 0
        var cursor = today
        while (daysWithSets.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        if (streak == 0) return null
        val body = if (streak == 1) "1 day streak — keep it going" else "$streak day streak"
        return Highlight(
            icon = "streak",
            title = "Workout streak",
            body = body
        )
    }

    private fun formatKg(value: Double): String = String.format("%.1f", value)
}
