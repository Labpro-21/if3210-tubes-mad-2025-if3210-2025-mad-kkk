package com.example.purrytify.ui.model

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.purrytify.data.repository.SongLogsRepository
import com.example.purrytify.data.repository.SongRecommendationRepository
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.service.ApiClient
import com.example.purrytify.service.OnlineSongResponse
import com.example.purrytify.ui.util.extractColorsFromImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RecommendationsViewModel(
    application: Application,
    private val globalViewModel: GlobalViewModel
) : AndroidViewModel(application) {

    private val songRepository: SongRepository
    private val songLogsRepository: SongLogsRepository
    private val songRecommendationRepository: SongRecommendationRepository

    var isLoading by mutableStateOf(true)

    var success by mutableStateOf(true)

    var recommendedSongs = mutableStateListOf<Song>()
        private set

    private var loadJob: Job? = null

    private val MIN_RECOMMENDATIONS_TARGET = 20

    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        val songLogsDao = SongDatabase.getDatabase(application).songLogsDao()
        songRepository = SongRepository(songDao, application)
        songLogsRepository = SongLogsRepository(songLogsDao, application)
        songRecommendationRepository = SongRecommendationRepository(
            songDao,
            songLogsDao,
            application
        )
        isLoading = true
    }

    fun loadRecommendedSongs() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            isLoading = true
            try {
                val currentUserId = globalViewModel.userId.value
                    ?: throw IllegalStateException("User ID is null")

                val likedArtistBased = withContext(Dispatchers.IO) {
                    songRecommendationRepository.getLikedArtistsBasedRecommendations(currentUserId)
                        .first()
                }
                Log.d("RecommendationsVM", "Liked Artist Based: ${likedArtistBased.size} songs")

                val durationArtistBased = withContext(Dispatchers.IO) {
                    songRecommendationRepository.getDurationArtistsBasedRecommendations(
                        currentUserId
                    ).first()
                }
                Log.d(
                    "RecommendationsVM",
                    "Duration Artist Based: ${durationArtistBased.size} songs"
                )

                val combinedUserRecommendations = (likedArtistBased + durationArtistBased)
                    .distinctBy { it.id }

                val finalRecommendations = mutableListOf<SongEntity>()
                finalRecommendations.addAll(combinedUserRecommendations)

                Log.d(
                    "RecommendationsVM",
                    "User-related recommendations combined: ${finalRecommendations.size} songs"
                )

                if (finalRecommendations.size < MIN_RECOMMENDATIONS_TARGET) {
                    Log.d(
                        "RecommendationsVM",
                        "Below target (${finalRecommendations.size} < $MIN_RECOMMENDATIONS_TARGET). Fetch Top Global Songs."
                    )

                    val context = getApplication<Application>().applicationContext

                    val rawOnlineSongs: List<OnlineSongResponse> = withContext(Dispatchers.IO) {
                        try {
                            ApiClient.onlineSongService.topGlobalSongs()
                        } catch (e: Exception) {
                            Log.e(
                                "RecommendationsVM",
                                "Error fetching online top global songs: ${e.message}",
                                e
                            )
                            emptyList()
                        }
                    }

                    val sortedOnlineSongs = rawOnlineSongs.sortedBy { it.rank }
                    Log.d(
                        "RecommendationsVM",
                        "Fetched ${sortedOnlineSongs.size} song from Top Global Songs."
                    )

                    val serverIds = sortedOnlineSongs.map { it.id }
                    val existing = songRepository
                        .getSongsByServerId(serverIds, currentUserId)
                        .associateBy { it.serverId }

                    val toInsert = mutableListOf<SongEntity>()

                    for (onlineSong in sortedOnlineSongs) {
                        if (finalRecommendations.size >= MIN_RECOMMENDATIONS_TARGET) {
                            Log.d(
                                "RecommendationsVM",
                                "Target (${MIN_RECOMMENDATIONS_TARGET}) reached. Stopping."
                            )
                            break
                        }

                        if (finalRecommendations.none { it.serverId == onlineSong.id }) {
                            val local = existing[onlineSong.id]
                            if (local == null) {
                                val uriImage = getUriFromPath(onlineSong.artwork)
                                val colors = extractColorsFromImage(context, uriImage)
                                val primaryColor = colors[0].toArgb()
                                val secondaryColor = colors[1].toArgb()
                                toInsert += SongEntity(
                                    serverId = onlineSong.id,
                                    title = onlineSong.title,
                                    artist = onlineSong.artist,
                                    imagePath = onlineSong.artwork,
                                    audioPath = onlineSong.url,
                                    remoteImagePath = onlineSong.artwork,
                                    remoteAudioPath = onlineSong.url,
                                    primaryColor = primaryColor,
                                    secondaryColor = secondaryColor,
                                    userId = currentUserId,
                                    isDownloaded = false
                                )
                                finalRecommendations += SongEntity(
                                    serverId = onlineSong.id,
                                    title = onlineSong.title,
                                    artist = onlineSong.artist,
                                    imagePath = onlineSong.artwork,
                                    audioPath = onlineSong.url,
                                    remoteImagePath = onlineSong.artwork,
                                    remoteAudioPath = onlineSong.url,
                                    primaryColor = primaryColor,
                                    secondaryColor = secondaryColor,
                                    userId = currentUserId,
                                    isDownloaded = false
                                )
                            } else {
                                finalRecommendations += local.copy()
                            }
                        }
                    }

                    withContext(Dispatchers.IO) {
                        songRepository.insertAndUpdate(
                            toInsert,
                            emptyList(),
                            serverIds,
                            currentUserId
                        )
                    }
                }

                Log.d(
                    "RecommendationsVM",
                    "Final recommendations list size before conversion: ${finalRecommendations.size} songs"
                )

                withContext(Dispatchers.Main) {
                    recommendedSongs.clear()
                    recommendedSongs.addAll(finalRecommendations.map { it.toSong() })
                    success = true
                }
                Log.d(
                    "RecommendationsVM",
                    "Recommended songs list updated. Final size: ${recommendedSongs.size}"
                )


            } catch (e: Exception) {
                Log.e("RecommendationsVM", "Error loading recommended songs: ${e.message}", e)
                if (e is ConnectException || e is UnknownHostException) {
                    Log.w(
                        "RecommendationsVM",
                        "Network issue: Could not fetch online global songs. Recommendations might be sparse."
                    )
                }
                withContext(Dispatchers.Main) {
                    success = false
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
                Log.d(
                    "RecommendationsVM",
                    "Finished loading recommended songs. isLoading: $isLoading"
                )
            }
        }
    }

    fun toggleLiked(song: Song) {
        val newVal = !song.isLiked
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                songRepository.updateLikedStatus(song.id, newVal)
            }
            val index = recommendedSongs.indexOfFirst { it.id == song.id }
            if (index != -1) {
                recommendedSongs[index] = song.copy(isLiked = newVal)
            }
            globalViewModel.notifyLikeSong(song, newVal)
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

    class RecommendationsViewModelFactory(
        private val application: Application,
        private val globalViewModel: GlobalViewModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RecommendationsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RecommendationsViewModel(application, globalViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}