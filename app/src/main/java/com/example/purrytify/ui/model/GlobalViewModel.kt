package com.example.purrytify.ui.model

import android.app.Application
import android.content.Context
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.math.roundToInt
import androidx.core.net.toUri

class GlobalViewModel(application: Application) : AndroidViewModel(application) {
    private var mediaController: MediaController? = null
    private val _queue: ArrayDeque<Song> = ArrayDeque<Song>()
    private val _history: ArrayDeque<Song> = ArrayDeque<Song>()
    private val _maxHistorySize = 18
    private val repository: SongRepository

    var currentSong by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var currentPosition by mutableStateOf(0)
        private set
    var duration by mutableStateOf(0)
        private set
    var queue by mutableStateOf<List<Song>>(emptyList())
        private set
    var history by mutableStateOf<List<Song>>(emptyList())
        private set

    // TODO: check if this is needed
    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        repository = SongRepository(songDao, application)
    }

    fun bindMediaController(controller: MediaController) {
        mediaController = controller
        controller.addListener(mediaListener)
    }

    private val mediaListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) {
                // TODO: handle state change
            }
        }
    }
    private var progressUpdateJob: Job? = null


    private fun resumeProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                updatePlaybackState()
                delay(500) // Update every half second
            }
        }
    }

    private fun updatePlaybackState() {
        mediaController?.let { service ->
            isPlaying = service.isPlaying
            currentPosition = service.currentPosition.toDouble().div(1000).roundToInt()
            duration = service.duration.toDouble().div(1000).roundToInt()
        }
    }

    fun play(song: Song) {
        val uri: Uri = getUriFromPath(song.audioPath)
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setDisplayTitle(song.title)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()

        mediaController?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

    fun playSong(song: Song) {
        currentSong?.let {
            addToHistory(it)
        }
        play(song)
        currentSong = song
        refreshQueueAndHistoryUI()
    }

    fun addToQueue(song: Song) {
        _queue.add(song)
        refreshQueueAndHistoryUI()
    }

    fun addAllToQueue(songs: List<Song>) {
        _queue.addAll(songs)
        refreshQueueAndHistoryUI()
    }

    fun clearQueue() {
        _queue.clear()
        refreshQueueAndHistoryUI()
    }

    fun removeFromQueue(song: Song) {
        _queue.remove(song)
        refreshQueueAndHistoryUI()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex in 0 until _queue.size && toIndex in 0 until _queue.size && fromIndex != toIndex) {
            val song = _queue.removeAt(fromIndex)
            _queue.add(toIndex, song)
            refreshQueueAndHistoryUI()
        }
    }

    private fun addToHistory(song: Song) {
        if (_history.isEmpty() || _history.first() != song) {
            _history.addFirst(song)
            while (_history.size > _maxHistorySize) {
                _history.removeLast()
            }
            refreshQueueAndHistoryUI()
        }
    }

    fun clearHistory() {
        _history.clear()
        refreshQueueAndHistoryUI()
    }

    private fun refreshQueueAndHistoryUI() {
        queue = _queue.toList()
        history = _history.toList()
    }

    fun playNextSong() {
        if (_queue.isNotEmpty()) {
            val nextSong = _queue.removeFirst()
            playSong(nextSong)
        } else {
            // No more songs in queue, stop playback or loop
            mediaController?.pause()
        }
    }

    fun playPreviousSong() {
        if (_history.isNotEmpty()) {
            val prevSong = _history.removeFirst()

            // Add current song to the front of the queue
            currentSong?.let { _queue.addFirst(it) }

            // Play the previous song
            play(prevSong)
            currentSong = prevSong

            refreshQueueAndHistoryUI()
        }
    }

    fun togglePlayPause() {
        if (isPlaying) {
            mediaController?.pause()
        } else {
            if (currentSong == null && _queue.isNotEmpty()) {
                playNextSong()
            } else {
                mediaController?.play()
            }
        }
    }

    fun seekTo(position: Int) {
        mediaController?.seekTo(position * 1000L)
    }

    fun playNext(song: Song) {
        _queue.addFirst(song)
        refreshQueueAndHistoryUI()
    }

    private fun SongEntity.toSong(): Song {
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

    fun getUriFromPath(path: String): Uri {
        return if (path.startsWith("content://")) path.toUri()
        else Uri.fromFile(File(path))
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