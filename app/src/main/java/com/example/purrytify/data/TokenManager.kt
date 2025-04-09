package com.example.purrytify.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class TokenManager private constructor(context: Context) {
    private val masterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        "secure_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"

        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context).also { INSTANCE = it }
            }
        }
    }

    suspend fun saveAccessToken(accessToken: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit() { putString(ACCESS_TOKEN_KEY, accessToken) }
    }

    suspend fun saveRefreshToken(refreshToken: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit() { putString(REFRESH_TOKEN_KEY, refreshToken) }
    }

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
    }

    suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        sharedPreferences.getString(REFRESH_TOKEN_KEY, null)
    }

    suspend fun clearTokens() = withContext(Dispatchers.IO) {
        sharedPreferences.edit().apply {
            remove(ACCESS_TOKEN_KEY)
            remove(REFRESH_TOKEN_KEY)
            apply()
        }
    }
}
