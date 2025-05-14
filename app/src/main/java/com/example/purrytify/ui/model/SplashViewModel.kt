//package com.example.purrytify.ui.model
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewModelScope
//import com.example.purrytify.PurrytifyApplication
//import com.example.purrytify.data.TokenManager
//import com.example.purrytify.service.ApiClient
//import com.example.purrytify.service.RefreshRequest
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.net.ConnectException
//
//class SplashViewModel(private val tokenManager: TokenManager) : ViewModel() {
//    suspend fun checkIsTokenExist(): Boolean {
//        return tokenManager.getRefreshToken() != null
//    }
//
//    fun validateToken(
//        onValid: (Int, String) -> Unit,
//        onRefreshFailed: () -> Unit
//    ) {
//        viewModelScope.launch {
//            try {
//                val accessToken = tokenManager.getAccessToken() ?: run {
//                    onRefreshFailed()
//                    return@launch
//                }
//
//                try {
//                    val response = withContext(Dispatchers.IO) {
//                        ApiClient.authService.validate("Bearer $accessToken")
//                    }
//                    if (response.valid) {
//                        val user = withContext(Dispatchers.IO) {
//                            ApiClient.profileService.getProfile("Bearer $accessToken")
//                        }
//                        onValid(user.id, user.location)
//                    } else {
//                        refreshToken(onValid, onRefreshFailed)
//                    }
//                } catch (e: Exception) {
//                    if (e is ConnectException) {
//                        //
//                    } else {
//                        refreshToken(onValid, onRefreshFailed)
//                    }
//                }
//            } catch (e: Exception) {
//                onRefreshFailed()
//            }
//        }
//    }
//
//    private suspend fun refreshToken(
//        onSuccess: (Int, String) -> Unit,
//        onFailed: () -> Unit
//    ) {
//        val refreshToken = tokenManager.getRefreshToken() ?: run {
//            onFailed()
//            return
//        }
//
//        try {
//            val response = withContext(Dispatchers.IO) {
//                ApiClient.authService.refresh(RefreshRequest(refreshToken))
//            }
//            val user = withContext(Dispatchers.IO) {
//                ApiClient.profileService.getProfile("Bearer ${response.accessToken}")
//            }
//            tokenManager.saveAccessToken(response.accessToken)
//            tokenManager.saveRefreshToken(response.refreshToken)
//            onSuccess(user.id, user.location)
//        } catch (e: Exception) {
//            if (e is ConnectException) {
//                //
//            }
//            onFailed()
//        }
//    }
//
//    companion object {
//        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
//            @Suppress("UNCHECKED_CAST")
//            override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                return SplashViewModel(PurrytifyApplication.tokenManager) as T
//            }
//        }
//    }
//}