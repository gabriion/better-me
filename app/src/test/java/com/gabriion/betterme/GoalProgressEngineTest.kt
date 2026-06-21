package com.gabriion.betterme

import com.gabriion.betterme.domain.goals.GoalProgressEngine
import com.gabriion.betterme.domain.goals.PaceState
import com.gabriion.betterme.domain.model.Goal
import com.gabriion.betterme.domain.model.GoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalProgressEngineTest {

    @Test
    fun `percentComplete clamps to 0 and 1`() {
        assertEquals(0f, GoalProgressEngine.percentComplete(-5.0, 10.0), 0.0001f)
        assertEquals(0.5f, GoalProgressEngine.percentComplete(5.0, 10.0), 0.0001f)
        assertEquals(1f, GoalProgressEngine.percentComplete(20.0, 10.0), 0.0001f)
        assertEquals(0f, GoalProgressEngine.percentComplete(5.0, 0.0), 0.0001f)
    }

    @Test
    fun `projectedCompletion estimates remaining days linearly`() {
        val created = LocalDate.of(2026, 1, 1)
        val today = LocalDate.of(2026, 1, 11) // 10 days elapsed
        // total=20, perDay=2, remaining=80 => 40 days from today
        val projection = GoalProgressEngine.projectedCompletion(
            total = 20.0, target = 100.0, createdAt = created, today = today
        )
        assertNotNull(projection)
        assertEquals(today.plusDays(40), projection)

        // No progress => null
        assertNull(
            GoalProgressEngine.projectedCompletion(0.0, 100.0, created, today)
        )

        // Already complete => today
        assertEquals(
            today,
            GoalProgressEngine.projectedCompletion(150.0, 100.0, created, today)
        )
    }

    @Test
    fun `nextMilestone returns next un-hit quarter`() {
        assertEquals(0.25f, GoalProgressEngine.nextMilestone(0f))
        assertEquals(0.5f, GoalProgressEngine.nextMilestone(0.3f))
        assertEquals(0.75f, GoalProgressEngine.nextMilestone(0.5f))
        assertEquals(1f, GoalProgressEngine.nextMilestone(0.8f))
        assertNull(GoalProgressEngine.nextMilestone(1f))
    }

    @Test
    fun `summary classifies pace state correctly`() {
        val created = LocalDate.of(2026, 1, 1)
        val deadline = LocalDate.of(2026, 12, 31)
        val today = LocalDate.of(2026, 7, 1) // ~half year elapsed

        val goal = Goal(
            id = 1L,
            title = "Read 100 books",
            type = GoalType.COUNT,
            target = 100.0,
            unit = "books",
            deadline = deadline,
            createdAt = created
        )

        // Behind: only 10% done when ~50% time elapsed
        val behind = GoalProgressEngine.summary(goal, total = 10.0, today = today)
        assertEquals(PaceState.BEHIND, behind.paceState)
        assertTrue(behind.remaining == 90.0)

        // Complete
        val done = GoalProgressEngine.summary(goal, total = 100.0, today = today)
        assertEquals(PaceState.COMPLETE, done.paceState)
        assertEquals(0.0, done.remaining, 0.0001)

        // Ahead: 80% done at midyear
        val ahead = GoalProgressEngine.summary(goal, total = 80.0, today = today)
        assertEquals(PaceState.AHEAD, ahead.paceState)

        // No deadline => ON_TRACK by default
        val noDeadline = goal.copy(deadline = null)
        val onTrack = GoalProgressEngine.summary(noDeadline, total = 5.0, today = today)
        assertEquals(PaceState.ON_TRACK, onTrack.paceState)
    }
}
