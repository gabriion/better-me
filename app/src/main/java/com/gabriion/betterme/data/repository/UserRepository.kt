package com.gabriion.betterme.data.repository

import android.content.Context
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

    val firstNameFlow: Flow<String?> = context.userDataStore.data.map { it[firstNameKey] }

    // Blocking read for ViewModel init; replace with collected flow once onboarding is wired.
    fun firstNameOrDefault(): String = runBlocking {
        runCatching { firstNameFlow.first() }.getOrNull() ?: "friend"
    }

    suspend fun setFirstName(name: String) {
        context.userDataStore.edit { it[firstNameKey] = name }
    }
}
