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
import com.example.purrytify.data.model.Song
import com.example.purrytify.data.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.collections.map
import android.util.Log

class GlobalViewModel(application: Application) : AndroidViewModel(application) {

    private val queue: ArrayDeque<Long> = ArrayDeque<Long>()
    private val history: ArrayDeque<Long> = ArrayDeque<Long>()
    private val repository: SongRepository

    private var _currentlyPlayingSong = MutableStateFlow<Song?>(null)
    val currentlyPlayingSong: StateFlow<Song?> = _currentlyPlayingSong

    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        repository = SongRepository(songDao, application)
    }

    private fun getNextSong() : Long {
        return 1
    }

    fun enqueue(song: Song) {
        queue.add(song.id)
    }

    fun forceChange(song: Song) {
        if (queue.isNotEmpty()) {
            queue.removeFirst()
        }
        Log.d("FORCE CHANGE", "FORCING CHANGE")
        queue.addFirst(song.id)
        _currentlyPlayingSong.value = song
    }

    fun next() {
        viewModelScope.launch {
            if (queue.isNotEmpty()) {
                history.add(queue.first())
                queue.removeFirst()
            }
            if (queue.isEmpty()) {
                var nextSongId = getNextSong()
                queue.add(nextSongId)
            }
            var currentSongId = queue.first()
            var currentSong = repository.getSongById(currentSongId).first()
            _currentlyPlayingSong.value = currentSong?.toSong()
        }
    }

    fun prev() {
        viewModelScope.launch {
            if (history.isEmpty()) {
                history.add(getNextSong())
            }
            var prevSongId = history.last()
            history.removeLast()
            queue.addFirst(prevSongId)
            var currentSong = repository.getSongById(prevSongId).first()
            _currentlyPlayingSong.value = currentSong?.toSong()
        }
    }

    private fun SongEntity.toSong(): Song {
        // TODO: changed later
        val coverResId = this.imagePath ?: R.drawable.starboy.toString()
        return Song(
            id = this.id,
            title = this.title,
            artist = this.artist,
            imagePath = coverResId,
            audioPath = this.audioPath,
            isLiked = this.isLiked,
            duration = this.duration
        )
    }

    class GlobalViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GlobalViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return GlobalViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}