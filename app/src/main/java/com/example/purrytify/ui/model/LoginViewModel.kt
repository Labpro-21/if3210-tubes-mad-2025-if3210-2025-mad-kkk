package com.example.purrytify.ui.model

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.PurrytifyApplication
import com.example.purrytify.data.TokenManager
import com.example.purrytify.service.ApiClient
import com.example.purrytify.service.AuthService
import com.example.purrytify.service.LoginRequest
import com.example.purrytify.service.RefreshRequest
import kotlinx.coroutines.launch

class LoginViewModel(private val tokenManager: TokenManager) : ViewModel() {
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
                Log.d("CONSOLE_DEBUG", "Login successful")
                tokenManager.saveAccessToken(response.accessToken)
                tokenManager.saveRefreshToken(response.refreshToken)
                onSuccess()
            } catch (e: Exception) {
                Log.e("CONSOLE_DEBUG", "Login failed", e)
                errorMessage = "Login failed"
            } finally {
                isSubmitLoading = false
            }
        }
    }

    suspend fun checkIsTokenExist(): Boolean {
        return tokenManager.getAccessToken() != null
    }

    fun validateToken(
        onValid: () -> Unit,
        onRefreshFailed: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            try {
                val accessToken = tokenManager.getAccessToken() ?: run {
                    isLoading = false
                    onRefreshFailed()
                    return@launch
                }

                try {
                    val response = ApiClient.authService.validate("Bearer $accessToken")
                    if (response.valid) {
                        onValid()
                    } else {
                        refreshToken(onValid, onRefreshFailed)
                    }
                } catch (e: Exception) {
                    Log.e("CONSOLE_DEBUG", "Token validation failed", e)
                    refreshToken(onValid, onRefreshFailed)
                }
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun refreshToken(
        onSuccess: () -> Unit,
        onFailed: () -> Unit
    ) {
        val refreshToken = tokenManager.getRefreshToken() ?: run {
            onFailed()
            return
        }

        try {
            val response = ApiClient.authService.refresh(RefreshRequest(refreshToken))
            tokenManager.saveAccessToken(response.accessToken)
            tokenManager.saveRefreshToken(response.refreshToken)
            onSuccess()
        } catch (e: Exception) {
            Log.e("CONSOLE_DEBUG", "Token refresh failed", e)
            onFailed()
        }
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(PurrytifyApplication.tokenManager) as T
            }
        }
    }
}

