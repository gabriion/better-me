package com.gabriion.betterme

import com.gabriion.betterme.core.db.FoodCacheEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-Kotlin unit tests for macro scaling (per-100g × grams / 100).
 * Mirrors the logic in CaloriesRepository.logEntry without pulling in Android deps.
 */
class CaloriesScalingTest {

    private fun cache(
        kcal: Double = 250.0,
        protein: Double = 10.0,
        carbs: Double = 30.0,
        fat: Double = 8.0
    ) = FoodCacheEntity(
        foodId = "x",
        name = "Sample",
        brand = null,
        kcalPer100g = kcal,
        proteinPer100g = protein,
        carbsPer100g = carbs,
        fatPer100g = fat
    )

    private data class Scaled(val kcal: Double, val p: Double, val c: Double, val f: Double)

    private fun scale(c: FoodCacheEntity, grams: Double): Scaled {
        val factor = grams / 100.0
        return Scaled(
            kcal = c.kcalPer100g * factor,
            p = c.proteinPer100g * factor,
            c = c.carbsPer100g * factor,
            f = c.fatPer100g * factor
        )
    }

    @Test
    fun `100g returns per-100g values`() {
        val r = scale(cache(), 100.0)
        assertEquals(250.0, r.kcal, 0.0001)
        assertEquals(10.0, r.p, 0.0001)
        assertEquals(30.0, r.c, 0.0001)
        assertEquals(8.0, r.f, 0.0001)
    }

    @Test
    fun `150g scales by 1_5`() {
        val r = scale(cache(), 150.0)
        assertEquals(375.0, r.kcal, 0.0001)
        assertEquals(15.0, r.p, 0.0001)
        assertEquals(45.0, r.c, 0.0001)
        assertEquals(12.0, r.f, 0.0001)
    }

    @Test
    fun `0g returns zero macros`() {
        val r = scale(cache(), 0.0)
        assertEquals(0.0, r.kcal, 0.0001)
        assertEquals(0.0, r.p, 0.0001)
        assertEquals(0.0, r.c, 0.0001)
        assertEquals(0.0, r.f, 0.0001)
    }

    @Test
    fun `very large grams scales linearly`() {
        val r = scale(cache(kcal = 500.0, protein = 20.0, carbs = 40.0, fat = 25.0), 10_000.0)
        // factor = 100
        assertEquals(50_000.0, r.kcal, 0.0001)
        assertEquals(2_000.0, r.p, 0.0001)
        assertEquals(4_000.0, r.c, 0.0001)
        assertEquals(2_500.0, r.f, 0.0001)
    }

    @Test
    fun `fractional grams scales correctly`() {
        val r = scale(cache(kcal = 200.0), 37.5)
        // factor = 0.375
        assertEquals(75.0, r.kcal, 0.0001)
    }

    @Test
    fun `zero per-100g values stay zero regardless of grams`() {
        val r = scale(cache(kcal = 0.0, protein = 0.0, carbs = 0.0, fat = 0.0), 250.0)
        assertEquals(0.0, r.kcal, 0.0001)
        assertEquals(0.0, r.p, 0.0001)
        assertEquals(0.0, r.c, 0.0001)
        assertEquals(0.0, r.f, 0.0001)
    }
}
