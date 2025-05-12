package com.example.purrytify.ui.model

import android.app.Application
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.data.model.Song
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.ui.util.extractColorsFromImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application, private val globalViewModel: GlobalViewModel) :
    AndroidViewModel(application) {

    private val repository: SongRepository

    val recentlyPlayedSongs: StateFlow<List<Song>>
    val recentlyAddedSongs: StateFlow<List<Song>>

    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        repository = SongRepository(songDao, application)
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

        recentlyAddedSongs = globalViewModel.userId
            .filterNotNull()
            .flatMapLatest { userId ->
                repository.recentlyAddedSongs(userId)
                    .map { entities -> entities.map { it.toSong() } }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        recentlyPlayedSongs = globalViewModel.userId
            .filterNotNull()
            .flatMapLatest { userId ->
                repository.recentlyPlayedSongs(userId)
                    .map { entities -> entities.map { it.toSong() } }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
            secondaryColor = this.secondaryColor,
            userId = this.userId,
            lastPlayed = this.lastPlayed,
            isDownloaded = this.isDownloaded,
            serverId = this.serverId
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
                secondaryColor = this.secondaryColor,
                userId = this.userId,
                isDownloaded = this.isDownloaded,
                serverId = this.serverId
            )
        } else null
    }

    fun updateSong(id: Long, title: String, artist: String, uriImage: Uri?, uriAudio: Uri?) {
        viewModelScope.launch {
            val userId = globalViewModel.userId.value ?: return@launch
            val context = getApplication<Application>().applicationContext
            val songEntity = repository.getSongById(id).first() ?: return@launch

            var thumbnail = songEntity.imagePath
            var audio = songEntity.audioPath
            var primaryColor = songEntity.primaryColor
            var secondaryColor = songEntity.secondaryColor

            val prevThumbnail = thumbnail
            val prevAudio = audio

            val imageJob = async(Dispatchers.IO) {
                uriImage?.let {
                    val imagePath = repository.saveThumbnail(it, userId)
                    val colors = extractColorsFromImage(context, it)
                    thumbnail = imagePath
                    primaryColor = colors[0].toArgb()
                    secondaryColor = colors[1].toArgb()
                }
            }

            val audioJob = async(Dispatchers.IO) {
                uriAudio?.let {
                    audio = repository.saveAudio(it, userId)
                }
            }
            imageJob.await()
            audioJob.await()

            repository.updateSongById(
                id = id,
                title = title,
                artist = artist,
                imageUri = thumbnail,
                audioUri = audio,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                isLiked = songEntity.isLiked,
                userId = userId,
                lastPlayed = songEntity.lastPlayed,
                serverId = songEntity.serverId,
                isDownloaded = songEntity.isDownloaded
            )

            globalViewModel.notifyUpdateSong(
                Song(
                    id = id,
                    title = title,
                    artist = artist,
                    imagePath = thumbnail,
                    audioPath = audio,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    isLiked = songEntity.isLiked,
                    userId = userId,
                    lastPlayed = songEntity.lastPlayed,
                    serverId = songEntity.serverId,
                    isDownloaded = songEntity.isDownloaded
                )
            )

            withContext(Dispatchers.IO) {
                if (prevThumbnail != thumbnail) repository.deleteFile(prevThumbnail)
                if (prevAudio != audio) repository.deleteFile(prevAudio)
            }
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            val songEntity: SongEntity = song.toEntity() ?: return@launch
            withContext(Dispatchers.IO) {
                repository.deleteSong(songEntity)
            }
            globalViewModel.notifyDeleteSong(song)
        }
    }

    fun toggleLiked(song: Song) {
        val newVal = !song.isLiked
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateLikedStatus(song.id, newVal)
            }
            globalViewModel.notifyLikeSong(song, newVal)
        }
    }

    class HomeViewModelFactory(
        private val application: Application,
        private val globalViewModel: GlobalViewModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(application, globalViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}