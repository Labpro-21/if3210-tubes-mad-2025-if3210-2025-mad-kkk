package com.example.purrytify.data.repository

import android.content.Context
import android.net.Uri
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.data.model.Song
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class SongRepository(
    private val songDao: SongDao,
    private val context: Context
) {
    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val likedSongs: Flow<List<SongEntity>> = songDao.getLikedSongs()
    val recentlyPlayedSongs: Flow<List<SongEntity>> = songDao.getRecentlyPlayedSongs()
    val recentlyAddedSongs: Flow<List<SongEntity>> = songDao.getRecentlyAddedSongs()
    val lastPlayedSong: Flow<SongEntity?> = songDao.getLastPlayedSong()

    fun searchAllSongs(query: String): Flow<List<SongEntity>> {
        return songDao.searchAllSongs(query)
    }

    fun searchLikedSongs(query: String): Flow<List<SongEntity>> {
        return songDao.searchAllLikedSongs(query)
    }

    fun getSongById(songId: Long): Flow<SongEntity?> {
        return songDao.getSong(songId)
    }

    fun getNumberOfSong() : Flow<Int> {
        return songDao.getNumberOfSong()
    }

    fun getCountOfListenedSong() : Flow<Int> {
        return songDao.getCountOfListenedSong()
    }

    suspend fun insertSong(
        title: String,
        artist: String,
        imageUri: Uri,
        audioUri: Uri,
        duration: Int
    ): Long {
        val imagePath = saveFileToInternalStorage(imageUri, "images")

        val audioPath = saveFileToInternalStorage(audioUri, "audio")

        val song = SongEntity(
            title = title,
            artist = artist,
            imagePath = imagePath,
            audioPath = audioPath,
            duration = duration
        )

        return songDao.insertSong(song)
    }

    suspend fun updateLikedStatus(songId: Long, isLiked: Boolean) {
        songDao.updateLikedStatus(songId, isLiked)
    }

    suspend fun deleteAllSongs() {
        songDao.deleteAll()
    }

    suspend fun setLastPlayed(songId: Long) {
        songDao.updateLastPlayed(songId, System.currentTimeMillis())
    }

    suspend fun deleteSong(song: SongEntity) {
        deleteFile(song.imagePath)
        deleteFile(song.audioPath)
        songDao.deleteSong(song)
    }

    private fun saveFileToInternalStorage(uri: Uri, folderName: String): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "${UUID.randomUUID()}.${getFileExtension(uri)}"
        val directory = File(context.filesDir, folderName)

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, fileName)
        val outputStream = FileOutputStream(file)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return file.absolutePath
    }

    private fun getFileExtension(uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri) ?: return ""
        return when {
            mimeType.contains("image/jpeg") -> "jpg"
            mimeType.contains("image/png") -> "png"
            mimeType.contains("audio/mpeg") -> "mp3"
            mimeType.contains("audio/wav") -> "wav"
            else -> "dat"
        }
    }

    private fun deleteFile(path: String) {
        try {
            File(path).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}