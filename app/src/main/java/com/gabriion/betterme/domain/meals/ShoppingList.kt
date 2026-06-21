package com.gabriion.betterme.domain.meals

data class ShoppingItem(
    val ingredient: String,
    val count: Int,
    val mealNames: List<String>,
)

object ShoppingList {

    /**
     * Flatten a [MealPlan] into a sorted shopping list. Quantity = number of meals
     * referencing that ingredient. Sort: count desc, then alphabetical.
     */
    fun fromPlan(plan: MealPlan): List<ShoppingItem> {
        val acc = linkedMapOf<String, MutableList<String>>()
        for (meal in plan.allMeals) {
            for (raw in meal.ingredients) {
                val key = raw.trim().lowercase()
                if (key.isEmpty()) continue
                acc.getOrPut(key) { mutableListOf() } += meal.name
            }
        }
        return acc.map { (ingredient, names) ->
            ShoppingItem(
                ingredient = ingredient,
                count = names.size,
                mealNames = names.distinct().sorted(),
            )
        }.sortedWith(compareByDescending<ShoppingItem> { it.count }.thenBy { it.ingredient })
    }
}
