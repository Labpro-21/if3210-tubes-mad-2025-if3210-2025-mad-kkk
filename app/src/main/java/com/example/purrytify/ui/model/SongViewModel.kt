package com.example.purrytify.ui.model

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SongViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SongRepository

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType

    private val _currentPlayingSong = MutableStateFlow<Song?>(null)
    val currentPlayingSong: StateFlow<Song?> = _currentPlayingSong

    private var _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private var _selectedAudioUri = MutableStateFlow<Uri?>(null)
    val selectedAudioUri: StateFlow<Uri?> = _selectedAudioUri

    val songs: Flow<List<Song>>

    init {
        val database = SongDatabase.getDatabase(application)
        repository = SongRepository(database.songDao(), application)
        @OptIn(kotlinx. coroutines. ExperimentalCoroutinesApi::class)
        songs = _filterType.flatMapLatest { filterType ->
            when (filterType) {
                FilterType.ALL -> repository.allSongs
                FilterType.LIKED -> repository.likedSongs
            }.map { entities -> entities.map { it.toSong() } }
        }

    }

    fun setFilter(filterType: FilterType) {
        _filterType.value = filterType
    }

    fun setCurrentPlayingSong(song: Song) {
        _currentPlayingSong.value = song
    }

    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun setSelectedAudioUri(uri: Uri?) {
        _selectedAudioUri.value = uri
    }

    fun uploadSong(title: String, artist: String) {
        viewModelScope.launch {
            repository.insertSong(
                title = title,
                artist = artist,
                imageUri = _selectedImageUri.value,
                audioUri = _selectedAudioUri.value
            )

            // Clear selected files after upload
            _selectedImageUri.value = null
            _selectedAudioUri.value = null
        }
    }

    fun toggleLiked(song: Song) {
        viewModelScope.launch {
            repository.updateLikedStatus(song.id, !song.isLiked)
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            song.toEntity()?.let { repository.deleteSong(it) }
        }
    }

    // Helper extension functions
    private fun SongEntity.toSong(): Song {
        return Song(
            id = this.id,
            title = this.title,
            artist = this.artist,
            imagePath = this.imagePath,
            audioPath = this.audioPath,
            isLiked = this.isLiked
        )
    }

    private fun Song.toEntity(): SongEntity? {
        return if (this.id > 0) {
            SongEntity(
                id = this.id,
                title = this.title,
                artist = this.artist,
                imagePath = this.imagePath,
                audioPath = this.audioPath,
                isLiked = this.isLiked
            )
        } else null
    }

    enum class FilterType {
        ALL, LIKED
    }
}