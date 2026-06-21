package com.gabriion.betterme.core.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * One row per food consumed at a given meal slot on a given date.
 * `foodId` is OpenFoodFacts barcode or a local manual id (prefixed "m_").
 */
@Entity(
    tableName = "food_entries",
    indices = [Index("date"), Index("foodId")]
)
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "slot") val slot: String, // breakfast/lunch/dinner/snack
    @ColumnInfo(name = "foodId") val foodId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "grams") val grams: Double,
    @ColumnInfo(name = "kcal") val kcal: Double,
    @ColumnInfo(name = "protein_g") val proteinG: Double,
    @ColumnInfo(name = "carbs_g") val carbsG: Double,
    @ColumnInfo(name = "fat_g") val fatG: Double
)

/**
 * Cached OpenFoodFacts (or manually added) food entry, used for search-as-you-type.
 * Macros are per 100g.
 */
@Entity(
    tableName = "food_cache",
    indices = [Index("name"), Index(value = ["foodId"], unique = true)]
)
data class FoodCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "foodId") val foodId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "brand") val brand: String? = null,
    @ColumnInfo(name = "kcal_per_100g") val kcalPer100g: Double,
    @ColumnInfo(name = "protein_per_100g") val proteinPer100g: Double,
    @ColumnInfo(name = "carbs_per_100g") val carbsPer100g: Double,
    @ColumnInfo(name = "fat_per_100g") val fatPer100g: Double,
    @ColumnInfo(name = "favourite") val favourite: Boolean = false,
    @ColumnInfo(name = "last_used") val lastUsed: Long? = null
)
