package com.gabriion.betterme.domain.tips

import com.gabriion.betterme.health.AppSignals
import com.gabriion.betterme.health.HealthSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TipEngineTest {

    private val engine = TipEngine()

    @Test
    fun `null snapshot and empty signals returns default mindfulness pack`() {
        val tips = engine.generate(snapshot = null, signals = AppSignals.EMPTY)
        val kinds = tips.map { it.kind }.toSet()
        assertEquals(setOf("mindfulness", "hydration"), kinds)
    }

    @Test
    fun `poor sleep produces sleep tip with highest priority`() {
        val tips = engine.generate(
            snapshot = HealthSnapshot(
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
            snapshot = HealthSnapshot(
                sleepHours = 8.0, restingHeartRate = 70, hrv = 60,
                stressAvg = 30, steps = 8000,
                lastActivityHrAvg = 100, lastActivityType = "Walk"
            ),
            rhr7dAvg = 62
        )
        assertTrue(tips.any { it.kind == "rhr" })
    }

    @Test
    fun `app signal workout gap fires when no workout in 4+ days`() {
        val tips = engine.generate(
            snapshot = null,
            signals = AppSignals(daysSinceLastWorkout = 5)
        )
        assertTrue("expected workout_gap", tips.any { it.kind == "workout_gap" })
    }

    @Test
    fun `workout streak celebration fires at 3+ days`() {
        val tips = engine.generate(
            snapshot = null,
            signals = AppSignals(workoutStreakDays = 4)
        )
        assertTrue("expected streak_celebration", tips.any { it.kind == "streak_celebration" })
    }

    @Test
    fun `weight trend down surfaces only when meaningful`() {
        val downTips = engine.generate(
            snapshot = null,
            signals = AppSignals(weightTrendKgPerWeek = -0.5)
        )
        assertTrue(downTips.any { it.kind == "weight_trend_down" })

        val noTrend = engine.generate(
            snapshot = null,
            signals = AppSignals(weightTrendKgPerWeek = -0.1)
        )
        assertFalse(noTrend.any { it.kind == "weight_trend_down" })
    }

    @Test
    fun `priorities order biometric over app signals when both apply`() {
        val tips = engine.generate(
            snapshot = HealthSnapshot(sleepHours = 4.0),                  // priority 10
            signals = AppSignals(daysSinceLastWorkout = 6)                // priority 8
        )
        assertEquals("sleep", tips.first().kind)
    }
}
