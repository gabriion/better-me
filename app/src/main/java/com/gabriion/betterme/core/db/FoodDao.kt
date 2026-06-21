package com.gabriion.betterme.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface FoodDao {
    // entries -----------------------------------------------------------
    @Query("SELECT * FROM food_entries WHERE date = :date ORDER BY id ASC")
    fun observeEntriesFor(date: LocalDate): Flow<List<FoodEntryEntity>>

    @Query("SELECT COALESCE(SUM(kcal),0) FROM food_entries WHERE date = :date")
    fun observeTotalKcal(date: LocalDate): Flow<Double>

    @Insert
    suspend fun insertEntry(entry: FoodEntryEntity): Long

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    // cache -------------------------------------------------------------
    @Query("SELECT * FROM food_cache WHERE name LIKE '%' || :q || '%' OR brand LIKE '%' || :q || '%' ORDER BY favourite DESC, last_used DESC NULLS LAST, name ASC LIMIT 30")
    suspend fun searchCache(q: String): List<FoodCacheEntity>

    @Query("SELECT * FROM food_cache WHERE favourite = 1 ORDER BY name ASC LIMIT 50")
    fun observeFavourites(): Flow<List<FoodCacheEntity>>

    @Query("SELECT * FROM food_cache ORDER BY last_used DESC NULLS LAST LIMIT 20")
    fun observeRecent(): Flow<List<FoodCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsertCache(items: List<FoodCacheEntity>): List<Long>

    @Update
    suspend fun updateCache(item: FoodCacheEntity)

    @Query("UPDATE food_cache SET favourite = :fav WHERE foodId = :foodId")
    suspend fun setFavourite(foodId: String, fav: Boolean)

    @Query("UPDATE food_cache SET last_used = :ts WHERE foodId = :foodId")
    suspend fun touchUsed(foodId: String, ts: Long)
}
