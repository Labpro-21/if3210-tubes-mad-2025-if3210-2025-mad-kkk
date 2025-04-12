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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.PurrytifyApplication
import com.example.purrytify.data.TokenManager
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.service.ApiClient
import com.example.purrytify.service.Profile
import com.example.purrytify.service.RefreshRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException

data class SongStats(
    val totalSongs: Int = 0,
    val likedSongs: Int = 0,
    val listenedSongs: Int = 50
)

class ProfileViewModel(application: Application, private val tokenManager: TokenManager) : AndroidViewModel(application) {
    private val songRepository: SongRepository

    private val _userState = MutableStateFlow<Profile>(Profile())
    val userState: StateFlow<Profile> = _userState.asStateFlow()

    private val _songStats = MutableStateFlow<SongStats>(SongStats())
    val songStats: StateFlow<SongStats> = _songStats.asStateFlow()

    var isLoading by mutableStateOf(true)
    var success by mutableStateOf(true)
    var isLoggingOut by mutableStateOf(false)
        private set

    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        songRepository = SongRepository(songDao, application)
    }

    fun loadUserProfile(onLogout: () -> Unit, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                var accessToken = tokenManager.getAccessToken()

                if (accessToken == null) {
                    onLogout()
                    return@launch
                }

                val isValid = try {
                    ApiClient.authService.validate("Bearer $accessToken").valid
                } catch (e: Exception) {
                    Log.d("LOAD_USER_PROFILE", e.message?:"")
                    if (e is ConnectException || e is UnknownHostException) {
                        success = false
                        return@launch
                    } else {
                        false
                    }
                }

                if (!isValid) {
                    val refreshToken = tokenManager.getRefreshToken()
                    if (refreshToken == null) {
                        onLogout()
                        return@launch
                    }

                    try {
                        val refreshResponse = ApiClient.authService.refresh(RefreshRequest(refreshToken))
                        accessToken = refreshResponse.accessToken
                        tokenManager.saveAccessToken(refreshResponse.accessToken)
                        tokenManager.saveRefreshToken(refreshResponse.refreshToken)
                    } catch (e: Exception) {
                        Log.d("LOAD_USER_PROFILE", e.message?:"")
                        if (e is ConnectException || e is UnknownHostException) {
                            success = false
                            return@launch
                        } else {
                            onLogout()
                            return@launch
                        }
                    }
                }

                val profile = ApiClient.profileService.getProfile("Bearer $accessToken")
                _userState.value = profile
                success = true

                onSuccess()

            } catch (e: Exception) {
                Log.d("LOAD_USER_PROFILE", e.message?:"")
                if (e is ConnectException || e is UnknownHostException) {
                    success = false
                } else {
                    onLogout()
                }
            } finally {
                isLoading = false
            }
        }
    }



    fun loadSongStats() {
        viewModelScope.launch {
            val curr = userState.first()
            val userId = curr.id
            songRepository.getNumberOfSong(userId).collect { totalSongs ->
                val likedCount = songRepository.likedSongs(userId).first().size
                val listenedCount = songRepository.getCountOfListenedSong(userId).first()
                _songStats.value = SongStats(
                    totalSongs = totalSongs,
                    likedSongs = likedCount,
                    listenedSongs = listenedCount
                )
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            isLoggingOut = true
            tokenManager.clearTokens()
            isLoggingOut = false
            onComplete()
        }
    }

    class ProfileViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(application, PurrytifyApplication.tokenManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}