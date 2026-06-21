package com.gabriion.betterme.garmin

/**
 * Garmin Connect API client placeholder.
 *
 * V1 will implement OAuth1.0a flow against Garmin Health API once developer
 * credentials are issued (~1 week approval).
 *
 * Endpoints we'll need:
 *  - daily summaries (steps, RHR, HRV, stress)
 *  - sleep summaries
 *  - activities (HR, duration, type)
 *
 * See docs/garmin-setup.md
 */
interface GarminClient {
    suspend fun fetchDailySnapshot(): GarminSnapshot?
}

data class GarminSnapshot(
    val sleepHours: Double?,
    val restingHeartRate: Int?,
    val hrv: Int?,
    val stressAvg: Int?,
    val steps: Int?,
    val lastActivityHrAvg: Int?,
    val lastActivityType: String?
)

class GarminClientStub : GarminClient {
    override suspend fun fetchDailySnapshot(): GarminSnapshot? = null
}
