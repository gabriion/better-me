package com.gabriion.betterme.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Slot {
    @SerialName("breakfast") BREAKFAST,
    @SerialName("lunch")     LUNCH,
    @SerialName("dinner")    DINNER,
    @SerialName("snack")     SNACK;

    val label: String
        get() = name.lowercase().replaceFirstChar { it.titlecase() }
}

@Serializable
data class Meal(
    val id: String,
    val name: String,
    val slot: Slot,
    val ingredients: List<String> = emptyList(),
    val kcal: Int = 0,
    @SerialName("protein_g") val proteinG: Int = 0,
    @SerialName("carbs_g")   val carbsG: Int = 0,
    @SerialName("fat_g")     val fatG: Int = 0,
)
