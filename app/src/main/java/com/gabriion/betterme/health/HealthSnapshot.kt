package com.gabriion.betterme.health

/**
 * Vendor-agnostic biometric snapshot for "today".
 *
 * Populated by [HealthDataSource] implementations. All fields nullable —
 * the data source may be Health Connect (vendor-agnostic Android API), a
 * future wearable SDK, or simply absent (no tracker connected at all).
 */
data class HealthSnapshot(
    val sleepHours: Double? = null,
    val restingHeartRate: Int? = null,
    val hrv: Int? = null,
    val stressAvg: Int? = null,
    val steps: Int? = null,
    val activeMinutes: Int? = null,
    val lastActivityHrAvg: Int? = null,
    val lastActivityType: String? = null
) {
    val isEmpty: Boolean
        get() = sleepHours == null && restingHeartRate == null && hrv == null &&
                stressAvg == null && steps == null && activeMinutes == null &&
                lastActivityHrAvg == null

    companion object {
        val EMPTY = HealthSnapshot()
    }
}

/**
 * Signals derived from the app's own data — workout streaks, weight trend,
 * calorie consistency, goal pacing. Always available, no permissions needed.
 */
data class AppSignals(
    val daysSinceLastWorkout: Int? = null,
    val workoutStreakDays: Int = 0,
    val weightTrendKgPerWeek: Double? = null,
    val daysSinceLastWeighIn: Int? = null,
    val caloriesLoggedToday: Boolean = false,
    val goalsBehindPaceCount: Int = 0
) {
    companion object {
        val EMPTY = AppSignals()
    }
}
