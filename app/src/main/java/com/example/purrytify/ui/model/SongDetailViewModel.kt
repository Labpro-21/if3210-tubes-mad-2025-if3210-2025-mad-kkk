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
import android.util.Log

class SongDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SongRepository

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked

    private var _songId = MutableStateFlow<Long?>(null)

    private val tag = "SongDetailViewModel"
    private val _debugInfo = MutableStateFlow("")
    val debugInfo: StateFlow<String> = _debugInfo

    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        repository = SongRepository(songDao, application)
    }

    fun loadSong(songId: String) {
//        val id = songId.toLongOrNull() ?: return
//        Log.d("LOAD SONG CALLED", "LOAD SONG")
//        viewModelScope.launch {
//
//            repository.getSongById(id).collect { songEntity ->
//                songEntity?.let {
//                    _currentSong.value = convertEntityToSong(it)
//                    _isLiked.value = it.isLiked
//                }
//            }
//        }
        val id = songId.toLongOrNull() ?: return

        // Set our stored ID first
        _songId.value = id

        Log.d(tag, "Loading song with ID: $id")
        _debugInfo.value = "Loading song: $id"

        // Cancel any existing collection job and start a new one
        viewModelScope.launch {
            try {
                // Get the specific song
                repository.getSongById(id).collect { songEntity ->
                    // Safety check - make sure the current ID hasn't changed
                    if (_songId.value != id) {
                        Log.w(tag, "Song ID changed during loading. Was $id, now ${_songId.value}")
                        return@collect
                    }

                    songEntity?.let {
                        Log.d(tag, "Received song: ${it.id}, ${it.title}")
                        _debugInfo.value = "Loaded: ${it.id}, ${it.title}"

                        val song = convertEntityToSong(it)
                        _currentSong.value = song
                        _isLiked.value = it.isLiked
                    } ?: run {
                        Log.e(tag, "Song with ID $id not found")
                        _debugInfo.value = "Song not found: $id"
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading song $id", e)
                _debugInfo.value = "Error: ${e.message}"
            }
        }
    }

    fun toggleLikedStatus() {

//        viewModelScope.launch {
//
//            val songId = _currentSong.value?.id ?: return@launch
//            val newLikedStatus = !_isLiked.value
//
//            _isLiked.value = newLikedStatus
//            Log.d("TOGGLE LIKED", _currentSong.value?.id.toString())
//
//            repository.updateLikedStatus(songId, newLikedStatus)
////            _currentSong.value?.let { currentSong ->
////                val updatedSong = currentSong.copy(isLiked = newLikedStatus)
////                _currentSong.value = updatedSong
////            }
//
//        }
        val songId = _songId.value ?: return
        val currentSongValue = _currentSong.value ?: return

        if (currentSongValue.id != songId) {
            Log.e(tag, "Song ID mismatch: stored ID = $songId, current song ID = ${currentSongValue.id}")
            _debugInfo.value = "ID mismatch: $songId vs ${currentSongValue.id}"
            return
        }

        val newLikedStatus = !_isLiked.value

        Log.d(tag, "Toggling like for song $songId to $newLikedStatus")
        _debugInfo.value = "Toggling like: $songId to $newLikedStatus"

        viewModelScope.launch {
            try {
                // First update UI state to avoid lag
                _isLiked.value = newLikedStatus

                // Copy current song with new liked status
                val updatedSong = currentSongValue.copy(isLiked = newLikedStatus)
                _currentSong.value = updatedSong

                // Then update database
                repository.updateLikedStatus(songId, newLikedStatus)

                Log.d(tag, "Successfully updated like status for $songId")
                _debugInfo.value = "Like updated for: $songId"
            } catch (e: Exception) {
                // Revert UI changes on error
                _isLiked.value = !newLikedStatus
                _currentSong.value = currentSongValue

                Log.e(tag, "Error updating like status", e)
                _debugInfo.value = "Update error: ${e.message}"
            }
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
            isLiked = entity.isLiked
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