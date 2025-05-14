package com.example.purrytify.ui.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.dao.TopArtistResult
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.repository.SongLogsRepository
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.HomeViewModel
import com.example.purrytify.ui.model.TopCountryViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class SongWithPlayCount (
    val title: String,
    val artist: String,
    val imagePath: String,
    val playCount: Int
)

class TopSongViewModel(
    application: Application,
    private val globalViewModel: GlobalViewModel
) : AndroidViewModel(application) {

    private val _topSongs = MutableStateFlow<List<SongWithPlayCount>>(emptyList())
    val topSongs: StateFlow<List<SongWithPlayCount>> = _topSongs

    private val songLogsRepository: SongLogsRepository

    init {
        val songLogsDao = SongDatabase.getDatabase(application).songLogsDao()
        songLogsRepository = SongLogsRepository(songLogsDao, application)

        loadTopSongsForCurrentMonth()
    }

    private fun loadTopSongsForCurrentMonth() {
        viewModelScope.launch {

            val userId = globalViewModel.userId.value
                ?: throw IllegalStateException("User ID is null")

            // Calculate start and end of current month
            val currentMonth = YearMonth.now()
            val startOfMonth = currentMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfMonth = currentMonth.atEndOfMonth().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

            val songList = mutableListOf<SongWithPlayCount>()

            // First, get the top artist
            songLogsRepository.getTopSongForMonth(userId, startOfMonth, endOfMonth)
                .filterNotNull()
                .forEach { topSong ->
                    // In a real app, we'd likely have a dedicated query for getting all artists
                    // with play counts. For now, we'll create a simulated list with the top artist
                    // and some dummy data.

                    // Add the top artist
                    songList.add(
                        SongWithPlayCount(
                            title = topSong.title,
                            artist = topSong.artist,
                            imagePath = topSong.imagePath,
                            playCount = topSong.playCount
                        )
                    )
                    // Sort by play count (descending
                }
            _topSongs.value = songList
        }
    }

    class TopSongViewModelFactory(
        private val application: Application,
        private val globalViewModel: GlobalViewModel
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TopSongViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TopSongViewModel(application, globalViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}