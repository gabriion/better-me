package com.gabriion.betterme.data.repository

import android.content.Context
import com.gabriion.betterme.domain.model.Quote
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val quotes: List<Quote> by lazy {
        runCatching {
            val raw = context.assets.open("content/quotes.json").bufferedReader().use { it.readText() }
            json.decodeFromString<List<Quote>>(raw)
        }.getOrDefault(emptyList())
    }

    fun quoteOfTheDay(today: LocalDate = LocalDate.now()): String {
        if (quotes.isEmpty()) return "Be gentle with yourself today."
        val idx = (today.toEpochDay() % quotes.size).toInt().let { if (it < 0) it + quotes.size else it }
        return quotes[idx].text
    }
}
