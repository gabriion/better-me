package com.gabriion.betterme.core.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Logged workout set — one row per (workoutSession, exerciseId, setIndex).
 */
@Entity(
    tableName = "workout_sets",
    indices = [Index("date"), Index("exerciseId")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "date") val date: Long, // epoch millis
    @ColumnInfo(name = "exerciseId") val exerciseId: String,
    @ColumnInfo(name = "exerciseName") val exerciseName: String,
    @ColumnInfo(name = "setIndex") val setIndex: Int,
    @ColumnInfo(name = "reps") val reps: Int,
    @ColumnInfo(name = "weightKg") val weightKg: Double?,
    @ColumnInfo(name = "note") val note: String? = null
)

/**
 * Persists the user's chosen training plan profile (days/week, target groups, level).
 */
@Entity(tableName = "training_plan")
data class TrainingPlanEntity(
    @PrimaryKey val id: Int = 0, // singleton row
    @ColumnInfo(name = "daysPerWeek") val daysPerWeek: Int,
    @ColumnInfo(name = "targetGroups") val targetGroupsCsv: String,
    @ColumnInfo(name = "level") val level: String, // beginner/intermediate/advanced
    @ColumnInfo(name = "templateId") val templateId: String
)
