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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SongRepository

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs: StateFlow<List<Song>> = _likedSongs

    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        repository = SongRepository(songDao, application)
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            repository.allSongs.collect { songEntities ->
                val mappedSongs = songEntities.map { it.toSong() }
                _songs.value = mappedSongs
            }
            repository.likedSongs.collect { songEntities ->
                val mappedSongs = songEntities.map { it.toSong() }
                _likedSongs.value = mappedSongs
            }
        }
    }

    private fun SongEntity.toSong(): Song {
        val coverResId = imagePath ?: R.drawable.starboy.toString()

        return Song(
            id = this.id,
            title = this.title,
            artist = this.artist,
            imagePath = coverResId
        )
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