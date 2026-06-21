package com.gabriion.betterme.data.repository

import com.gabriion.betterme.core.db.FoodCacheEntity
import com.gabriion.betterme.core.db.FoodDao
import com.gabriion.betterme.core.db.FoodEntryEntity
import com.gabriion.betterme.data.openfoodfacts.OffProduct
import com.gabriion.betterme.data.openfoodfacts.OpenFoodFactsApi
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaloriesRepository @Inject constructor(
    private val dao: FoodDao,
    private val api: OpenFoodFactsApi
) {

    fun observeEntriesFor(date: LocalDate): Flow<List<FoodEntryEntity>> =
        dao.observeEntriesFor(date)

    fun observeTotalKcal(date: LocalDate): Flow<Double> =
        dao.observeTotalKcal(date)

    fun observeFavourites(): Flow<List<FoodCacheEntity>> = dao.observeFavourites()

    fun observeRecent(): Flow<List<FoodCacheEntity>> = dao.observeRecent()

    suspend fun search(query: String): List<FoodCacheEntity> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val local = dao.searchCache(q)
        if (local.size >= 3) return local

        val remote: List<FoodCacheEntity> = runCatching {
            api.search(q).products.mapNotNull { it.toCacheEntity() }
        }.getOrDefault(emptyList())

        if (remote.isNotEmpty()) {
            dao.upsertCache(remote)
        }
        // Merge: prefer local entries (have id/favourite/lastUsed); add remote entries
        // whose foodId isn't already present in local results.
        val localIds = local.mapTo(mutableSetOf()) { it.foodId }
        val extra = remote.filter { it.foodId !in localIds }
        return local + extra
    }

    suspend fun logEntry(
        cache: FoodCacheEntity,
        grams: Double,
        slot: String,
        date: LocalDate
    ) {
        val factor = grams / 100.0
        val entry = FoodEntryEntity(
            date = date,
            slot = slot,
            foodId = cache.foodId,
            name = cache.name,
            grams = grams,
            kcal = cache.kcalPer100g * factor,
            proteinG = cache.proteinPer100g * factor,
            carbsG = cache.carbsPer100g * factor,
            fatG = cache.fatPer100g * factor
        )
        dao.insertEntry(entry)
        dao.touchUsed(cache.foodId, System.currentTimeMillis())
    }

    suspend fun logManual(
        name: String,
        kcal: Double,
        grams: Double = 100.0,
        slot: String,
        date: LocalDate
    ) {
        val foodId = "m_" + System.currentTimeMillis()
        val entry = FoodEntryEntity(
            date = date,
            slot = slot,
            foodId = foodId,
            name = name,
            grams = grams,
            kcal = kcal,
            proteinG = 0.0,
            carbsG = 0.0,
            fatG = 0.0
        )
        dao.insertEntry(entry)
    }

    suspend fun delete(id: Long) = dao.deleteEntry(id)

    suspend fun toggleFavourite(foodId: String, fav: Boolean) =
        dao.setFavourite(foodId, fav)

    private fun OffProduct.toCacheEntity(): FoodCacheEntity? {
        val id = code?.takeIf { it.isNotBlank() } ?: return null
        val name = productName?.takeIf { it.isNotBlank() } ?: return null
        val n = nutriments
        return FoodCacheEntity(
            foodId = id,
            name = name,
            brand = brands?.takeIf { it.isNotBlank() },
            kcalPer100g = n?.energyKcal100g ?: 0.0,
            proteinPer100g = n?.proteins100g ?: 0.0,
            carbsPer100g = n?.carbohydrates100g ?: 0.0,
            fatPer100g = n?.fat100g ?: 0.0,
            favourite = false,
            lastUsed = null
        )
    }
}
