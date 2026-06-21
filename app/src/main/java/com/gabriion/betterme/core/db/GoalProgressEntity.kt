package com.gabriion.betterme.core.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gabriion.betterme.domain.model.GoalProgressEvent

@Entity(
    tableName = "goal_progress",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId")]
)
data class GoalProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val goalId: Long,
    val amount: Double,
    val note: String?,
    val timestamp: Long,
    val source: String
) {
    fun toDomain(): GoalProgressEvent = GoalProgressEvent(
        id = id,
        goalId = goalId,
        amount = amount,
        note = note,
        timestamp = timestamp,
        source = source
    )

    companion object {
        fun fromDomain(event: GoalProgressEvent): GoalProgressEntity = GoalProgressEntity(
            id = event.id,
            goalId = event.goalId,
            amount = event.amount,
            note = event.note,
            timestamp = event.timestamp,
            source = event.source
        )
    }
}
