package com.example.purrytify.ui.model

import android.app.Application
import android.net.Uri
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SongRepository

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private var _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private var _selectedAudioUri = MutableStateFlow<Uri?>(null)
    val selectedAudioUri: StateFlow<Uri?> = _selectedAudioUri

    val songs: Flow<List<Song>>

    init {
        val database = SongDatabase.getDatabase(application)
        repository = SongRepository(database.songDao(), application)
        @OptIn(kotlinx. coroutines. ExperimentalCoroutinesApi::class)
        songs = combine(_filterType, _searchQuery) { filterType, query ->
            filterType to query.trim()
        }.flatMapLatest { (filterType, query) ->
            val flow = when (filterType) {
                FilterType.ALL -> repository.searchAllSongs(query)
                FilterType.LIKED -> repository.searchLikedSongs(query)
            }
            flow.map { entities -> entities.map { it.toSong() } }
        }
    }

    fun setFilter(filterType: FilterType) {
        _filterType.value = filterType
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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

    private fun SongEntity.toSong(): Song {
        // TODO: changed later
        val coverResId = this.imagePath ?: R.drawable.starboy.toString()
        return Song(
            id = this.id,
            title = this.title,
            artist = this.artist,
            imagePath = coverResId,
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

    class LibraryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LibraryViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}