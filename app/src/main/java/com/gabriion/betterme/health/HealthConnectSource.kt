package com.gabriion.betterme.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads biometrics from the Android Health Connect platform.
 *
 * Health Connect is a vendor-agnostic on-device API that aggregates data from
 * Garmin Connect, Samsung Health, Fitbit, Google Fit, Whoop, Mi Fit and other
 * fitness apps the user installs. It needs no business approval — the user
 * grants per-record-type permissions in the system Health Connect UI.
 *
 * Permission flow lives in the UI layer; this class only reads. When permissions
 * are missing or Health Connect is not installed, every read returns null and
 * the [fetchSnapshot] call yields [HealthSnapshot.EMPTY].
 */
@Singleton
class HealthConnectSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client: HealthConnectClient? by lazy {
        runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    val requiredReadPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    )

    suspend fun isReady(): Boolean {
        val c = client ?: return false
        return runCatching {
            c.permissionController.getGrantedPermissions().any { it in requiredReadPermissions }
        }.getOrDefault(false)
    }

    suspend fun fetchSnapshot(): HealthSnapshot {
        val c = client ?: return HealthSnapshot.EMPTY
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfDay = today.atStartOfDay(zone).toInstant()
        val now = java.time.Instant.now()
        val startOfYesterday = today.minusDays(1).atStartOfDay(zone).toInstant()
        val sleepWindow = TimeRangeFilter.between(
            today.minusDays(1).atTime(LocalTime.of(18, 0)).atZone(zone).toInstant(),
            today.atTime(LocalTime.of(12, 0)).atZone(zone).toInstant()
        )
        val todayWindow = TimeRangeFilter.between(startOfDay, now)
        val recentWindow = TimeRangeFilter.between(startOfYesterday, now)

        return runCatching {
            val sleepRecords = c.readRecords(ReadRecordsRequest(SleepSessionRecord::class, sleepWindow)).records
            val totalSleep = sleepRecords.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
            val sleepHours = if (totalSleep > 0) totalSleep / 60.0 else null

            val stepsRecords = c.readRecords(ReadRecordsRequest(StepsRecord::class, todayWindow)).records
            val steps = stepsRecords.sumOf { it.count }.toInt().takeIf { it > 0 }

            val rhrRecords = c.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, recentWindow)).records
            val rhr = rhrRecords.lastOrNull()?.beatsPerMinute?.toInt()

            val hrvRecords = c.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, recentWindow)).records
            val hrv = hrvRecords.lastOrNull()?.heartRateVariabilityMillis?.toInt()

            HealthSnapshot(
                sleepHours = sleepHours,
                restingHeartRate = rhr,
                hrv = hrv,
                steps = steps,
                activeMinutes = null,
                lastActivityHrAvg = null,
                lastActivityType = null
            )
        }.getOrDefault(HealthSnapshot.EMPTY)
    }
}
