package com.gabriion.betterme

import com.gabriion.betterme.core.db.WeightEntity
import com.gabriion.betterme.core.db.WorkoutSetEntity
import com.gabriion.betterme.domain.evolution.HighlightsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HighlightsEngineTest {

    private val today: LocalDate = LocalDate.of(2026, 6, 1)

    private fun setOn(date: LocalDate, id: Long = 0): WorkoutSetEntity {
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return WorkoutSetEntity(
            id = id,
            date = millis,
            exerciseId = "ex",
            exerciseName = "Push-ups",
            setIndex = 0,
            reps = 10,
            weightKg = null
        )
    }

    @Test
    fun `weight delta categorises loss, gain and steady correctly`() {
        val baselineDate = today.minusDays(30)

        val lost = listOf(
            WeightEntity(id = 1, date = baselineDate, kg = 80.0),
            WeightEntity(id = 2, date = today, kg = 78.8)
        )
        val lostHighlight = HighlightsEngine.weightDeltaThisMonth(lost, today)!!
        assertTrue(lostHighlight.body.startsWith("Lost"))
        assertTrue(lostHighlight.body.contains("1.2"))

        val gained = listOf(
            WeightEntity(id = 1, date = baselineDate, kg = 80.0),
            WeightEntity(id = 2, date = today, kg = 80.4)
        )
        assertTrue(HighlightsEngine.weightDeltaThisMonth(gained, today)!!.body.startsWith("Gained"))

        val steady = listOf(
            WeightEntity(id = 1, date = baselineDate, kg = 80.0),
            WeightEntity(id = 2, date = today, kg = 80.1)
        )
        assertEquals("Steady this month", HighlightsEngine.weightDeltaThisMonth(steady, today)!!.body)
    }

    @Test
    fun `workout streak counts consecutive days including today`() {
        val sets = listOf(
            setOn(today, id = 1),
            setOn(today.minusDays(1), id = 2),
            setOn(today.minusDays(2), id = 3),
            // gap on day 3
            setOn(today.minusDays(4), id = 4)
        )
        val h = HighlightsEngine.workoutStreak(sets, today)!!
        assertEquals("Workout streak", h.title)
        assertTrue(h.body.contains("3"))
    }

    @Test
    fun `empty data still yields a welcome fallback`() {
        val out = HighlightsEngine.compute(emptyList(), emptyList(), today)
        assertEquals(1, out.size)
        assertEquals("Welcome back", out.first().title)
    }

    @Test
    fun `partial data — only weights — produces weight highlight without fallback`() {
        val weights = listOf(
            WeightEntity(id = 1, date = today.minusDays(30), kg = 75.0),
            WeightEntity(id = 2, date = today, kg = 74.0)
        )
        val out = HighlightsEngine.compute(weights, emptyList(), today)
        assertEquals(1, out.size)
        assertEquals("scale", out.first().icon)
        assertTrue(out.none { it.title == "Welcome back" })
    }
}
