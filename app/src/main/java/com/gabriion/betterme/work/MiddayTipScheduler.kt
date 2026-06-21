package com.gabriion.betterme.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MiddayTipScheduler @Inject constructor() {

    fun schedule(context: Context, hour: Int = 13, minute: Int = 0) {
        val initialDelay = computeInitialDelayMillis(hour, minute)
        val request = PeriodicWorkRequestBuilder<MiddayTipWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }

    private fun computeInitialDelayMillis(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val nowMs = now.atZone(zone).toInstant().toEpochMilli()
        val targetMs = target.atZone(zone).toInstant().toEpochMilli()
        return (targetMs - nowMs).coerceAtLeast(0L)
    }

    companion object {
        private const val UNIQUE_NAME = "midday_tip"
    }
}
