package com.gabriion.betterme.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.caloriesDataStore by preferencesDataStore(name = "calories_prefs")

@Singleton
class CaloriesPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val targetKey = intPreferencesKey("calorie_target_kcal")

    val dailyTargetFlow: Flow<Int> =
        context.caloriesDataStore.data.map { it[targetKey] ?: DEFAULT_TARGET }

    suspend fun setDailyTarget(value: Int) {
        context.caloriesDataStore.edit { it[targetKey] = value.coerceAtLeast(0) }
    }

    companion object {
        const val DEFAULT_TARGET = 2000
    }
}
