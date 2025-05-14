package com.example.purrytify.ui.model

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.PurrytifyApplication
import com.example.purrytify.data.TokenManager
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.model.Song
import com.example.purrytify.data.repository.SongLogsRepository
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.service.ApiClient
import com.example.purrytify.service.Profile
import com.example.purrytify.service.RefreshRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SongStats(
    val totalSongs: Int = 0,
    val likedSongs: Int = 0,
    val listenedSongs: Int = 50
)

class ProfileViewModel(application: Application, private val tokenManager: TokenManager) :
    AndroidViewModel(application) {
    private val songRepository: SongRepository
    private val songLogsRepository: SongLogsRepository

    private val _userState: MutableStateFlow<Profile?> = MutableStateFlow(null)
    val userState: StateFlow<Profile?> = _userState.asStateFlow()

    private val _songStats: MutableStateFlow<SongStats?> = MutableStateFlow(null)
    val songStats: StateFlow<SongStats?> = _songStats.asStateFlow()

    var isLoading by mutableStateOf(true)
    var success by mutableStateOf(true)
    var isLoggingOut by mutableStateOf(false)
        private set
    private var loadJob: Job? = null

    private val _monthlyCapsules: MutableStateFlow<List<MonthlySoundCapsule>> = MutableStateFlow(emptyList())
    val monthlyCapsules: StateFlow<List<MonthlySoundCapsule>> = _monthlyCapsules.asStateFlow()

    private val _streaks: MutableStateFlow<List<ListeningStreak?>> = MutableStateFlow(emptyList())
    val streaks: StateFlow<List<ListeningStreak?>> = _streaks.asStateFlow()

    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        val songLogsDao = SongDatabase.getDatabase(application).songLogsDao()
        songRepository = SongRepository(songDao, application)
        songLogsRepository = SongLogsRepository(songLogsDao, application)
    }

    fun loadUserProfile(onLogout: () -> Unit, onSuccess: () -> Unit) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            isLoading = true
            try {
                var accessToken = tokenManager.getAccessToken()

                if (accessToken == null) {
                    onLogout()
                    return@launch
                }

                val isValid = withContext(Dispatchers.IO) {
                    runCatching {
                        ApiClient.authService.validate("Bearer $accessToken").valid
                    }.getOrElse {
                        if (it is ConnectException || it is UnknownHostException) {
                            success = false
                            return@withContext true // assume valid for now to avoid logout
                        } else false
                    }
                }

                if (!isValid) {
                    val refreshed = refreshAccessToken() ?: run {
                        onLogout()
                        return@launch
                    }
                    accessToken = refreshed
                }

                val profile = withContext(Dispatchers.IO) {
                    ApiClient.profileService.getProfile("Bearer $accessToken")
                }

                _userState.value = profile
                success = true
                onSuccess()

            } catch (e: Exception) {
                Log.d("LOAD_USER_PROFILE", e.message ?: "")
                if (e is ConnectException || e is UnknownHostException) {
                    success = false
                } else {
                    onLogout()
                }
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun refreshAccessToken(): String? = withContext(Dispatchers.IO) {
        val refreshToken = tokenManager.getRefreshToken() ?: return@withContext null
        return@withContext runCatching {
            val refreshResponse = ApiClient.authService.refresh(RefreshRequest(refreshToken))
            tokenManager.saveAccessToken(refreshResponse.accessToken)
            tokenManager.saveRefreshToken(refreshResponse.refreshToken)
            refreshResponse.accessToken
        }.getOrElse {
            Log.d("REFRESH_TOKEN", it.message ?: "")
            null
        }
    }


    fun loadSongStats() {
        viewModelScope.launch {
            val userId = userState.firstOrNull()?.id ?: return@launch

            val totalSongsFlow = songRepository.getNumberOfSong(userId)
            val likedSongsDeferred = async { songRepository.likedSongs(userId).first().size }
            val listenedCountDeferred =
                async { songRepository.getCountOfListenedSong(userId).first() }

            totalSongsFlow.collect { total ->
                val liked = likedSongsDeferred.await()
                val listened = listenedCountDeferred.await()
                _songStats.value = SongStats(
                    totalSongs = total,
                    likedSongs = liked,
                    listenedSongs = listened
                )
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            isLoggingOut = true
            tokenManager.clearTokens()
            isLoggingOut = false
            onComplete()
        }
    }

    class ProfileViewModelFactory(private val application: Application) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(application, PurrytifyApplication.tokenManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    fun loadSoundCapsules() {
        viewModelScope.launch {
            val userId = userState.firstOrNull()?.id ?: return@launch

            // Get data for last 6 months
            val capsules = mutableListOf<MonthlySoundCapsule>()
            val calendar = Calendar.getInstance()

            val streaks = mutableListOf<ListeningStreak?>()


            val calendarEarliestLog = Calendar.getInstance()
            val earliestLog = songLogsRepository.getEarliestLog()
            calendarEarliestLog.time = Date(earliestLog)

            // Process current month and previous months
            for (i in 0 until 3) {
                // Set to first day of month
                if (i > 0) calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                val startOfMonth = calendar.timeInMillis

                // Set to last day of month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)

                val endOfMonth = calendar.timeInMillis

                // Month name
                val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

                Log.d("STARTMONTH1", startOfMonth.toString())
                Log.d("ENDMONTH", endOfMonth.toString())

                // Get data from DAO
                val totalListeningTime = songLogsRepository.getTotalListeningTimeForMonth(userId, startOfMonth, endOfMonth).firstOrNull() ?: 0
                val topSong = songLogsRepository.getTopSongForMonth(userId, startOfMonth, endOfMonth).firstOrNull()
                val topArtist = songLogsRepository.getTopArtistForMonth(userId, startOfMonth, endOfMonth).firstOrNull()

                // Add to list
                capsules.add(
                    MonthlySoundCapsule(
                        month = monthName,
                        totalListeningMinutes = totalListeningTime.div(60),
                        topSong = topSong?.let {
                            SongDetails(it.id, it.title, it.artist, it.imagePath, it.playCount)
                        },
                        topArtist = topArtist?.let {
                            ArtistDetails(it.artist, it.imagePath, it.artistPlayCount)
                        }
                    )
                )

                val currentStreak = songLogsRepository.getTopSongStreakForMonth(userId, startOfMonth, endOfMonth)
                // Add to list

                if (currentStreak != null) {
                    Log.d("CURRENT STREAK", currentStreak.title)
                    streaks.add(
                        ListeningStreak(
                            dayCount = currentStreak.streak_length,
                            startDate = currentStreak.start_date,
                            endDate = currentStreak.end_date,
                            trackDetails =
                                SongDetails(
                                    id = currentStreak.id,
                                    title = currentStreak.title,
                                    artist = currentStreak.artist,
                                    imagePath = currentStreak.imagePath,
                                    playCount = 0
                                )
                        )
                    )
                } else {
                    Log.d("CURRENT STREAK", "null")
                    streaks.add(null)
                }
                if (calendar.get(Calendar.MONTH) == calendarEarliestLog.get(Calendar.MONTH) &&
                    calendar.get(Calendar.YEAR) == calendarEarliestLog.get(Calendar.YEAR)) {
                    break
                }
            }

            _monthlyCapsules.value = capsules
            _streaks.value = streaks
            // Also load listening streaks
//            loadListeningStreaks(userId)
        }
    }
}

data class MonthlySoundCapsule(
    val month: String,
    val totalListeningMinutes: Int,
    val topSong: SongDetails? = null,
    val topArtist: ArtistDetails? = null
)
data class SongDetails(
    val id: Long,
    val title: String,
    val artist: String,
    val imagePath: String,
    val playCount: Int
)
data class ArtistDetails(
    val name: String,
    val imagePath: String,
    val playCount: Int
)
data class ListeningStreak(
    val dayCount: Int,
    val startDate: Long,
    val endDate: Long,
    val trackDetails: SongDetails? = null
)