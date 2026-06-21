package com.gabriion.betterme.data.openfoodfacts

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFoodFactsApi {
    @GET("cgi/search.pl?search_simple=1&action=process&json=1&page_size=20")
    suspend fun search(
        @Query("search_terms") query: String
    ): OffSearchResponse
}
