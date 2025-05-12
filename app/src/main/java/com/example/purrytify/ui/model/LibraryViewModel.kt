package com.example.purrytify.ui.model

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.data.model.Song
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.ui.util.extractColorsFromImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LibraryViewModel(application: Application, private val globalViewModel: GlobalViewModel) :
    AndroidViewModel(application) {

    private val repository: SongRepository

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private var _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private var _selectedAudioUri = MutableStateFlow<Uri?>(null)
    val selectedAudioUri: StateFlow<Uri?> = _selectedAudioUri

    val songs: StateFlow<List<Song>>

    init {
        val database = SongDatabase.getDatabase(application)
        repository = SongRepository(database.songDao(), application)
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        songs = combine(
            globalViewModel.userId.filterNotNull(),
            _filterType,
            _searchQuery.debounce(300)
        ) { userId, filterType, query ->
            Triple(userId, filterType, query.trim())
        }.flatMapLatest { (userId, filterType, query) ->
            val flow = when (filterType) {
                FilterType.ALL -> repository.searchAllSongs(query, userId)
                FilterType.LIKED -> repository.searchLikedSongs(query, userId)
            }
            flow.map { entities -> entities.map { it.toSong() } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setFilter(filterType: FilterType) {
        _filterType.value = filterType
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun setSelectedAudioUri(uri: Uri?) {
        _selectedAudioUri.value = uri
    }

    fun uploadSong(title: String, artist: String) {
        viewModelScope.launch {
            val userId = globalViewModel.userId.value!!
            val context = getApplication<Application>().applicationContext

            withContext(Dispatchers.IO) {
                val colors = extractColorsFromImage(context, _selectedImageUri.value)
                val primaryColor = colors[0].toArgb()
                val secondaryColor = colors[1].toArgb()

                repository.insertSong(
                    title = title,
                    artist = artist,
                    imageUri = _selectedImageUri.value!!,
                    audioUri = _selectedAudioUri.value!!,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    userId = userId
                )
            }

            _selectedImageUri.value = null
            _selectedAudioUri.value = null
        }
    }


    fun getTitleFromFile(): String? {
        if (selectedAudioUri.value == null) return null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(getApplication(), selectedAudioUri.value)
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        } catch (e: Exception) {
            return ""
        } finally {
            retriever.release()
        }
    }

    fun getArtistFromFile(): String? {
        if (selectedAudioUri.value == null) return null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(getApplication(), selectedAudioUri.value)
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        } catch (e: Exception) {
            return ""
        } finally {
            retriever.release()
        }
    }

    fun toggleLiked(song: Song) {
        val newVal = !song.isLiked
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateLikedStatus(song.id, newVal)
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
            isDownloaded = this.isDownloaded,
            serverId = this.serverId
        )
    }

    private fun Song.toEntity(): SongEntity? {
        return if (this.id > 0) {
            SongEntity(
                id = this.id,
                title = this.title,
                artist = this.artist,
                imagePath = this.imagePath,
                audioPath = this.audioPath,
                isLiked = this.isLiked,
                primaryColor = this.primaryColor,
                secondaryColor = this.secondaryColor,
                userId = this.userId,
                isDownloaded = this.isDownloaded,
                serverId = this.serverId
            )
        } else null
    }

    fun updateSong(id: Long, title: String, artist: String, uriImage: Uri?, uriAudio: Uri?) {
        viewModelScope.launch {
            val userId = globalViewModel.userId.value ?: return@launch
            val context = getApplication<Application>().applicationContext
            val songEntity = repository.getSongById(id).first() ?: return@launch

            var thumbnail = songEntity.imagePath
            var audio = songEntity.audioPath
            var primaryColor = songEntity.primaryColor
            var secondaryColor = songEntity.secondaryColor

            val prevThumbnail = thumbnail
            val prevAudio = audio

            val imageJob = async(Dispatchers.IO) {
                uriImage?.let {
                    val imagePath = repository.saveThumbnail(it, userId)
                    val colors = extractColorsFromImage(context, it)
                    thumbnail = imagePath
                    primaryColor = colors[0].toArgb()
                    secondaryColor = colors[1].toArgb()
                }
            }

            val audioJob = async(Dispatchers.IO) {
                uriAudio?.let {
                    audio = repository.saveAudio(it, userId)
                }
            }
            imageJob.await()
            audioJob.await()

            repository.updateSongById(
                id = id,
                title = title,
                artist = artist,
                imageUri = thumbnail,
                audioUri = audio,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                isLiked = false,
                userId = userId,
                lastPlayed = songEntity.lastPlayed,
                serverId = songEntity.serverId,
                isDownloaded = songEntity.isDownloaded
            )

            globalViewModel.notifyUpdateSong(
                Song(
                    id = id,
                    title = title,
                    artist = artist,
                    imagePath = thumbnail,
                    audioPath = audio,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    isLiked = false,
                    userId = userId,
                    lastPlayed = songEntity.lastPlayed,
                    serverId = songEntity.serverId,
                    isDownloaded = songEntity.isDownloaded
                )
            )

            withContext(Dispatchers.IO) {
                if (prevThumbnail != thumbnail) repository.deleteFile(prevThumbnail)
                if (prevAudio != audio) repository.deleteFile(prevAudio)
            }
        }
    }


    fun deleteSong(song: Song) {
        viewModelScope.launch {
            val songEntity: SongEntity = song.toEntity() ?: return@launch
            withContext(Dispatchers.IO) {
                repository.deleteSong(songEntity)
            }
            globalViewModel.notifyDeleteSong(song)
        }
    }

    enum class FilterType {
        ALL, LIKED
    }

    class LibraryViewModelFactory(
        private val application: Application,
        private val globalViewModel: GlobalViewModel
    ) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LibraryViewModel(application, globalViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}