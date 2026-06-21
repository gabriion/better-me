package com.gabriion.betterme.core.db

import androidx.room.TypeConverter
import com.gabriion.betterme.domain.model.GoalType
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epoch: Long?): LocalDate? = epoch?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun goalTypeToString(type: GoalType): String = type.name

    @TypeConverter
    fun stringToGoalType(name: String): GoalType = GoalType.valueOf(name)
}
