package com.example.chader.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_session")

data class UserSession(
    val token: String?,
    val userId: String?,
    val userName: String?,
    val userEmail: String?,
    val isDarkMode: Boolean? = null,
    val language: String? = null
)

class UserSessionManager(private val context: Context) {
    companion object {
        val USER_TOKEN = stringPreferencesKey("user_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val userToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_TOKEN]
    }

    val userSession: Flow<UserSession> = context.dataStore.data.map { preferences ->
        UserSession(
            token = preferences[USER_TOKEN],
            userId = preferences[USER_ID],
            userName = preferences[USER_NAME],
            userEmail = preferences[USER_EMAIL],
            isDarkMode = preferences[IS_DARK_MODE],
            language = preferences[LANGUAGE]
        )
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE] = language
        }
    }

    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDark
        }
    }

    suspend fun saveSession(token: String, userId: String, userName: String, userEmail: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_TOKEN] = token
            preferences[USER_ID] = userId
            preferences[USER_NAME] = userName
            preferences[USER_EMAIL] = userEmail
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_TOKEN)
            preferences.remove(USER_ID)
            preferences.remove(USER_NAME)
            preferences.remove(USER_EMAIL)
            // On garde IS_DARK_MODE si on veut que le thème persiste après déconnexion
        }
    }
}
