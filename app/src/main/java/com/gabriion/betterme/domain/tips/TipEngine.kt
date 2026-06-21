package com.gabriion.betterme.domain.tips

import com.gabriion.betterme.garmin.GarminSnapshot

/**
 * Rule-based tip engine. 8 signals -> ranked tips. No LLM at runtime.
 */
class TipEngine {

    fun generate(snapshot: GarminSnapshot?, rhr7dAvg: Int? = null): List<Tip> {
        if (snapshot == null) return defaults()
        val out = mutableListOf<Tip>()

        snapshot.sleepHours?.let { if (it < 6.0) out += Tip("sleep", "You slept ${"%.1f".format(it)}h. Consider a 20-min power nap after lunch.", priority = 10) }
        snapshot.restingHeartRate?.let { rhr ->
            rhr7dAvg?.let { avg -> if (rhr - avg >= 5) out += Tip("rhr", "Your resting heart rate is up vs your 7-day average. Try 10 minutes of slow breathing.", priority = 9) }
        }
        snapshot.lastActivityHrAvg?.let { hr ->
            if (hr >= 160) out += Tip("activity_hr", "HR was high during your ${snapshot.lastActivityType ?: "last activity"}. Add a recovery day before your next training.", priority = 8)
        }
        snapshot.steps?.let { if (it < 5000) out += Tip("steps", "Only $it steps so far. A 15-minute walk would close the gap.", priority = 6) }
        snapshot.stressAvg?.let { if (it >= 65) out += Tip("stress", "Stress trending high today. Try a 5-minute guided breath.", priority = 7) }
        snapshot.hrv?.let { if (it < 35) out += Tip("hrv", "HRV is suppressed. Prioritise sleep tonight — skip late caffeine.", priority = 7) }

        return if (out.isEmpty()) defaults() else out.sortedByDescending { it.priority }
    }

    private fun defaults(): List<Tip> = listOf(
        Tip("default", "Two minutes of stillness sets a kinder tone for the day."),
        Tip("default", "Drink a glass of water. Small kindness, big effect.")
    )
}

data class Tip(
    val kind: String,
    val message: String,
    val priority: Int = 1
)
