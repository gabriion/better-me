package com.gabriion.betterme.domain.tips

import com.gabriion.betterme.garmin.GarminSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TipEngineTest {

    private val engine = TipEngine()

    @Test
    fun `null snapshot returns defaults`() {
        val tips = engine.generate(null)
        assertTrue(tips.isNotEmpty())
        assertTrue(tips.all { it.kind == "default" })
    }

    @Test
    fun `poor sleep produces sleep tip with highest priority`() {
        val tips = engine.generate(
            GarminSnapshot(
                sleepHours = 4.5,
                restingHeartRate = 60,
                hrv = 50,
                stressAvg = 30,
                steps = 8000,
                lastActivityHrAvg = 120,
                lastActivityType = "Run"
            )
        )
        assertEquals("sleep", tips.first().kind)
    }

    @Test
    fun `elevated RHR vs avg triggers rhr tip`() {
        val tips = engine.generate(
            GarminSnapshot(
                sleepHours = 8.0, restingHeartRate = 70, hrv = 60,
                stressAvg = 30, steps = 8000,
                lastActivityHrAvg = 100, lastActivityType = "Walk"
            ),
            rhr7dAvg = 62
        )
        assertTrue(tips.any { it.kind == "rhr" })
    }

    @Test
    fun `low steps after high activity HR still surfaces activity tip first`() {
        val tips = engine.generate(
            GarminSnapshot(
                sleepHours = 8.0, restingHeartRate = 60, hrv = 60,
                stressAvg = 30, steps = 3000,
                lastActivityHrAvg = 175, lastActivityType = "Swimming"
            )
        )
        val priorities = tips.map { it.kind }
        // activity_hr (priority 8) ranks above steps (priority 6)
        assertTrue(priorities.indexOf("activity_hr") < priorities.indexOf("steps"))
    }
}
