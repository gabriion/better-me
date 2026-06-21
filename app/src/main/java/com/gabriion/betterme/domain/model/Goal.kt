package com.gabriion.betterme.domain.model

import java.time.LocalDate

enum class GoalType {
    COUNT,
    DISTANCE_KM,
    DURATION_MIN,
    FREQUENCY_DAYS,
    WEIGHT_KG
}

data class Goal(
    val id: Long = 0L,
    val title: String,
    val type: GoalType,
    val target: Double,
    val unit: String,
    val deadline: LocalDate? = null,
    val createdAt: LocalDate = LocalDate.now(),
    val archived: Boolean = false,
    val isSportTracked: Boolean = false
)

data class GoalProgressEvent(
    val id: Long = 0L,
    val goalId: Long,
    val amount: Double,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "manual"
)
