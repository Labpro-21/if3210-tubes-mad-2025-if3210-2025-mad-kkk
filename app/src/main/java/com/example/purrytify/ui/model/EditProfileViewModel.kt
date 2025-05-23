package com.example.purrytify.ui.model

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.PurrytifyApp
import com.example.purrytify.PurrytifyApplication
import com.example.purrytify.data.TokenManager
import com.example.purrytify.service.ApiClient
import com.example.purrytify.service.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.ConnectException

class EditProfileViewModel(
    application: Application,
    private val globalViewModel: GlobalViewModel,
    private val tokenManager: TokenManager
) :
    AndroidViewModel(application)
{
    var _isLoading = MutableStateFlow(false)
    var isLoading: StateFlow<Boolean> = _isLoading

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError

    fun uploadProfilePhoto(file: File, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _uploadError.value = null

            try {
                val accessToken = tokenManager.getAccessToken() ?: run {
                    _isLoading.value = false
                    _uploadError.value = "No access token available"
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    // Create request body with proper media type
                    val requestBody = file.asRequestBody("image/jpeg".toMediaType())

                    // Create multipart body part
                    val profilePhotoPart = MultipartBody.Part.createFormData(
                        "profilePhoto",
                        file.name,
                        requestBody
                    )

                    // Make the API call
                    ApiClient.editProfileService.editProfilePicture(
                        "Bearer $accessToken",
                        profilePhotoPart
                    )

                    Log.d("EDIT PROFILE", "Upload successful")

                    // Delete the temporary file after successful upload
                    if (file.exists()) {
                        file.delete()
                    }

                    withContext(Dispatchers.Main) {
                        _isLoading.value = false
                        onSuccess()
                    }
                }
            } catch (e: retrofit2.HttpException) {
                Log.e("EDIT PROFILE", "HTTP error: ${e.code()} - ${e.message()}")
                _uploadError.value = "Upload failed: ${e.code()}"
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("EDIT PROFILE", "Upload error: ${e.message}", e)
                _uploadError.value = "Upload failed: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    class EditProfileViewModelFactory(
        private val application: Application,
        private val globalViewModel: GlobalViewModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EditProfileViewModel(application, globalViewModel, PurrytifyApplication.tokenManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}