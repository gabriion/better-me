package com.gabriion.betterme.health

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [HealthDataSource] composite. Reads biometrics from Health Connect
 * (when installed + authorised) and signals from the user's own logged data.
 */
@Singleton
class CombinedHealthSource @Inject constructor(
    private val healthConnect: HealthConnectSource,
    private val appSignals: AppSignalsSource
) : HealthDataSource {

    override suspend fun fetchSnapshot(): HealthSnapshot = healthConnect.fetchSnapshot()

    override suspend fun fetchAppSignals(): AppSignals = appSignals.fetch()

    override suspend fun isHealthConnectReady(): Boolean = healthConnect.isReady()
}
