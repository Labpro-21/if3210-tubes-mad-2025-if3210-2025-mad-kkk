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
import com.example.purrytify.ui.model.LibraryViewModel.FilterType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SongRepository

    val recentlyPlayedSongs: Flow<List<Song>>
    val recentlyAddedSongs: Flow<List<Song>>

    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        repository = SongRepository(songDao, application)
        @OptIn(kotlinx. coroutines. ExperimentalCoroutinesApi::class)
        recentlyAddedSongs = repository.recentlyAddedSongs.map {
            entities -> entities.map { it.toSong() }
        }
        recentlyPlayedSongs = repository.recentlyPlayedSongs.map {
                entities -> entities.map { it.toSong() }
        }
    }

    private fun SongEntity.toSong(): Song {
        // TODO: changed later
        return Song(
            id = this.id,
            title = this.title,
            artist = this.artist,
            imagePath = this.imagePath,
            audioPath = this.audioPath,
            isLiked = this.isLiked,
            duration = this.duration
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
                isLiked = this.isLiked,
                duration = this.duration
            )
        } else null
    }

    class HomeViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}