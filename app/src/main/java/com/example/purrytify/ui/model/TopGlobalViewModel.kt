package com.example.purrytify.ui.model

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.example.purrytify.service.OnlineSongResponse
import com.example.purrytify.ui.util.extractColorsFromImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.net.UnknownHostException

class TopGlobalViewModel(application: Application, private val globalViewModel: GlobalViewModel) :
    AndroidViewModel(application) {
    private val songRepository: SongRepository;

    var isLoading by mutableStateOf(true)
    var success by mutableStateOf(true)
    var songs by mutableStateOf<List<Song>>(emptyList())
    private var loadJob: Job? = null

    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        songRepository = SongRepository(songDao, application)
        isLoading = true
    }

    fun loadOnlineSong() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            val filesToUndo = mutableListOf<String>()
            val filesToDelete = mutableListOf<String>()
            isLoading = true
            try {
                val userId = globalViewModel.userId.value
                    ?: throw IllegalStateException("User ID is null")

                val context = getApplication<Application>().applicationContext

                // fetch online songs
                val rawOnlineSongs: List<OnlineSongResponse> =
                    ApiClient.onlineSongService.topGlobalSongs()

                val sortedRaw = rawOnlineSongs.sortedBy { it.rank }

                val serverIds = sortedRaw.map { it.id }
                val existing = songRepository
                    .getSongsByServerId(serverIds, userId)
                    .associateBy { it.serverId }

                val toInsert = mutableListOf<SongEntity>()
                val toUpdate = mutableListOf<SongEntity>()

                sortedRaw.forEach { remote ->
                    val local = existing[remote.id]

                    if (local == null) {
                        val uriImage = getUriFromPath(remote.artwork)
                        val colors = extractColorsFromImage(context, uriImage)
                        val primaryColor = colors[0].toArgb()
                        val secondaryColor = colors[1].toArgb()
                        toInsert += SongEntity(
                            serverId = remote.id,
                            title = remote.title,
                            artist = remote.artist,
                            imagePath = remote.artwork,
                            audioPath = remote.url,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            userId = userId,
                            isDownloaded = false
                        )
                    } else if (
                        local.title != remote.title ||
                        local.artist != remote.artist ||
                        local.imagePath != remote.artwork ||
                        local.audioPath != remote.url
                    ) {
                        val uriImage = getUriFromPath(remote.artwork)
                        val colors = extractColorsFromImage(context, uriImage)
                        val primaryColor = colors[0].toArgb()
                        val secondaryColor = colors[1].toArgb()
                        var thumbnail = remote.artwork
                        var audio = remote.url

                        if (local.isDownloaded) {
                            val uriAudio = getUriFromPath(remote.url)
                            val imageJob = async(Dispatchers.IO) {
                                thumbnail = songRepository.saveThumbnail(uriImage, userId)
                            }
                            val audioJob = async(Dispatchers.IO) {
                                audio = songRepository.saveAudio(uriAudio, userId)
                            }
                            imageJob.await()
                            audioJob.await()
                            filesToUndo += listOf(thumbnail, audio)
                            filesToDelete += listOf(local.imagePath, local.audioPath)
                        }

                        toUpdate += local.copy(
                            title = remote.title,
                            artist = remote.artist,
                            imagePath = thumbnail,
                            audioPath = audio,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )
                    }
                }

                // transaction
                songs = if (toInsert.isNotEmpty() || toUpdate.isNotEmpty()) {
                    Log.d("LOAD_ONLINE_SONGS", "Transaction")
                    songRepository.insertAndUpdate(toInsert, toUpdate, serverIds, userId)
                        .sortedBy { serverIds.indexOf(it.serverId) }
                        .map { it.toSong() }
                } else {
                    // just fetch existing songs
                    Log.d("LOAD_ONLINE_SONGS", "Fetch existing songs")
                    songRepository.getSongsByServerId(serverIds, userId)
                        .sortedBy { serverIds.indexOf(it.serverId) }
                        .map { it.toSong() }
                }

                // delete old files asynchronously
                launch(Dispatchers.IO) {
                    filesToDelete.forEach { path ->
                        try {
                            File(path).delete()
                        } catch (e: Exception) {
                            Log.e("LOAD_ONLINE_SONGS", "Failed to delete $path", e)
                        }
                    }
                }

                success = true
            } catch (e: Exception) {
                Log.e("LOAD_ONLINE_SONGS", "Error: ${e.message}", e)

                // undo newly saved files asynchronously
                launch(Dispatchers.IO) {
                    filesToUndo.forEach { path ->
                        try {
                            File(path).delete()
                        } catch (e: Exception) {
                            Log.e("LOAD_ONLINE_SONGS", "Failed to undo $path", e)
                        }
                    }
                }

                if (e is ConnectException || e is UnknownHostException) {
                    Log.w(
                        "LOAD_ONLINE_SONGS",
                        "Network issue: loading from cache not yet implemented"
                    )
                }

                success = false
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleLiked(song: Song) {
        val newVal = !song.isLiked
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                songRepository.updateLikedStatus(song.id, newVal)
            }
            globalViewModel.notifyLikeSong(song, newVal)
        }
    }

    // util functions
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
            serverId = this.serverId,
            isDownloaded = this.isDownloaded
        )
    }

    private fun getUriFromPath(path: String): Uri {
        return if (path.startsWith("content://") || path.startsWith("file://") ||
            path.startsWith("http://") || path.startsWith("https://")
        ) path.toUri()
        else Uri.fromFile(File(path))
    }

    class TopGlobalViewModelFactory(
        private val application: Application,
        private val globalViewModel: GlobalViewModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TopGlobalViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TopGlobalViewModel(application, globalViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}