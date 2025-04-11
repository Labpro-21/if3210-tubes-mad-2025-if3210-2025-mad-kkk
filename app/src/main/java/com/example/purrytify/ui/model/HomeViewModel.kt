package com.example.purrytify.ui.model

import android.app.Application
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
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
import com.example.purrytify.ui.util.extractColorsFromImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application, private val globalViewModel: GlobalViewModel) : AndroidViewModel(application) {

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
        return Song(
            id = this.id,
            title = this.title,
            artist = this.artist,
            imagePath = this.imagePath,
            audioPath = this.audioPath,
            isLiked = this.isLiked,
            primaryColor = this.primaryColor,
            secondaryColor = this.secondaryColor
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
                primaryColor = this.primaryColor,
                secondaryColor = this.secondaryColor
            )
        } else null
    }

    fun updateSong(id: Long, title: String, artist: String, uriImage: Uri?, uriAudio: Uri?) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            var songEntity: SongEntity = repository.getSongById(id).first() ?: return@launch
            var thumbnail = songEntity.imagePath
            var audio = songEntity.audioPath
            var primaryColor = songEntity.primaryColor
            var secondaryColor = songEntity.secondaryColor

            if (uriImage != null) {
                withContext(Dispatchers.IO) {
                    val imagePath = repository.saveThumbnail(uriImage)

                    thumbnail = imagePath

                    val colors = extractColorsFromImage(context, uriImage)
                    primaryColor = colors[0].toArgb()
                    secondaryColor = colors[1].toArgb()

                }
            }

            if (uriAudio != null) {
                withContext(Dispatchers.IO) {
                    val audioPath = repository.saveAudio(uriAudio)
                    audio = audioPath
                }
            }

            repository.updateSongById(
                id = id,
                title = title,
                artist = artist,
                imageUri = thumbnail,
                audioUri = audio,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                isLiked = false
            )

            val updatedSong = Song(
                id = id,
                title = title,
                artist = artist,
                imagePath = thumbnail,
                audioPath = audio,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                isLiked = false
            )

            globalViewModel.notifyUpdateSong(updatedSong)
        }
    }

    class HomeViewModelFactory(private val application: Application, private val globalViewModel: GlobalViewModel) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(application, globalViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}