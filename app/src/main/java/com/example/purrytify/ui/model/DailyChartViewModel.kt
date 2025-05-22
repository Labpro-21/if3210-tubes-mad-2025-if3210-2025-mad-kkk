package com.example.purrytify.ui.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.repository.SongLogsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

data class MonthDataValue (
    val value: Int,
    val day: String
)

class DailyChartViewModel(
    application: Application,
    private val globalViewModel: GlobalViewModel,
    private val month: Int,
    private val year: Int
) : AndroidViewModel(application) {

    private val songLogsRepository: SongLogsRepository

    val monthData: StateFlow<List<MonthDataValue>>

    init {
        val songLogsDao = SongDatabase.getDatabase(application).songLogsDao()
        songLogsRepository = SongLogsRepository(songLogsDao, application)

        monthData = songLogsRepository.getMonthData(month, year).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    class DailyChartViewModelFactory(
        private val application: Application,
        private val globalViewModel: GlobalViewModel,
        private val month: Int,
        private val year: Int
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DailyChartViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DailyChartViewModel(application, globalViewModel, month, year) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}