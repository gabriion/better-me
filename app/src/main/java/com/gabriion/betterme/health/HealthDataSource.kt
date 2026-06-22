package com.gabriion.betterme.health

/**
 * Pluggable source for wellbeing data. Two implementations exist in V1:
 *  - [HealthConnectSource]   reads vendor-agnostic biometrics from the Android
 *                            Health Connect platform (Garmin Connect, Samsung
 *                            Health, Fitbit, Google Fit, etc. all sync into it).
 *  - [AppSignalsSource]      derives signals from the app's own DAOs
 *                            (workouts, weights, food, goals) — no permissions
 *                            needed, works for users without a tracker.
 *
 * The bound [HealthDataSource] (see [com.gabriion.betterme.core.di.AppModule])
 * is the composite that merges both behind the scenes.
 */
interface HealthDataSource {
    /** Vendor-agnostic biometrics. Returns [HealthSnapshot.EMPTY] when unavailable. */
    suspend fun fetchSnapshot(): HealthSnapshot

    /** App-internal signals. Always succeeds, may be all-zero on a fresh install. */
    suspend fun fetchAppSignals(): AppSignals

    /** True if Health Connect is installed and the user has granted at least one read permission. */
    suspend fun isHealthConnectReady(): Boolean = false
}
