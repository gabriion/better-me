package com.gabriion.betterme

import com.gabriion.betterme.domain.tips.TipComposer
import com.gabriion.betterme.domain.tips.TipTemplate
import com.gabriion.betterme.garmin.GarminSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TipComposerTest {

    private val templates = listOf(
        TipTemplate("sleep", 10, listOf(
            "You slept {hours}h. A 20-min power nap after lunch could help.",
            "Short night ({hours}h) — be gentle on intensity today.",
            "{hours}h of sleep last night. Hydrate early."
        )),
        TipTemplate("rhr", 9, listOf("RHR is up.")),
        TipTemplate("activity_hr", 8, listOf(
            "HR was high during your {activity}. Recovery day before your next training."
        )),
        TipTemplate("stress", 7, listOf("Stress trending high.")),
        TipTemplate("hrv", 7, listOf("HRV is suppressed.")),
        TipTemplate("steps", 6, listOf("Only {steps} steps so far.")),
        TipTemplate("hydration", 5, listOf(
            "Drink a glass of water. Small kindness, big effect.",
            "Refill the bottle."
        )),
        TipTemplate("mindfulness", 4, listOf(
            "Two minutes of stillness sets a kinder tone for the day.",
            "Three slow breaths."
        ))
    )

    private val composer = TipComposer(templates)

    @Test
    fun `null snapshot returns mindfulness and hydration defaults`() {
        val tips = composer.compose(snapshot = null, rhr7dAvg = null, today = LocalDate.of(2026, 4, 1))
        val kinds = tips.map { it.kind }
        assertEquals(setOf("hydration", "mindfulness"), kinds.toSet())
        assertEquals(2, tips.size)
    }

    @Test
    fun `interpolation substitutes hours steps and activity correctly`() {
        val snapshot = GarminSnapshot(
            sleepHours = 5.0,
            restingHeartRate = 60,
            hrv = 60,
            stressAvg = 30,
            steps = 3000,
            lastActivityHrAvg = 175,
            lastActivityType = "Swimming"
        )
        val tips = composer.compose(snapshot = snapshot, rhr7dAvg = null, today = LocalDate.of(2026, 4, 1))
        val byKind = tips.associateBy { it.kind }

        val sleep = byKind.getValue("sleep").message
        assertTrue("expected hours substitution, got: $sleep", sleep.contains("5.0"))
        assertTrue("placeholder must be gone, got: $sleep", !sleep.contains("{hours}"))

        val steps = byKind.getValue("steps").message
        assertTrue("expected steps substitution, got: $steps", steps.contains("3000"))

        val activity = byKind.getValue("activity_hr").message
        assertTrue("expected activity substitution, got: $activity", activity.contains("Swimming"))
    }

    @Test
    fun `variant rotates deterministically by date`() {
        // hydration has 2 message variants — pick two adjacent epoch days.
        val day0 = LocalDate.of(2026, 4, 1)
        val day1 = day0.plusDays(1)
        val msg0 = composer.compose(null, null, day0).first { it.kind == "hydration" }.message
        val msg1 = composer.compose(null, null, day1).first { it.kind == "hydration" }.message
        assertNotEquals("variants should rotate across days", msg0, msg1)

        // And the rotation is deterministic — same day yields same message.
        val msg0Again = composer.compose(null, null, day0).first { it.kind == "hydration" }.message
        assertEquals(msg0, msg0Again)
    }

    @Test
    fun `priority order preserved`() {
        val snapshot = GarminSnapshot(
            sleepHours = 4.5,             // sleep priority 10
            restingHeartRate = 60,
            hrv = 30,                     // hrv priority 7
            stressAvg = 70,               // stress priority 7
            steps = 2000,                 // steps priority 6
            lastActivityHrAvg = 170,      // activity_hr priority 8
            lastActivityType = "Run"
        )
        val tips = composer.compose(snapshot, rhr7dAvg = null, today = LocalDate.of(2026, 4, 1))
        val priorities = tips.map { it.priority }
        assertEquals("must be descending by priority", priorities.sortedDescending(), priorities)
        assertEquals("sleep", tips.first().kind)
    }
}
