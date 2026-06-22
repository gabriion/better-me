package com.gabriion.betterme

import com.gabriion.betterme.domain.gym.PlanBuilder
import com.gabriion.betterme.domain.model.Exercise
import com.gabriion.betterme.domain.model.WorkoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanBuilderTest {

    /** Mini catalogue: every muscle group × {barbell compound, dumbbell, cable, machine, bodyweight}. */
    private val catalogue: List<Exercise> = buildList {
        val groups = listOf("chest", "back", "shoulders", "legs", "arms", "core")
        val equipments = listOf("barbell", "dumbbell", "cable", "machine", "bodyweight")
        for (g in groups) {
            for (e in equipments) {
                val (low, high, sets) = when {
                    g == "core" -> Triple(10, 25, 3)
                    e == "bodyweight" -> Triple(8, 20, 3)
                    e == "barbell" -> Triple(6, 12, 4)
                    e == "dumbbell" -> Triple(6, 12, 3)
                    else -> Triple(8, 15, 3)
                }
                add(
                    Exercise(
                        id = "${g}_${e}",
                        name = "$e $g",
                        muscleGroup = g,
                        equipment = e,
                        repsLow = low,
                        repsHigh = high,
                        setsDefault = sets,
                        lottie = "ex/${g}_${e}.json",
                    )
                )
            }
        }
    }

    private val ppl = WorkoutTemplate("ppl_3", "PPL", listOf("push", "pull", "legs"))

    @Test
    fun `PPL produces 3 days`() {
        val plan = PlanBuilder.build(ppl, catalogue, level = "intermediate")
        assertEquals(3, plan.size)
        assertEquals(listOf("push", "pull", "legs"), plan.map { it.dayLabel })
        plan.forEach { day ->
            assertTrue(
                "day ${day.dayLabel} should have at least 4 exercises, got ${day.exercises.size}",
                day.exercises.size >= 4,
            )
            assertTrue(
                "day ${day.dayLabel} should have at most 6 exercises, got ${day.exercises.size}",
                day.exercises.size <= 6,
            )
        }
    }

    @Test
    fun `level scales reps and sets correctly`() {
        val beginner = PlanBuilder.build(ppl, catalogue, level = "beginner")
        val intermediate = PlanBuilder.build(ppl, catalogue, level = "intermediate")
        val advanced = PlanBuilder.build(ppl, catalogue, level = "advanced")

        beginner.flatMap { it.exercises }.forEach { p ->
            assertEquals(3, p.sets)
            assertEquals(p.exercise.repsLow, p.repsLow)
            assertEquals(p.exercise.repsLow, p.repsHigh)
        }
        intermediate.flatMap { it.exercises }.forEach { p ->
            val expectedSets = if (p.exercise.setsDefault >= 4) 4 else 3
            assertEquals(expectedSets, p.sets)
            assertEquals(p.exercise.repsHigh, p.repsLow)
            assertEquals(p.exercise.repsHigh, p.repsHigh)
        }
        advanced.flatMap { it.exercises }.forEach { p ->
            assertEquals(4, p.sets)
            assertEquals(p.exercise.repsHigh, p.repsLow)
            assertEquals(p.exercise.repsHigh, p.repsHigh)
        }
    }

    @Test
    fun `day labels map to allowed muscle groups`() {
        val plan = PlanBuilder.build(ppl, catalogue, level = "intermediate")
        for (day in plan) {
            val allowed = PlanBuilder.DAY_LABEL_TO_GROUPS[day.dayLabel]!!.toSet()
            for (planned in day.exercises) {
                assertTrue(
                    "day ${day.dayLabel} should not pick ${planned.exercise.muscleGroup}",
                    planned.exercise.muscleGroup in allowed,
                )
            }
        }

        // full-body picks from every group
        val full = WorkoutTemplate("fb_1", "Full", listOf("full"))
        val day = PlanBuilder.build(full, catalogue, level = "intermediate").first()
        val allowedFull = PlanBuilder.DAY_LABEL_TO_GROUPS["full"]!!.toSet()
        for (planned in day.exercises) {
            assertTrue(planned.exercise.muscleGroup in allowedFull)
        }
        assertNotNull(day.exercises.firstOrNull())
    }

    @Test
    fun `deterministic seed produces same output twice`() {
        val a = PlanBuilder.build(ppl, catalogue, level = "intermediate")
        val b = PlanBuilder.build(ppl, catalogue, level = "intermediate")
        assertEquals(a.size, b.size)
        for (i in a.indices) {
            assertEquals(a[i].dayLabel, b[i].dayLabel)
            assertEquals(
                a[i].exercises.map { it.exercise.id },
                b[i].exercises.map { it.exercise.id },
            )
        }
    }

    @Test
    fun `different templates produce different exercise sequences`() {
        val ul = WorkoutTemplate("ul_2", "UL", listOf("upper", "lower"))
        val pplIds = PlanBuilder.build(ppl, catalogue, "intermediate")
            .flatMap { it.exercises }.map { it.exercise.id }
        val ulIds = PlanBuilder.build(ul, catalogue, "intermediate")
            .flatMap { it.exercises }.map { it.exercise.id }
        assertTrue(pplIds.isNotEmpty())
        assertTrue(ulIds.isNotEmpty())
        // Not asserting strict inequality of contents — just that templates run independently
        // and at least one of them includes a group the other does not (core is lower-only).
        val ulHasCore = ulIds.any { it.startsWith("core_") }
        val pplHasCore = pplIds.any { it.startsWith("core_") }
        assertTrue("upper/lower should include a core exercise", ulHasCore)
        assertTrue("push/pull/legs should not include core", !pplHasCore)
    }
}
