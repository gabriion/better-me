package com.gabriion.betterme.domain.meals

import com.gabriion.betterme.domain.model.Meal
import com.gabriion.betterme.domain.model.Slot
import kotlin.math.abs
import kotlin.random.Random

data class PlanItem(val slot: Slot, val meal: Meal)
data class DayPlan(val dayIndex: Int, val items: List<PlanItem>) {
    val totalKcal: Int get() = items.sumOf { it.meal.kcal }
}
data class MealPlan(val days: List<DayPlan>) {
    val allMeals: List<Meal> get() = days.flatMap { d -> d.items.map { it.meal } }
}

/**
 * Pure-Kotlin meal plan engine. No Android deps so it's trivially unit-testable.
 *
 * Algorithm per day:
 *  1. Pre-filter candidate pool: meals whose ingredient list intersects [selectedIngredients]
 *     (or the full pool when the selection is empty).
 *  2. For breakfast/lunch/dinner, pick the candidate whose kcal best moves the running total
 *     toward the per-slot kcal share (target / 3), with deterministic seeded shuffling.
 *  3. If the day still sits below `targetKcalPerDay - 200`, append the snack closest to the gap.
 *  4. Never repeat the same meal as the previous day in the same slot.
 */
class MealPlanEngine(private val baseSeed: Long = 0L) {

    fun build(
        allMeals: List<Meal>,
        selectedIngredients: Set<String>,
        days: Int,
        targetKcalPerDay: Int = 2000,
    ): MealPlan {
        if (allMeals.isEmpty() || days <= 0) return MealPlan(emptyList())

        val normalisedSelection = selectedIngredients.map { it.trim().lowercase() }.toSet()
        val pool = if (normalisedSelection.isEmpty()) {
            allMeals
        } else {
            allMeals.filter { meal ->
                meal.ingredients.any { it.trim().lowercase() in normalisedSelection }
            }.ifEmpty { allMeals }
        }

        val byMainSlot: Map<Slot, List<Meal>> = listOf(Slot.BREAKFAST, Slot.LUNCH, Slot.DINNER)
            .associateWith { slot -> pool.filter { it.slot == slot }.ifEmpty { allMeals.filter { it.slot == slot } } }
        val snackPool = pool.filter { it.slot == Slot.SNACK }
            .ifEmpty { allMeals.filter { it.slot == Slot.SNACK } }

        val mainSlots = listOf(Slot.BREAKFAST, Slot.LUNCH, Slot.DINNER)
        val perSlotTarget = targetKcalPerDay / 3
        val previousPerSlot = mutableMapOf<Slot, String?>()

        val dayPlans = (0 until days).map { dayIndex ->
            val items = mutableListOf<PlanItem>()
            var running = 0
            for ((slotPos, slot) in mainSlots.withIndex()) {
                val candidates = byMainSlot[slot].orEmpty()
                if (candidates.isEmpty()) continue
                val seeded = candidates.shuffled(Random(baseSeed + dayIndex * 31L + slotPos))
                val previousId = previousPerSlot[slot]
                val desired = (slotPos + 1) * perSlotTarget - running
                val pick = seeded
                    .sortedBy { abs(it.kcal - desired) }
                    .firstOrNull { it.id != previousId }
                    ?: seeded.first()
                items += PlanItem(slot, pick)
                running += pick.kcal
                previousPerSlot[slot] = pick.id
            }

            val deficit = targetKcalPerDay - running
            if (deficit > 200 && snackPool.isNotEmpty()) {
                val seeded = snackPool.shuffled(Random(baseSeed + dayIndex * 31L + 99))
                val previousId = previousPerSlot[Slot.SNACK]
                val snack = seeded
                    .sortedBy { abs(it.kcal - deficit) }
                    .firstOrNull { it.id != previousId }
                    ?: seeded.first()
                items += PlanItem(Slot.SNACK, snack)
                previousPerSlot[Slot.SNACK] = snack.id
            }

            DayPlan(dayIndex = dayIndex, items = items)
        }

        return MealPlan(dayPlans)
    }
}
