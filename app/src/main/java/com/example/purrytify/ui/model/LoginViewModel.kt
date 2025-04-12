package com.example.purrytify.ui.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.PurrytifyApplication
import com.example.purrytify.data.TokenManager
import com.example.purrytify.service.ApiClient
import com.example.purrytify.service.LoginRequest
import com.example.purrytify.service.RefreshRequest
import kotlinx.coroutines.launch
import java.net.ConnectException

class LoginViewModel(private val tokenManager: TokenManager) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(true)
    var isSubmitLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun login(onSuccess: (Int) -> Unit) {
        viewModelScope.launch {
            isSubmitLoading = true
            try {
                val response = ApiClient.authService.login(LoginRequest(email, password))
                val user = ApiClient.profileService.getProfile("Bearer ${response.accessToken}")
                tokenManager.saveAccessToken(response.accessToken)
                tokenManager.saveRefreshToken(response.refreshToken)
                onSuccess(user.id)
            } catch (e: Exception) {
                if (e is ConnectException) {
                    errorMessage = "No internet connection"
                }
            } finally {
                isSubmitLoading = false
            }
        }
    }

    suspend fun checkIsTokenExist(): Boolean {
        return tokenManager.getAccessToken() != null
    }

    fun validateToken(
        onValid: (Int) -> Unit,
        onRefreshFailed: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val accessToken = tokenManager.getAccessToken() ?: run {
                    isLoading = false
                    onRefreshFailed()
                    return@launch
                }

                try {
                    val response = ApiClient.authService.validate("Bearer $accessToken")
                    if (response.valid) {
                        val user = ApiClient.profileService.getProfile("Bearer $accessToken")
                        onValid(user.id)
                    } else {
                        refreshToken(onValid, onRefreshFailed)
                    }
                } catch (e: Exception) {
                    if (e is ConnectException) {
                        errorMessage = "No internet connection"
                        isLoading = false
                    } else {
                        refreshToken(onValid, onRefreshFailed)
                    }
                }
            } catch (e: Exception) {
                isLoading = false
                onRefreshFailed()
            }
        }
    }

    private suspend fun refreshToken(
        onSuccess: (Int) -> Unit,
        onFailed: () -> Unit
    ) {
        val refreshToken = tokenManager.getRefreshToken() ?: run {
            isLoading = false
            onFailed()
            return
        }

        try {
            val response = ApiClient.authService.refresh(RefreshRequest(refreshToken))
            val user = ApiClient.profileService.getProfile("Bearer ${response.accessToken}")
            tokenManager.saveAccessToken(response.accessToken)
            tokenManager.saveRefreshToken(response.refreshToken)
            onSuccess(user.id)
        } catch (e: Exception) {
            if (e is ConnectException) {
                errorMessage = "No internet connection"
            }
            isLoading = false
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

