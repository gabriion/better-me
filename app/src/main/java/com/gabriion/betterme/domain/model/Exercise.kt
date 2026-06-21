package com.gabriion.betterme.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val repsLow: Int,
    val repsHigh: Int,
    val setsDefault: Int,
    val lottie: String,
)
