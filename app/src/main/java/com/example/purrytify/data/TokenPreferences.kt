package com.example.purrytify.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Extension untuk dataStore (harus berada di luar object)
private val Context.dataStore by preferencesDataStore(name = "auth")

object TokenPreferences {
    private lateinit var appContext: Context

    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun saveAccessToken(accessToken: String) {
        appContext.dataStore.edit {
            it[ACCESS_TOKEN_KEY] = accessToken
        }
    }

    suspend fun saveRefreshToken(refreshToken: String) {
        appContext.dataStore.edit {
            it[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun getAccessToken(): String? {
        return appContext.dataStore.data.first()[ACCESS_TOKEN_KEY]
    }

    suspend fun getRefreshToken(): String? {
        return appContext.dataStore.data.first()[REFRESH_TOKEN_KEY]
    }

    suspend fun clearAccessToken() {
        appContext.dataStore.edit { it.remove(ACCESS_TOKEN_KEY) }
    }

    suspend fun clearRefreshToken() {
        appContext.dataStore.edit { it.remove(REFRESH_TOKEN_KEY) }
    }
}
