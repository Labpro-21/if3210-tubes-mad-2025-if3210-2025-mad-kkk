package com.example.purrytify.model

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.TokenPreferences
import com.example.purrytify.service.ApiClient
import com.example.purrytify.service.AuthService
import com.example.purrytify.service.LoginRequest
import com.example.purrytify.service.RefreshRequest
import kotlinx.coroutines.launch

class LoginViewModel() : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var isSubmitLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSubmitLoading = true
            try {
                val response = ApiClient.authService.login(LoginRequest(email, password))
                Log.d("CONSOLE_DEBUG", "${response.refreshToken} ${response.accessToken}")
                TokenPreferences.saveAccessToken(response.accessToken)
                TokenPreferences.saveRefreshToken(response.refreshToken)
                onSuccess()
            } catch (e: Exception) {
                Log.d("CONSOLE_DEBUG", e.message?: "")
                errorMessage = "Login failed"
            }
            isSubmitLoading = false
        }
    }

    suspend fun checkIsTokenExist(): Boolean {
        val token = TokenPreferences.getAccessToken()
        return token != null
    }

    suspend fun validateToken(
        onValid: () -> Unit,
        onRefreshFailed: () -> Unit
    ) {
        isLoading = true
        val accessToken = TokenPreferences.getAccessToken() ?: run {
            isLoading = false
            return onRefreshFailed()
        }
        try {
            val response = ApiClient.authService.validate("Bearer $accessToken")
            if (response.valid) {
                onValid()
            } else {
                refreshToken(onValid, onRefreshFailed)
            }
        } catch (e: Exception) {
            refreshToken(onValid, onRefreshFailed)
        } finally {
            isLoading = false
        }
    }


    private suspend fun refreshToken(
        onSuccess: () -> Unit,
        onFailed: () -> Unit
    ) {
        val refreshToken = TokenPreferences.getRefreshToken() ?: return onFailed()
        try {
            val response = ApiClient.authService.refresh(RefreshRequest(refreshToken))
            TokenPreferences.saveAccessToken(response.accessToken)
            TokenPreferences.saveRefreshToken(response.refreshToken)
            onSuccess()
        } catch (e: Exception) {
            onFailed()
        }
    }
}

