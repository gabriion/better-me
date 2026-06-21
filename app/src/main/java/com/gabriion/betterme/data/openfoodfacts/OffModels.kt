package com.gabriion.betterme.data.openfoodfacts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OffSearchResponse(
    val products: List<OffProduct> = emptyList()
)

@Serializable
data class OffProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    val nutriments: Nutriments? = null
)

@Serializable
data class Nutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double = 0.0,
    @SerialName("proteins_100g") val proteins100g: Double = 0.0,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double = 0.0,
    @SerialName("fat_100g") val fat100g: Double = 0.0
)
