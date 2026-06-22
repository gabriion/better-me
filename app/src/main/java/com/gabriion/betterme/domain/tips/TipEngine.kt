package com.gabriion.betterme.domain.tips

import com.gabriion.betterme.health.AppSignals
import com.gabriion.betterme.health.HealthSnapshot

/**
 * Rule-based tip engine. Vendor-agnostic — consumes a [HealthSnapshot]
 * (from Health Connect or any other source) plus [AppSignals] derived
 * from the user's own logged data. No LLM at runtime.
 *
 * Outputs an ordered list of [Tip] (by descending priority); the
 * [TipComposer] then resolves each `kind` to a localised message
 * template variant.
 */
class TipEngine {

    fun generate(
        snapshot: HealthSnapshot?,
        signals: AppSignals = AppSignals.EMPTY,
        rhr7dAvg: Int? = null
    ): List<Tip> {
        val out = mutableListOf<Tip>()

        // -- Biometric signals (Health Connect)
        snapshot?.sleepHours?.let { if (it < 6.0) out += Tip("sleep", priority = 10) }
        snapshot?.restingHeartRate?.let { rhr ->
            rhr7dAvg?.let { avg -> if (rhr - avg >= 5) out += Tip("rhr", priority = 9) }
        }
        snapshot?.lastActivityHrAvg?.let { hr ->
            if (hr >= 160) out += Tip("activity_hr", priority = 8)
        }
        snapshot?.stressAvg?.let { if (it >= 65) out += Tip("stress", priority = 7) }
        snapshot?.hrv?.let { if (it < 35) out += Tip("hrv", priority = 7) }
        snapshot?.steps?.let { if (it < 5000) out += Tip("steps", priority = 6) }

        // -- App-internal signals (always available)
        signals.daysSinceLastWorkout?.let { if (it >= 4) out += Tip("workout_gap", priority = 8) }
        if (signals.workoutStreakDays >= 3) out += Tip("streak_celebration", priority = 5)
        signals.weightTrendKgPerWeek?.let { trend ->
            if (trend <= -0.3) out += Tip("weight_trend_down", priority = 4)
            else if (trend >= 0.3) out += Tip("weight_trend_up", priority = 4)
        }
        signals.daysSinceLastWeighIn?.let { if (it >= 14) out += Tip("weigh_in_nudge", priority = 3) }

        return if (out.isEmpty()) defaults() else out.sortedByDescending { it.priority }
    }

    private fun defaults(): List<Tip> = listOf(
        Tip("mindfulness", priority = 4),
        Tip("hydration", priority = 5)
    )
}

/**
 * A surfaced tip — only the `kind` is meaningful here. The [TipComposer]
 * looks up the localised message variant for the day. [Snapshot] fields
 * may also be passed to it for interpolation (e.g. `{hours}`).
 */
data class Tip(
    val kind: String,
    val priority: Int = 1
)

// Kept for source compatibility with existing tests / callers; the second
// constructor parameter is no longer used directly (it lives in the message
// template now), but we accept it transparently to avoid breaking imports.
@Suppress("unused")
fun Tip(kind: String, message: String, priority: Int = 1): Tip = Tip(kind, priority)
