package com.example.purrytify.ui.model

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.data.model.Song
import com.example.purrytify.data.repository.SongLogsRepository
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.network.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.min

class GlobalViewModel(application: Application) : AndroidViewModel(application) {
    // current user
    private val _userId = MutableStateFlow<Int?>(null)
    val userId: StateFlow<Int?> = _userId
    private val _userLocation = MutableStateFlow<String>("");
    val userLocation: StateFlow<String> = _userLocation

    // media player related variables
    private var mediaController: MediaController? = null
    private var _queue = MutableStateFlow<ArrayList<Song>>(ArrayList())
    val queue: StateFlow<ArrayList<Song>> = _queue
    private val repository: SongRepository;
    private val songLogsRepository: SongLogsRepository
    private val randomQueueSize = 10;
    private var progressUpdateJob: Job? = null

    // current song
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong
    private val _currentIdx = MutableStateFlow<Int>(0)
    val currIdx: StateFlow<Int> = _currentIdx

    // is song playing
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    // current song duration
    private val _currentPosition = MutableStateFlow(0.0)
    val currentPosition: StateFlow<Double> = _currentPosition

    // max duration
    private val _duration = MutableStateFlow(0.0)
    val duration: StateFlow<Double> = _duration

    // network monitor
    private val networkMonitor = NetworkMonitor(application)
    val isConnected = networkMonitor.isConnected

    // repeat
    private val _isRepeat = MutableStateFlow(0)
    val isRepeat: StateFlow<Int> = _isRepeat

    // shuffle
    private val _shuffled = MutableStateFlow(false)
    val shuffled = _shuffled
    
    private var lastTimePlayed: Long

    // constructor
    init {
        val songDao = SongDatabase.getDatabase(application).songDao()
        val songLogsDao = SongDatabase.getDatabase(application).songLogsDao()
        lastTimePlayed = System.currentTimeMillis()
        repository = SongRepository(songDao, application)
        songLogsRepository = SongLogsRepository(songLogsDao, application)
        networkMonitor.register()
    }

    // destructor onCleared
    override fun onCleared() {
        super.onCleared()
        networkMonitor.unregister()
    }

    // user related functions
    fun setUserId(newId: Int) {
        _userId.value = newId
    }

    fun clearUserId() {
        _userId.value = null
    }

    fun setUserLocation(newLocation: String) {
        _userLocation.value = newLocation;
    }

    fun clearUserLocation() {
        _userLocation.value = "";
    }

    fun logout() {
        mediaController?.stop()
        mediaController?.clearMediaItems()

        _currentIdx.value = 0
        _currentSong.value = null
        _isPlaying.value = false
        _currentPosition.value = 0.0
        _duration.value = 0.0
        _isRepeat.value = 0
        _shuffled.value = false
        mediaController?.apply {
            shuffleModeEnabled = false
            repeatMode = Player.REPEAT_MODE_OFF
        }

        clearUserId()
        clearUserLocation()
        _queue.value = ArrayList()
        stopProgressUpdate()
    }

    // media controller functions
    fun bindMediaController(controller: MediaController) {
        mediaController = controller
        controller.addListener(mediaListener)
        mediaController?.apply {
            shuffleModeEnabled = false
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    private val mediaListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            checkAndUpdateProgress()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            checkAndUpdateProgress()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            Log.d("MEDIA TRANSITION", "This is called " + reason.toString())

            if (mediaItem != null) {
                mediaController?.apply {
                    viewModelScope.launch {
                        if (userId.value !== null && _currentSong.value !== null) {
                            var temp_dur = System.currentTimeMillis() - lastTimePlayed
                            temp_dur = Math.max(temp_dur, 0)
                            Log.d("MEDIA TRANSITION", "INSERTED " + _currentSong.value!!.title + " " + temp_dur.toString())
                            songLogsRepository.insertLog(
                                _currentSong.value!!.id,
                                userId.value!!,
                                temp_dur.div(1000).toInt(),
                                System.currentTimeMillis()
                            )
                            lastTimePlayed = System.currentTimeMillis()
                        }
                        _currentIdx.value = currentMediaItemIndex
                        if (_currentIdx.value < _queue.value.size) {
                            _currentSong.value = _queue.value[_currentIdx.value]
                        }
                    }
                }
            }


            checkAndUpdateProgress()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                Log.d("MEDIA TRANSITION", "onEvents fallback called")
            }
        }
    }

    // media player indicator job
    private fun checkAndUpdateProgress() {
        val isPlaying = mediaController?.isPlaying == true
        val isReady = mediaController?.playbackState == Player.STATE_READY

        if (isPlaying && isReady) {
            resumeProgressUpdate()
        } else {
            stopProgressUpdate()
        }
    }

    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
        _isPlaying.value = false
    }

    private fun resumeProgressUpdate() {
        if (progressUpdateJob?.isActive == true) return
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                updatePlaybackState()
                delay(1000)
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

    // song playing functions
    private fun setLastPlayed(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setLastPlayed(song.id)
        }
    }

    fun playSong(song: Song) {
        if (userId.value == null) return
        mediaController?.clearMediaItems()

        val newQueue = arrayListOf(song)
        _queue.value = newQueue

        mediaController?.apply {
            setMediaItem(song.toMediaItem())
            prepare()
            play()
        }
        setLastPlayed(song)
    }

    fun playSongs(song: Song) {
        if (userId.value == null) return

        // clear media items
        mediaController?.clearMediaItems()

        // add multiple song, starting from current song
        viewModelScope.launch {
            val allSongs = withContext(Dispatchers.IO) {
                repository.allSongs(userId = userId.value!!).first()
            }
            val songSize = allSongs.size

            var idx = 0
            while (idx < songSize) {
                if (allSongs[idx].id == song.id) break
                idx += 1
            }

            // initialize list of mediaItems
            val newQueue = arrayListOf(song)
            val newMediaItems = mutableListOf(song.toMediaItem());

            for (i in 1..min(songSize, randomQueueSize)) {
                val song2 = allSongs[(i + idx).mod(songSize)].toSong()
                song2.let {
                    newQueue.add(song2)
                    newMediaItems.add(song2.toMediaItem())
                }
            }
            _queue.value = newQueue
            mediaController?.apply {
                addMediaItems(newMediaItems)
                prepare()
                play()
            }

            withContext(Dispatchers.IO) {
                repository.setLastPlayed(song.id)
            }
        }
    }

    fun playNextSong() {
        if (userId.value == null) return
        mediaController?.seekToNextMediaItem()
    }

    fun playPreviousSong() {
        if (userId.value == null) return
        if (_currentSong.value != null && _currentPosition.value <= 1) {
            mediaController?.seekToPreviousMediaItem()
        } else {
            mediaController?.seekTo(0)
            _currentPosition.value = 0.0
        }
    }

    fun playSongIndex(idx: Int) {
        if (userId.value == null) return
        mediaController?.seekTo(idx, 0)
    }

    // play pause seek drag
    fun togglePlayPause() {
        if (isPlaying.value) {
            mediaController?.pause()
            _isPlaying.value = false
        } else {
            mediaController?.play()
            _isPlaying.value = true
        }
    }

    fun dragTo(position: Int) {
        if (mediaController?.isPlaying == true) {
            mediaController?.pause()
        }
        _currentPosition.value = position.toDouble()
    }

    fun seekTo(position: Int) {
        mediaController?.seekTo(position * 1000L)
        mediaController?.play()
        _currentPosition.value = position.toDouble()
    }

    // shuffle and repeat
    fun shuffle() {
        _shuffled.value = !_shuffled.value
        mediaController?.shuffleModeEnabled = _shuffled.value
    }

    fun repeat() {
        _isRepeat.value = (isRepeat.value + 1) % 3
        mediaController?.repeatMode = _isRepeat.value
        Log.d("REPEAT_MODE", mediaController?.repeatMode.toString())
    }

    // queue
    fun addToQueue(song: Song) {
        val newList = _queue.value.toMutableList()
        newList.add(song)
        _queue.value = ArrayList(newList)
        mediaController?.addMediaItem(song.toMediaItem())
    }

    fun addToNext(song: Song) {
        mediaController?.apply {
            val nextIndex = currentMediaItemIndex + 1
            addMediaItem(nextIndex, song.toMediaItem())
        }

        val nextIndex = (mediaController?.currentMediaItemIndex ?: -1) + 1
        val newList = _queue.value.toMutableList()
        if (nextIndex in 0..newList.size) {
            newList.add(nextIndex, song)
            _queue.value = ArrayList(newList)
        }
    }

    fun moveQueue(from: Int, to: Int) {
        if (from !in _queue.value.indices || to !in _queue.value.indices) return

        val newList = _queue.value.toMutableList()
        val song = newList.removeAt(from)
        newList.add(to, song)
        _queue.value = ArrayList(newList)
        mediaController?.moveMediaItem(from, to)
        _currentIdx.value = mediaController?.currentMediaItemIndex ?: 0
    }

    // like
    fun toggleLikedStatus() {
        val songId = _currentSong.value?.id ?: return
        val isLiked = _currentSong.value?.isLiked ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateLikedStatus(songId, !isLiked)
            }
            _currentSong.value?.let { currentSong ->
                val updatedSong = currentSong.copy(isLiked = !isLiked)
                _currentSong.value = updatedSong

                val updatedQueue = ArrayList<Song>()
                for (s in _queue.value) {
                    if (s.id == songId) {
                        updatedQueue.add(s.copy(isLiked = !isLiked))
                    } else {
                        updatedQueue.add(s)
                    }
                }
                _queue.value = updatedQueue
            }
        }
    }

    fun notifyUpdateSong(updatedSong: Song) {
        viewModelScope.launch {
            val oldList = _queue.value
            val newList = oldList.map { if (it.id == updatedSong.id) updatedSong.copy() else it }

            _queue.value = ArrayList(newList)

            mediaController?.let { controller ->
                newList.forEachIndexed { index, song ->
                    if (song.id == updatedSong.id) {
                        controller.replaceMediaItem(index, song.toMediaItem())
                    }
                }
            }
        }
    }

    fun notifyDeleteSong(song: Song) {
        viewModelScope.launch {
            val oldList = _queue.value
            val newList = oldList.toMutableList()

            val indicesToRemove = newList.mapIndexedNotNull { index, s -> if (s.id == song.id) index else null }

            if (indicesToRemove.isEmpty()) return@launch

            for (i in indicesToRemove.reversed()) {
                newList.removeAt(i)
                mediaController?.removeMediaItem(i)
            }

            _queue.value = ArrayList(newList)
        }
    }

    fun notifyLikeSong(song: Song, newVal: Boolean) {
        _currentSong.value?.takeIf { it.id == song.id }?.let {
            _currentSong.value = it.copy(isLiked = newVal)
        }

        val updatedQueue = _queue.value.map { s ->
            if (s.id == song.id) s.copy(isLiked = newVal) else s
        }
        _queue.value = ArrayList(updatedQueue)
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
        return if (path.startsWith("content://")) path.toUri()
        else Uri.fromFile(File(path))
    }

    private fun Song.toMediaItem(): MediaItem {
        val uri: Uri = getUriFromPath(audioPath)
        val imageUri: Uri = getUriFromPath(imagePath)
        val mediaItem = MediaItem.Builder()
            .setMediaId("${id}_${UUID.randomUUID()}")
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setDisplayTitle(title)
                    .setArtworkUri(imageUri)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()
        return mediaItem
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