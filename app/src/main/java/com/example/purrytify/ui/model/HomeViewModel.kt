package com.example.purrytify.ui.model

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.data.model.Song
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.service.ApiClient
import com.example.purrytify.service.DownloadRequest
import com.example.purrytify.service.DownloadService
import com.example.purrytify.service.OnlineSongResponse
import com.example.purrytify.ui.util.extractColorsFromImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import okhttp3.internal.wait
import java.io.File

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application, private val globalViewModel: GlobalViewModel) :
    AndroidViewModel(application) {

    private val repository: SongRepository

    val recentlyPlayedSongs: StateFlow<List<Song>>
    val recentlyAddedSongs: StateFlow<List<Song>>
    private var loadJob: Job? = null

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
            remoteImagePath = this.remoteImagePath,
            remoteAudioPath = this.remoteAudioPath,
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
                remoteImagePath = this.remoteImagePath,
                remoteAudioPath = this.remoteAudioPath,
                isLiked = this.isLiked,
                primaryColor = this.primaryColor,
                secondaryColor = this.secondaryColor,
                userId = this.userId,
                isDownloaded = this.isDownloaded,
                serverId = this.serverId
            )
        } else null
    }

    private fun getUriFromPath(path: String): Uri {
        return if (path.startsWith("content://") || path.startsWith("file://") ||
            path.startsWith("http://") || path.startsWith("https://")
        ) path.toUri()
        else Uri.fromFile(File(path))
    }

    fun playSharedSong(songId: Int, onSuccess: () -> Unit) {
        if (loadJob?.isActive == true) loadJob?.wait()
        loadJob = viewModelScope.launch {
            val filesToUndo = mutableListOf<String>()
            val filesToDelete = mutableListOf<String>()
            try {
                val userId = globalViewModel.userId.value
                    ?: throw IllegalStateException("User ID is null")

                val context = getApplication<Application>().applicationContext
                // fetch online song
                val rawSong: OnlineSongResponse = withContext(Dispatchers.IO) {
                    ApiClient.onlineSongService.songById(songId)
                }

                var songToPlay: Song? = null;

                // check online song
                val song: SongEntity? = repository.getSongByServerId(songId, userId)
                if (song == null) {
                    // if song not exist
                    val uriImage = getUriFromPath(rawSong.artwork)
                    val colors = extractColorsFromImage(context, uriImage)
                    val primaryColor = colors[0].toArgb()
                    val secondaryColor = colors[1].toArgb()
                    val songInsert = withContext(Dispatchers.IO) {
                        repository.insertAndGetSong(
                            song = SongEntity(
                                serverId = rawSong.id,
                                title = rawSong.title,
                                artist = rawSong.artist,
                                imagePath = rawSong.artwork,
                                audioPath = rawSong.url,
                                primaryColor = primaryColor,
                                remoteAudioPath = rawSong.url,
                                remoteImagePath = rawSong.artwork,
                                secondaryColor = secondaryColor,
                                userId = userId,
                                isDownloaded = false
                            ),
                            userId = userId,
                            serverId = songId
                        )
                    }
                    if (songInsert != null) {
                        songToPlay = songInsert.toSong()
                    }
                } else if (
                    song.title != rawSong.title ||
                    song.artist != rawSong.artist ||
                    song.remoteImagePath != rawSong.artwork ||
                    song.remoteAudioPath != rawSong.url
                ) {
                    var primaryColor = song.primaryColor
                    var secondaryColor = song.secondaryColor
                    var remoteAudioPath = song.remoteAudioPath
                    var remoteImagePath = song.remoteImagePath
                    var audioPath = song.audioPath
                    var imagePath = song.imagePath

                    if (remoteImagePath != rawSong.artwork || remoteAudioPath != rawSong.url) {
                        val uriImage = getUriFromPath(rawSong.artwork)
                        val colors = extractColorsFromImage(context, uriImage)
                        primaryColor = colors[0].toArgb()
                        secondaryColor = colors[0].toArgb()

                        if (song.isDownloaded) {
                            val uriAudio = getUriFromPath(rawSong.url)
                            val imageJob = async(Dispatchers.IO) {
                                imagePath = repository.saveThumbnail(uriImage, userId)
                            }
                            val audioJob = async(Dispatchers.IO) {
                                audioPath = repository.saveAudio(uriAudio, userId)
                            }
                            imageJob.await()
                            audioJob.await()
                            filesToUndo += listOf(imagePath, audioPath)
                            filesToDelete += listOf(song.imagePath, song.audioPath)
                        } else {
                            audioPath = rawSong.url
                            imagePath = rawSong.artwork
                        }
                    }

                    remoteAudioPath = rawSong.url
                    remoteImagePath = rawSong.artwork

                    val songUpdate = withContext(Dispatchers.IO) {
                        repository.updateAndGetSong(
                            song = song.copy(
                                title = rawSong.title,
                                artist = rawSong.artist,
                                imagePath = imagePath,
                                audioPath = audioPath,
                                remoteAudioPath = remoteAudioPath,
                                remoteImagePath = remoteImagePath,
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor
                            ),
                            userId = userId,
                            serverId = songId
                        )
                    }
                    if (songUpdate != null) {
                        songToPlay = songUpdate.toSong()
                    }
                } else {
                    songToPlay = song.toSong()
                }

                launch(Dispatchers.IO) {
                    filesToDelete.forEach { path ->
                        try {
                            File(path).delete()
                        } catch (e: Exception) {
                            Log.e("LOAD_ONLINE_SONGS", "Failed to delete $path", e)
                        }
                    }
                }

                if (songToPlay != null) {
                    globalViewModel.playSong(songToPlay)
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("LOAD_ONLINE_SONGS", "Error: ${e.message}", e)

                launch(Dispatchers.IO) {
                    filesToUndo.forEach { path ->
                        try {
                            File(path).delete()
                        } catch (e: Exception) {
                            Log.e("LOAD_ONLINE_SONGS", "Failed to undo $path", e)
                        }
                    }
                }
            }
        }
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