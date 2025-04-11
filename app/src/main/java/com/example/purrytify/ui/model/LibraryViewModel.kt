package com.example.purrytify.ui.model

import android.app.Application
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.example.purrytify.R
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.data.repository.SongRepository
import com.example.purrytify.data.model.Song
import com.example.purrytify.ui.util.extractColorsFromImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LibraryViewModel(application: Application, private val globalViewModel: GlobalViewModel) : AndroidViewModel(application) {

    private val repository: SongRepository

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private var _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private var _selectedAudioUri = MutableStateFlow<Uri?>(null)
    val selectedAudioUri: StateFlow<Uri?> = _selectedAudioUri

    val songs: Flow<List<Song>>

    init {
        val database = SongDatabase.getDatabase(application)
        repository = SongRepository(database.songDao(), application)
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        songs = combine(_filterType, _searchQuery) { filterType, query ->
            filterType to query.trim()
        }.flatMapLatest { (filterType, query) ->
            val flow = when (filterType) {
                FilterType.ALL -> repository.searchAllSongs(query)
                FilterType.LIKED -> repository.searchLikedSongs(query)
            }
            flow.map { entities -> entities.map { it.toSong() } }
        }
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
                    secondaryColor = secondaryColor
                )
            }

            _selectedImageUri.value = null
            _selectedAudioUri.value = null

            globalViewModel.notifyAddSong()
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

    fun getDurationFromFile(): Int {
        if (selectedAudioUri.value == null) return 0
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(getApplication(), selectedAudioUri.value)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            return (durationMs / 1000).toInt()
        } catch (e: Exception) {
            return 0
        } finally {
            retriever.release()
        }
    }

    fun toggleLiked(song: Song) {
        viewModelScope.launch {
            repository.updateLikedStatus(song.id, !song.isLiked)
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            song.toEntity()?.let { repository.deleteSong(it) }
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
            secondaryColor = this.secondaryColor
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
                secondaryColor = this.secondaryColor
            )
        } else null
    }

    fun updateSong(id: Long, title: String, artist: String, uriImage: Uri?, uriAudio: Uri?) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            var songEntity: SongEntity = repository.getSongById(id).first() ?: return@launch
            var thumbnail = songEntity.imagePath
            var audio = songEntity.audioPath
            var primaryColor = songEntity.primaryColor
            var secondaryColor = songEntity.secondaryColor

            if (uriImage != null) {
                withContext(Dispatchers.IO) {
                    val imagePath = repository.saveThumbnail(uriImage)

                    thumbnail = imagePath

                    val colors = extractColorsFromImage(context, uriImage)
                    primaryColor = colors[0].toArgb()
                    secondaryColor = colors[1].toArgb()

                }
            }

            if (uriAudio != null) {
                withContext(Dispatchers.IO) {
                    val audioPath = repository.saveAudio(uriAudio)
                    audio = audioPath
                }
            }

            repository.updateSongById(
                id = id,
                title = title,
                artist = artist,
                imageUri = thumbnail,
                audioUri = audio,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor
            )

            val updatedSong = Song(
                id = id,
                title = title,
                artist = artist,
                imagePath = thumbnail,
                audioPath = audio,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                isLiked = false
            )

            globalViewModel.notifyUpdateSong(updatedSong)

        }
    }

    enum class FilterType {
        ALL, LIKED
    }

    class LibraryViewModelFactory(private val application: Application, private val globalViewModel: GlobalViewModel) :
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