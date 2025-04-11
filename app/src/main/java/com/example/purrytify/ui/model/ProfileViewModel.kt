package com.example.purrytify.ui.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.PurrytifyApplication
import com.example.purrytify.data.TokenManager
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.service.ApiClient
import com.example.purrytify.service.LoginRequest
import com.example.purrytify.service.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SongStats(
    val totalSongs: Int = 0,
    val likedSongs: Int = 0,
    val listenedSongs: Int = 50 // Default value as shown in the screenshot
)

class ProfileViewModel(application: Application, private val tokenManager: TokenManager) : AndroidViewModel(application) {

    private val songRepository: SongRepository

    private val _userState = MutableStateFlow<Profile>(Profile())
    val userState: StateFlow<Profile> = _userState.asStateFlow()

    private val _songStats = MutableStateFlow<SongStats>(SongStats())
    val songStats: StateFlow<SongStats> = _songStats.asStateFlow()

    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        songRepository = SongRepository(songDao, application)

        loadUserProfile()
        loadSongStats()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val accessToken = tokenManager.getAccessToken()
            val response = ApiClient.profileService.getProfile("Bearer $accessToken")
            _userState.value = response
            Log.d("IMAGE PATH", response.profilePhoto)
        }
    }

    private fun loadSongStats() {
        viewModelScope.launch {

            songRepository.getNumberOfSong().collect { totalSongs ->

                val likedCount = songRepository.likedSongs.first().size

                val listenedCount = songRepository.getCountOfListenedSong().first()

                _songStats.value = SongStats(
                    totalSongs = totalSongs,
                    likedSongs = likedCount,
                    listenedSongs = listenedCount
                )
            }
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