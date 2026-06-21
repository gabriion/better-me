package com.gabriion.betterme.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutTemplate(
    val id: String,
    val name: String,
    val days: List<String>,
)
