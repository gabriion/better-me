package com.gabriion.betterme.domain.gym

import com.gabriion.betterme.domain.model.Exercise
import com.gabriion.betterme.domain.model.WorkoutTemplate
import kotlin.random.Random

data class PlannedExercise(
    val exercise: Exercise,
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int,
)

data class BuiltWorkoutDay(
    val dayLabel: String,
    val exercises: List<PlannedExercise>,
)

/**
 * Pure-Kotlin gym plan builder. Maps day labels to muscle groups and
 * deterministically picks a compound + isolation pair per primary group,
 * scaling sets/reps by training level.
 */
object PlanBuilder {

    val DAY_LABEL_TO_GROUPS: Map<String, List<String>> = mapOf(
        "push"  to listOf("chest", "shoulders", "arms"),
        "pull"  to listOf("back", "arms"),
        "legs"  to listOf("legs"),
        "upper" to listOf("chest", "back", "shoulders", "arms"),
        "lower" to listOf("legs", "core"),
        "full"  to listOf("chest", "back", "legs", "shoulders", "arms", "core"),
    )

    private const val MIN_PER_DAY = 4
    private const val MAX_PER_DAY = 6

    /** Equipment classed as "compound" for picking the heaviest movement first. */
    private val COMPOUND_EQUIPMENT = setOf("barbell", "dumbbell", "bodyweight")

    fun build(
        template: WorkoutTemplate,
        allExercises: List<Exercise>,
        level: String,
    ): List<BuiltWorkoutDay> {
        if (allExercises.isEmpty() || template.days.isEmpty()) return emptyList()
        val seedBase = template.id.hashCode().toLong()

        return template.days.mapIndexed { dayIndex, label ->
            val groups = DAY_LABEL_TO_GROUPS[label.lowercase()] ?: emptyList()
            val picks = mutableListOf<Exercise>()
            val usedIds = mutableSetOf<String>()

            for (group in groups) {
                val seed = seedBase + dayIndex * 131L + group.hashCode().toLong()
                val pool = allExercises
                    .filter { it.muscleGroup.equals(group, ignoreCase = true) && it.id !in usedIds }
                    .shuffled(Random(seed))

                val compound = pool.firstOrNull { it.equipment.lowercase() in COMPOUND_EQUIPMENT }
                val isolation = pool.firstOrNull { it.id != compound?.id }

                compound?.let { picks += it; usedIds += it.id }
                isolation?.let { picks += it; usedIds += it.id }
            }

            var trimmed = picks.take(MAX_PER_DAY).toMutableList()
            if (trimmed.size < MIN_PER_DAY) {
                val seed = seedBase + dayIndex * 131L + 9_999L
                val backfillPool = allExercises
                    .filter { ex ->
                        ex.id !in usedIds && groups.any { g -> ex.muscleGroup.equals(g, true) }
                    }
                    .shuffled(Random(seed))
                for (ex in backfillPool) {
                    if (trimmed.size >= MIN_PER_DAY) break
                    trimmed += ex
                    usedIds += ex.id
                }
            }

            BuiltWorkoutDay(
                dayLabel = label,
                exercises = trimmed.map { ex -> scale(ex, level) },
            )
        }
    }

    private fun scale(exercise: Exercise, level: String): PlannedExercise {
        return when (level.lowercase()) {
            "beginner" -> PlannedExercise(
                exercise = exercise,
                sets = 3,
                repsLow = exercise.repsLow,
                repsHigh = exercise.repsLow,
            )
            "advanced" -> PlannedExercise(
                exercise = exercise,
                sets = 4,
                repsLow = exercise.repsHigh,
                repsHigh = exercise.repsHigh,
            )
            else -> PlannedExercise(
                exercise = exercise,
                sets = if (exercise.setsDefault >= 4) 4 else 3,
                repsLow = exercise.repsHigh,
                repsHigh = exercise.repsHigh,
            )
        }
    }
}
