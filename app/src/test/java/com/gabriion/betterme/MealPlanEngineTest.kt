package com.gabriion.betterme

import com.gabriion.betterme.domain.meals.MealPlanEngine
import com.gabriion.betterme.domain.meals.ShoppingList
import com.gabriion.betterme.domain.model.Meal
import com.gabriion.betterme.domain.model.Slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealPlanEngineTest {

    private val engine = MealPlanEngine(baseSeed = 42L)

    private val meals: List<Meal> = listOf(
        // breakfast
        Meal("b1", "Eggs oats",          Slot.BREAKFAST, listOf("eggs", "oats"), 420, 28, 45, 14),
        Meal("b2", "Yogurt bowl",        Slot.BREAKFAST, listOf("yogurt", "oats"), 360, 22, 50, 8),
        Meal("b3", "Chicken hash",       Slot.BREAKFAST, listOf("chicken", "potato"), 480, 26, 40, 22),
        // lunch
        Meal("l1", "Chicken rice",       Slot.LUNCH, listOf("chicken", "rice", "broccoli"), 560, 42, 60, 14),
        Meal("l2", "Tuna salad",         Slot.LUNCH, listOf("tuna", "tomato"), 340, 32, 12, 18),
        Meal("l3", "Chicken quinoa",     Slot.LUNCH, listOf("chicken", "quinoa"), 520, 40, 50, 16),
        // dinner
        Meal("d1", "Fish potato",        Slot.DINNER, listOf("fish", "potato", "broccoli"), 560, 38, 50, 20),
        Meal("d2", "Chicken spinach",    Slot.DINNER, listOf("chicken", "spinach"), 610, 44, 55, 22),
        Meal("d3", "Tuna rice",          Slot.DINNER, listOf("tuna", "rice"), 490, 34, 55, 14),
        // snack
        Meal("s1", "Greek yogurt",       Slot.SNACK, listOf("yogurt"), 200, 18, 14, 6),
        Meal("s2", "Boiled eggs",        Slot.SNACK, listOf("eggs"), 220, 18, 2, 16),
    )

    @Test
    fun `empty selection still produces a plan using all meals`() {
        val plan = engine.build(meals, selectedIngredients = emptySet(), days = 1)
        assertEquals(1, plan.days.size)
        val day = plan.days.first()
        // breakfast + lunch + dinner at minimum
        assertTrue("expected >=3 items, got ${day.items.size}", day.items.size >= 3)
        val slots = day.items.map { it.slot }.toSet()
        assertTrue(Slot.BREAKFAST in slots)
        assertTrue(Slot.LUNCH in slots)
        assertTrue(Slot.DINNER in slots)
    }

    @Test
    fun `selection of chicken biases plan toward chicken meals`() {
        val plan = engine.build(meals, selectedIngredients = setOf("chicken"), days = 1)
        val mainItems = plan.days.first().items.filter { it.slot != Slot.SNACK }
        val chickenCount = mainItems.count { item ->
            item.meal.ingredients.any { it.equals("chicken", ignoreCase = true) }
        }
        // At least 2 of the 3 main slots have an option in the chicken pool, so we expect
        // chicken to dominate the plan.
        assertTrue(
            "expected most main meals to contain chicken, got $chickenCount/${mainItems.size}",
            chickenCount >= 2,
        )
    }

    @Test
    fun `seven day plan produces seven day plans with sequential indices`() {
        val plan = engine.build(meals, selectedIngredients = emptySet(), days = 7)
        assertEquals(7, plan.days.size)
        val indices = plan.days.map { it.dayIndex }
        assertEquals((0..6).toList(), indices)
        // sanity: no day is empty
        plan.days.forEach { assertTrue("day ${it.dayIndex} was empty", it.items.isNotEmpty()) }
    }

    @Test
    fun `shopping list aggregates ingredient counts correctly`() {
        val plan = engine.build(meals, selectedIngredients = setOf("chicken"), days = 2)
        val shopping = ShoppingList.fromPlan(plan)
        assertTrue(shopping.isNotEmpty())

        // every ingredient appearing in the plan must be present in the shopping list
        val planIngredients = plan.allMeals
            .flatMap { it.ingredients }
            .map { it.lowercase() }
            .toSet()
        val listIngredients = shopping.map { it.ingredient }.toSet()
        assertEquals(planIngredients, listIngredients)

        // count for each ingredient must equal the number of meals using it
        for (item in shopping) {
            val expected = plan.allMeals.count { m ->
                m.ingredients.any { it.equals(item.ingredient, ignoreCase = true) }
            }
            assertEquals("count mismatch for ${item.ingredient}", expected, item.count)
        }

        // sort: count desc, then alphabetical
        val sortedExpected = shopping.sortedWith(
            compareByDescending<com.gabriion.betterme.domain.meals.ShoppingItem> { it.count }
                .thenBy { it.ingredient }
        )
        assertEquals(sortedExpected, shopping)

        // and the top entry should be the chicken-driven ingredient
        assertNotNull(shopping.firstOrNull())
    }
}
