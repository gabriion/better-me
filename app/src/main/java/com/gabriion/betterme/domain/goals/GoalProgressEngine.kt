package com.gabriion.betterme.domain.goals

import com.gabriion.betterme.domain.model.Goal
import com.gabriion.betterme.domain.model.GoalProgressEvent
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

enum class PaceState { AHEAD, ON_TRACK, BEHIND, COMPLETE }

data class GoalSummary(
    val percent: Float,
    val remaining: Double,
    val paceState: PaceState,
    val projectedCompletion: LocalDate?,
    val nextMilestone: Float?
)

/**
 * Pure-Kotlin progress engine. No Android deps.
 */
object GoalProgressEngine {

    private val MILESTONES = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f)

    fun percentComplete(total: Double, target: Double): Float {
        if (target <= 0.0) return 0f
        val raw = (total / target).toFloat()
        return min(1f, max(0f, raw))
    }

    /**
     * Linear projection: given the average daily pace since createdAt,
     * estimate the date the goal will be reached. Returns null if no pace yet.
     */
    fun projectedCompletion(
        total: Double,
        target: Double,
        createdAt: LocalDate,
        today: LocalDate
    ): LocalDate? {
        if (total <= 0.0 || target <= 0.0) return null
        if (total >= target) return today
        val daysElapsed = max(1L, ChronoUnit.DAYS.between(createdAt, today))
        val perDay = total / daysElapsed
        if (perDay <= 0.0) return null
        val remaining = target - total
        val daysNeeded = (remaining / perDay).roundToLong().coerceAtLeast(1L)
        return today.plusDays(daysNeeded)
    }

    fun nextMilestone(percent: Float): Float? =
        MILESTONES.firstOrNull { it > percent + 1e-4f }

    fun summary(goal: Goal, total: Double, today: LocalDate = LocalDate.now()): GoalSummary {
        val percent = percentComplete(total, goal.target)
        val remaining = max(0.0, goal.target - total)
        val projection = projectedCompletion(total, goal.target, goal.createdAt, today)
        val pace = classifyPace(goal, percent, today, projection)
        return GoalSummary(
            percent = percent,
            remaining = remaining,
            paceState = pace,
            projectedCompletion = projection,
            nextMilestone = nextMilestone(percent)
        )
    }

    private fun classifyPace(
        goal: Goal,
        percent: Float,
        today: LocalDate,
        projection: LocalDate?
    ): PaceState {
        if (percent >= 1f) return PaceState.COMPLETE
        val deadline = goal.deadline ?: return PaceState.ON_TRACK
        val totalSpan = max(1L, ChronoUnit.DAYS.between(goal.createdAt, deadline))
        val elapsed = max(0L, ChronoUnit.DAYS.between(goal.createdAt, today))
        val expected = (elapsed.toDouble() / totalSpan.toDouble()).coerceIn(0.0, 1.0).toFloat()
        return when {
            projection != null && projection.isBefore(deadline.minusDays(2)) -> PaceState.AHEAD
            percent + 0.05f < expected -> PaceState.BEHIND
            percent > expected + 0.05f -> PaceState.AHEAD
            else -> PaceState.ON_TRACK
        }
    }

    /** Convenience helper for callers that already have a list of events. */
    fun totalOf(events: List<GoalProgressEvent>): Double = events.sumOf { it.amount }
}
