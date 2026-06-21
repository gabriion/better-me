package com.gabriion.betterme.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gabriion.betterme.domain.model.Goal
import com.gabriion.betterme.domain.model.GoalType
import java.time.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val type: GoalType,
    val target: Double,
    val unit: String,
    val deadline: LocalDate?,
    val createdAt: LocalDate,
    val archived: Boolean,
    val isSportTracked: Boolean
) {
    fun toDomain(): Goal = Goal(
        id = id,
        title = title,
        type = type,
        target = target,
        unit = unit,
        deadline = deadline,
        createdAt = createdAt,
        archived = archived,
        isSportTracked = isSportTracked
    )

    companion object {
        fun fromDomain(goal: Goal): GoalEntity = GoalEntity(
            id = goal.id,
            title = goal.title,
            type = goal.type,
            target = goal.target,
            unit = goal.unit,
            deadline = goal.deadline,
            createdAt = goal.createdAt,
            archived = goal.archived,
            isSportTracked = goal.isSportTracked
        )
    }
}
