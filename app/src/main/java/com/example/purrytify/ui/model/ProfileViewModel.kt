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
import com.example.purrytify.PurrytifyApplication
import com.example.purrytify.data.TokenManager
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.model.Song
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.service.ApiClient
import com.example.purrytify.service.Profile
import com.example.purrytify.service.RefreshRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.UnknownHostException

data class SongStats(
    val totalSongs: Int = 0,
    val likedSongs: Int = 0,
    val listenedSongs: Int = 50
)

class ProfileViewModel(application: Application, private val tokenManager: TokenManager) :
    AndroidViewModel(application) {
    private val songRepository: SongRepository

    private val _userState: MutableStateFlow<Profile?> = MutableStateFlow(null)
    val userState: StateFlow<Profile?> = _userState.asStateFlow()

    private val _songStats: MutableStateFlow<SongStats?> = MutableStateFlow(null)
    val songStats: StateFlow<SongStats?> = _songStats.asStateFlow()

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

                val isValid = withContext(Dispatchers.IO) {
                    runCatching {
                        ApiClient.authService.validate("Bearer $accessToken").valid
                    }.getOrElse {
                        if (it is ConnectException || it is UnknownHostException) {
                            success = false
                            return@withContext true // assume valid for now to avoid logout
                        } else false
                    }
                }

                if (!isValid) {
                    val refreshed = refreshAccessToken() ?: run {
                        onLogout()
                        return@launch
                    }
                    accessToken = refreshed
                }

                val profile = withContext(Dispatchers.IO) {
                    ApiClient.profileService.getProfile("Bearer $accessToken")
                }

                _userState.value = profile
                success = true
                onSuccess()

            } catch (e: Exception) {
                Log.d("LOAD_USER_PROFILE", e.message ?: "")
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

    private suspend fun refreshAccessToken(): String? = withContext(Dispatchers.IO) {
        val refreshToken = tokenManager.getRefreshToken() ?: return@withContext null
        return@withContext runCatching {
            val refreshResponse = ApiClient.authService.refresh(RefreshRequest(refreshToken))
            tokenManager.saveAccessToken(refreshResponse.accessToken)
            tokenManager.saveRefreshToken(refreshResponse.refreshToken)
            refreshResponse.accessToken
        }.getOrElse {
            Log.d("REFRESH_TOKEN", it.message ?: "")
            null
        }
    }


    fun loadSongStats() {
        viewModelScope.launch {
            val userId = userState.firstOrNull()?.id ?: return@launch

            val totalSongsFlow = songRepository.getNumberOfSong(userId)
            val likedSongsDeferred = async { songRepository.likedSongs(userId).first().size }
            val listenedCountDeferred = async { songRepository.getCountOfListenedSong(userId).first() }

            totalSongsFlow.collect { total ->
                val liked = likedSongsDeferred.await()
                val listened = listenedCountDeferred.await()
                _songStats.value = SongStats(
                    totalSongs = total,
                    likedSongs = liked,
                    listenedSongs = listened
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

    class ProfileViewModelFactory(private val application: Application) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(application, PurrytifyApplication.tokenManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}