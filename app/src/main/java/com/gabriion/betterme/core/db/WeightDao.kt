package com.gabriion.betterme.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weight_entries WHERE date >= :from ORDER BY date ASC")
    fun observeSince(from: LocalDate): Flow<List<WeightEntity>>

    @Insert
    suspend fun insert(entry: WeightEntity): Long

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM weight_entries ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun latest(): WeightEntity?
}
