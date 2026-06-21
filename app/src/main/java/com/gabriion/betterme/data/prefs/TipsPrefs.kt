package com.gabriion.betterme.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tipsDataStore by preferencesDataStore(name = "tips_prefs")

@Singleton
class TipsPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notifyKey = booleanPreferencesKey("tips_notify_enabled")

    val notifyEnabledFlow: Flow<Boolean> =
        context.tipsDataStore.data.map { it[notifyKey] ?: false }

    suspend fun setNotifyEnabled(value: Boolean) {
        context.tipsDataStore.edit { it[notifyKey] = value }
    }
}
