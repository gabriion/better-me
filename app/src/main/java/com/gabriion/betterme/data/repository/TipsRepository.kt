package com.gabriion.betterme.data.repository

import android.content.Context
import com.gabriion.betterme.domain.tips.ComposedTip
import com.gabriion.betterme.domain.tips.TipComposer
import com.gabriion.betterme.domain.tips.TipTemplate
import com.gabriion.betterme.health.HealthDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TipsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val health: HealthDataSource
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val templates: List<TipTemplate> by lazy {
        runCatching {
            val raw = context.assets.open("content/tips.json").bufferedReader().use { it.readText() }
            json.decodeFromString<List<TipTemplate>>(raw)
        }.getOrDefault(emptyList())
    }

    private val composer: TipComposer by lazy { TipComposer(templates) }

    suspend fun getTodayTips(): List<ComposedTip> {
        val snapshot = runCatching { health.fetchSnapshot() }.getOrNull()
        val signals = runCatching { health.fetchAppSignals() }.getOrNull()
            ?: com.gabriion.betterme.health.AppSignals.EMPTY
        return composer.compose(
            snapshot = snapshot,
            signals = signals,
            rhr7dAvg = null,
            today = LocalDate.now()
        )
    }

    suspend fun isHealthConnectReady(): Boolean = runCatching { health.isHealthConnectReady() }.getOrDefault(false)

    fun observeTodayTips(): Flow<List<ComposedTip>> = flow { emit(getTodayTips()) }
}
