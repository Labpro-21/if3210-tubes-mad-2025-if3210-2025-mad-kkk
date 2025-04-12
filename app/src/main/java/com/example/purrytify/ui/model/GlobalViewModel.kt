package com.example.purrytify.ui.model

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.min
import com.example.purrytify.network.NetworkMonitor

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

    private val _userQueue: ArrayDeque<Song> = ArrayDeque<Song>()

    private val _isRepeat = MutableStateFlow<Boolean>(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat

    private val queueSize = 5

    private val A_queue_picker = 1
    private val B_queue_picker = 1
    private val _user_id = MutableStateFlow<Int?>(null)
    val user_id: StateFlow<Int?> =_user_id

    private val networkMonitor = NetworkMonitor(application)
    val isConnected = networkMonitor.isConnected

    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        repository = SongRepository(songDao, application)
        networkMonitor.register()
    }

    fun initializeQueue() {
        viewModelScope.launch {
            if (user_id.value == null) return@launch
            val songSize = repository.getNumberOfSong(userId = user_id.value!!).first()
            val allSongs = repository.allSongs(userId = user_id.value!!).first()
            if (songSize > 0) {
                Log.d("INIT GLOBAL VIEW MODEL", songSize.toString())
                for (i in 1..min(songSize, queueSize)) {
                    val song = allSongs[i - 1].toSong()
                    song.let { _queue.add(song) }
                }
                _queue_list.value = _userQueue.toList() + _queue.toList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkMonitor.unregister()
    }

    fun setUserId(newId: Int) {
        _user_id.value = newId
    }

    fun clearUserId() {
        _user_id.value = null
    }

    fun bindMediaController(controller: MediaController) {
        mediaController = controller
        controller.addListener(mediaListener)
    }


    private fun resumeProgressUpdate() {
        if (progressUpdateJob?.isActive == true) return
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                updatePlaybackState()
                delay(500)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    private fun checkAndUpdateProgress() {
        val isPlaying = mediaController?.isPlaying == true
        val isReady = mediaController?.playbackState == Player.STATE_READY

        if (isPlaying && isReady) {
            resumeProgressUpdate()
        } else {
            stopProgressUpdate()
        }
    }


    private val mediaListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            checkAndUpdateProgress()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            checkAndUpdateProgress()
            if (playbackState == Player.STATE_ENDED) {
                playNextSong(true)
            }
        }
    }

    private var progressUpdateJob: Job? = null

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

    fun toggleLikedStatus() {
        val songId = _currentSong.value?.id ?: return
        val isLiked = _currentSong.value?.isLiked ?: return

        viewModelScope.launch {
            repository.updateLikedStatus(songId, !isLiked)
            _currentSong.value?.let { currentSong ->
                // update current song
                val updatedSong = currentSong.copy(isLiked = !isLiked)
                _currentSong.value = updatedSong

                // update queue
                val updatedQueue = ArrayDeque<Song>()
                for (s in _queue) {
                    if (s.id == songId) {
                        updatedQueue.add(s.copy(isLiked = !isLiked))
                    } else {
                        updatedQueue.add(s)
                    }
                }
                _queue.clear()
                _queue.addAll(updatedQueue)

                // update history
                val updatedHistory = ArrayDeque<Song>()
                for (s in _history) {
                    if (s.id == songId) {
                        updatedHistory.add(s.copy(isLiked = !isLiked))
                    } else {
                        updatedHistory.add(s)
                    }
                }
                _history.clear()
                _history.addAll(updatedHistory)

                val updatedUserQ = ArrayDeque<Song>()
                for (s in _userQueue) {
                    if (s.id == songId) {
                        updatedUserQ.add(s.copy(isLiked = !isLiked))
                    } else {
                        updatedUserQ.add(s)
                    }
                }
                _userQueue.clear()
                _userQueue.addAll(updatedUserQ)
                refreshQueueAndHistoryUI()
            }
        }
    }

    fun logout() {
        mediaController?.stop()
        mediaController?.clearMediaItems()

        _currentSong.value = null
        _isPlaying.value = false
        _currentPosition.value = 0.0
        _duration.value = 0.0

        clearUserId()

        _queue.clear()
        _history.clear()
        _userQueue.clear()
        _queue_list.value = emptyList()
        _history_list.value = emptyList()

        stopProgressUpdate()
    }


    fun playSong(song: Song) {
        currentSong.value?.let {
            addToHistory(it)
        }
        play(song)
        _currentSong.value = song
        refreshQueueAndHistoryUI()
    }

    fun notifyAddSong() {
        viewModelScope.launch {
            if (user_id.value == null) return@launch
            val allSongs = repository.allSongs(userId = user_id.value!!).first()
            if (allSongs.isEmpty()) {
                return@launch
            }
            _queue.addFirst(allSongs[0].toSong())
            Log.d("GLOBAL VIEW MODEL", "ADDED ${allSongs[0].title}")
            if (_queue.size > queueSize) {
                _queue.removeLast()
            }
        }
    }

    fun notifyUpdateSong(song: Song) {
        viewModelScope.launch {
            val updatedQueue = ArrayDeque<Song>()
            for (s in _queue) {
                if (s.id == song.id) {
                    updatedQueue.add(song)
                } else {
                    updatedQueue.add(s)
                }
            }
            _queue.clear()
            _queue.addAll(updatedQueue)

            // update history
            val updatedHistory = ArrayDeque<Song>()
            for (s in _history) {
                if (s.id == song.id) {
                    continue
                } else {
                    updatedHistory.add(s)
                }
            }
            _history.clear()
            _history.addAll(updatedHistory)

            val updatedUserQ = ArrayDeque<Song>()
            for (s in _userQueue) {
                if (s.id == song.id) {
                    updatedUserQ.add(song)
                } else {
                    updatedUserQ.add(s)
                }
            }
            _userQueue.clear()
            _userQueue.addAll(updatedUserQ)
            refreshQueueAndHistoryUI()
        }
    }

    fun notifyDeleteSong(song: Song) {
        viewModelScope.launch {
            val filteredQueue = _queue.filter { it.id != song.id }
            _queue.clear()
            _queue.addAll(filteredQueue)

            val filteredHistory = _history.filter { it.id != song.id }
            _history.clear()
            _history.addAll(filteredHistory)

            val filteredUserQ = _userQueue.filter { it.id != song.id }
            _userQueue.clear()
            _userQueue.addAll(filteredUserQ)

            refreshQueueAndHistoryUI()
        }
    }

    fun notifyLikeSong(song: Song, newVal: Boolean) {
        viewModelScope.launch {
            // update current song
            if (_currentSong.value?.id == song.id) {
                _currentSong.value?.let { currentSong ->
                    val updatedSong = currentSong.copy(isLiked = newVal)
                    _currentSong.value = updatedSong
                }
            }

            // update queue
            val updatedQueue = ArrayDeque<Song>()
            for (s in _queue) {
                if (s.id == song.id) {
                    updatedQueue.add(s.copy(isLiked = newVal))
                } else {
                    updatedQueue.add(s)
                }
            }
            _queue.clear()
            _queue.addAll(updatedQueue)

            // update user history
            val updatedHistory = ArrayDeque<Song>()
            for (s in _history) {
                if (s.id == song.id) {
                    updatedHistory.add(s.copy(isLiked = newVal))
                } else {
                    updatedHistory.add(s)
                }
            }
            _history.clear()
            _history.addAll(updatedHistory)

            // update user queue
            val updatedUserQ = ArrayDeque<Song>()
            for (s in _userQueue) {
                if (s.id == song.id) {
                    updatedUserQ.add(s.copy(isLiked = newVal))
                } else {
                    updatedUserQ.add(s)
                }
            }
            _userQueue.clear()
            _userQueue.addAll(updatedUserQ)
            refreshQueueAndHistoryUI()
        }
    }


    fun addToQueue(song: Song) {
        _userQueue.add(song)
        val userQSize = _userQueue.size
        var systemQSize = _queue.size
        while (userQSize + systemQSize > queueSize && systemQSize > 0) {
            _queue.removeLast()
            systemQSize--
        }
        refreshQueueAndHistoryUI()
    }

    fun addAllToQueue(songs: List<Song>) {
        _userQueue.addAll(songs)
        val userQSize = _userQueue.size
        var systemQSize = _queue.size
        while (userQSize + systemQSize > queueSize && systemQSize > 0) {
            _queue.removeLast()
            systemQSize--
        }
        refreshQueueAndHistoryUI()
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    fun clearQueue() {
        viewModelScope.launch {
            if (user_id.value == null) return@launch
            _userQueue.clear()
            val userQSize = _userQueue.size
            var systemQSize = _queue.size
            val allSongs = repository.allSongs(user_id.value!!).first()
            val songSize = allSongs.size

            var lastIndex = -1
            var iterator = 0

            var lastSong = 1L

            if (_queue.isNotEmpty()) {
                lastSong = _queue.last().id
            }

            for (song in allSongs) {
                if (song.id == lastSong) {
                    lastIndex = iterator
                }
                iterator++
            }

            while (userQSize + systemQSize < queueSize) {
                lastSong = (A_queue_picker * lastSong + B_queue_picker).mod(songSize).toLong()
                val song = allSongs[lastSong.toInt()]
                _queue.add(song.toSong())
                systemQSize++
            }
            refreshQueueAndHistoryUI()
        }
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
        _queue_list.value = _userQueue.toList() + _queue.toList()
        _history_list.value = _history.toList()
    }

    fun playNextSong(auto: Boolean = false) : Long {
        if (auto && _isRepeat.value) {
            val currentSong = _currentSong.value ?: return 0
            playSong(currentSong)
            return currentSong.id
        }

        _isRepeat.value = false
        if (user_id.value == null) return 0

        if (_userQueue.isNotEmpty()) {
            val nextSong = _userQueue.removeFirst()
            var lastSong = nextSong

            if (_queue.isNotEmpty()) {
                lastSong = _queue.last()
            }

            playSong(nextSong)

            viewModelScope.launch {
                val userQSize = _userQueue.size
                if (_queue.size + userQSize < queueSize) {

                    val songSize = repository.getNumberOfSong(user_id.value!!).first()

                    val allSongs = repository.allSongs(user_id.value!!).first()

                    var lastIndex = -1
                    var iterator = 0

                    for (song in allSongs) {
                        if (song.id == lastSong.id) {
                            lastIndex = iterator
                        }
                        iterator++
                    }

                    if (lastIndex == -1) {
                        Log.e("GLOBAL VIEW MODEL", "CURRENT SONG NOT FOUND IN QUEUE")
                        return@launch
                    }

                    var song = allSongs[(A_queue_picker * lastIndex + B_queue_picker).mod(songSize)]
                    song.let { _queue.add(it.toSong()) }

                    Log.d("QUEUE NEXT SONG", song.title)

                    _queue_list.value = _userQueue.toList() + _queue.toList()
                }
            }

            return nextSong.id
        }

        if (_queue.isNotEmpty()) {
            val nextSong = _queue.removeFirst()
            var lastSong = nextSong

            if (_queue.isNotEmpty()) {
                lastSong = _queue.last()
            }

            playSong(nextSong)

            viewModelScope.launch {
                val userQSize = _userQueue.size
                if (_queue.size + userQSize < queueSize) {

                    val songSize = repository.getNumberOfSong(user_id.value!!).first()
                    val allSongs = repository.allSongs(user_id.value!!).first()

                    var lastIndex = -1

                    for ((iterator, song) in allSongs.withIndex()) {
                        if (song.id == lastSong.id) {
                            lastIndex = iterator
                        }
                    }

                    if (lastIndex == -1) {
                        Log.e("GLOBAL VIEW MODEL", "CURRENT SONG NOT FOUND IN QUEUE")
                        return@launch
                    }

                    val song = allSongs[(A_queue_picker * lastIndex + B_queue_picker).mod(songSize)]
                    song.let { _queue.add(it.toSong()) }

                    Log.d("QUEUE NEXT SONG", song.title)

                    _queue_list.value = _userQueue.toList() + _queue.toList()
                }
            }

            return nextSong.id
        } else {
            mediaController?.pause()
            return 0
        }
    }

    fun playPreviousSong() : Long {
        if (_history.isNotEmpty()) {
            val prevSong = _history.removeFirst()
            currentSong.value?.let { _queue.addFirst(it) }
            play(prevSong)
            _currentSong.value = prevSong

            if (_queue.size > queueSize) {
                _queue.removeLast()
            }

            refreshQueueAndHistoryUI()
            return prevSong.id
        } else {
            return 0
        }
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            mediaController?.pause()
            _isPlaying.value = false
        } else {
            if (currentSong == null && _queue.isNotEmpty()) {
                playNextSong()
            } else {
                mediaController?.play()
            }
            _isPlaying.value = true
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
            primaryColor = this.primaryColor,
            secondaryColor = this.secondaryColor,
            userId = this.userId
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