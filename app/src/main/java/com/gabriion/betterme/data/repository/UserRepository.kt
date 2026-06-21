package com.gabriion.betterme.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firstNameKey = stringPreferencesKey("first_name")
    private val onboardedKey = booleanPreferencesKey("onboarded")

    val firstNameFlow: Flow<String?> = context.userDataStore.data.map { it[firstNameKey] }
    val onboardedFlow: Flow<Boolean> = context.userDataStore.data.map { it[onboardedKey] ?: false }

    // Blocking read for ViewModel init; cheap because DataStore caches in memory after first read.
    fun firstNameOrDefault(): String = runBlocking {
        runCatching { firstNameFlow.first() }.getOrNull()?.takeIf { it.isNotBlank() } ?: "friend"
    }

    fun isOnboardedBlocking(): Boolean = runBlocking {
        runCatching { onboardedFlow.first() }.getOrDefault(false)
    }

    suspend fun setFirstName(name: String) {
        context.userDataStore.edit { it[firstNameKey] = name }
    }

    suspend fun setOnboarded(value: Boolean) {
        context.userDataStore.edit { it[onboardedKey] = value }
    }
}
