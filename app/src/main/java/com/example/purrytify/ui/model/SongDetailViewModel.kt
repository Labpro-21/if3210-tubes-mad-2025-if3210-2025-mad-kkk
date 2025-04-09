package com.example.purrytify.ui.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.R
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

class SongDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SongRepository

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked

    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        repository = SongRepository(songDao, application)
    }

    fun loadSong(songId: String) {
        val id = songId.toLongOrNull() ?: return

        viewModelScope.launch {
            repository.allSongs.collect { songs ->
                val song = songs.find { it.id == id }
                song?.let {
                    _currentSong.value = convertEntityToSong(it)
                    _isLiked.value = it.isLiked
                }
            }
        }
    }

    fun toggleLikedStatus() {
        val songId = _currentSong.value?.id ?: return
        val newLikedStatus = !_isLiked.value

        viewModelScope.launch {
            repository.updateLikedStatus(songId, newLikedStatus)
            _isLiked.value = newLikedStatus
        }
    }

    private fun convertEntityToSong(entity: SongEntity): Song {
        val coverResId = entity.imagePath ?: R.drawable.starboy.toString()

        return Song(
            id = entity.id,
            title = entity.title,
            artist = entity.artist,
            imagePath = coverResId,
            audioPath = entity.audioPath,
            duration = entity.duration,
        )
    }

    class SongDetailViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SongDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SongDetailViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}