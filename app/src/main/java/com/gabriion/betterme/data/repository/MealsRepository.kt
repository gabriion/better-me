package com.gabriion.betterme.data.repository

import android.content.Context
import com.gabriion.betterme.domain.model.Meal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val pantryStaples = setOf("olive oil", "salt", "pepper", "water")

    private val meals: List<Meal> by lazy {
        runCatching {
            val raw = context.assets.open("content/meals.json")
                .bufferedReader().use { it.readText() }
            json.decodeFromString<List<Meal>>(raw)
        }.getOrDefault(emptyList())
    }

    fun allMeals(): List<Meal> = meals

    /** Union of every meal's ingredients, deduped, sorted, pantry staples removed. */
    fun allIngredients(): List<String> = meals
        .flatMap { it.ingredients }
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() && it !in pantryStaples }
        .distinct()
        .sorted()
}
