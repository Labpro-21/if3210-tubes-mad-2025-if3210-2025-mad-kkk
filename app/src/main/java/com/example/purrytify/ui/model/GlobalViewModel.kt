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

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0.0)
    val currentPosition: StateFlow<Double> = _currentPosition

    private val _duration = MutableStateFlow(0.0)
    val duration: StateFlow<Double> = _duration

    private val _queue_list = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue_list

    private val _history_list = MutableStateFlow<List<Song>>(emptyList())
    val history: StateFlow<List<Song>> = _history_list


    // TODO: check if this is needed
    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        repository = SongRepository(songDao, application)

        viewModelScope.launch {
            var songSize = repository.getNumberOfSong().first()
            Log.d("INIT GLOBAL VIEW MODEL", songSize.toString())
            for (i in 1..18) {
                var song = repository.getSongById(i.mod(songSize).toLong() + 1).first()?.toSong()
                song?.let { _queue.add(song) }
            }
            _queue_list.value = _queue.toList()
        }
    }

    fun bindMediaController(controller: MediaController) {
        mediaController = controller
        controller.addListener(mediaListener)
    }

    private val mediaListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                resumeProgressUpdate()
            }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY && mediaController?.isPlaying == true) {
                resumeProgressUpdate()
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
            _isPlaying.value = service.isPlaying
            _currentPosition.value = service.currentPosition.toDouble().div(1000)
            _duration.value = service.duration.toDouble().div(1000)
        }
    }

    private fun play(song: Song) {
        val uri: Uri = getUriFromPath(song.audioPath)
        val imageUri: Uri = getUriFromPath(song.imagePath)
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setDisplayTitle(song.title)
                    .setArtworkUri(imageUri)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()

        viewModelScope.launch {
            repository.setLastPlayed(song.id)
        }

        mediaController?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

    fun playSong(song: Song) {
        currentSong.value?.let {
            addToHistory(it)
        }
        play(song)
        _currentSong.value = song
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
        _queue_list.value = _queue.toList()
        _history_list.value = _history.toList()
    }

    fun playNextSong() : Long {
        if (_queue.isNotEmpty()) {
            val nextSong = _queue.removeFirst()
            val lastSong = _queue.last()

            playSong(nextSong)

            viewModelScope.launch {
                var songSize = repository.getNumberOfSong().first()


                var song = repository.getSongById((lastSong.id).mod(songSize).toLong() + 1).first()?.toSong()
                song?.let { _queue.add(song) }

                Log.d("QUEUE NEXT SONG", song?.id.toString())

                _queue_list.value = _queue.toList()
            }

            return nextSong.id
        } else {
            // No more songs in queue, stop playback or loop
            mediaController?.pause()

            return 0
        }
    }

    fun playPreviousSong() : Long {
        if (_history.isNotEmpty()) {
            val prevSong = _history.removeFirst()

            // Add current song to the front of the queue
            currentSong.value?.let { _queue.addFirst(it) }

            // Play the previous song
            play(prevSong)
            _currentSong.value = prevSong

            refreshQueueAndHistoryUI()

            return prevSong.id
        } else {
            return 0
        }
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
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

    private fun getUriFromPath(path: String): Uri {
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