package com.example.purrytify.data.repository

import android.content.Context
import android.net.Uri
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID

class SongRepository(
    private val songDao: SongDao,
    private val context: Context
) {
    fun allSongs(userId: Int): Flow<List<SongEntity>> {
        return songDao.getAllSongs(userId)
    }

    fun likedSongs(userId: Int): Flow<List<SongEntity>> {
        return songDao.getLikedSongs(userId)
    }

    fun recentlyPlayedSongs(userId: Int): Flow<List<SongEntity>> {
        return songDao.getRecentlyPlayedSongs(userId)
    }

    fun recentlyAddedSongs(userId: Int): Flow<List<SongEntity>> {
        return songDao.getRecentlyAddedSongs(userId)
    }

    fun lastPlayedSong(userId: Int): Flow<SongEntity?> {
        return songDao.getLastPlayedSong(userId)
    }

    fun searchAllSongs(query: String, userId: Int): Flow<List<SongEntity>> {
        return songDao.searchAllSongs(query, userId)
    }

    fun searchLikedSongs(query: String, userId: Int): Flow<List<SongEntity>> {
        return songDao.searchAllLikedSongs(query, userId)
    }

    fun getSongById(songId: Long): Flow<SongEntity?> {
        return songDao.getSong(songId)
    }

    fun getNumberOfSong(userId: Int): Flow<Int> {
        return songDao.getNumberOfSong(userId)
    }

    fun getCountOfListenedSong(userId: Int): Flow<Int> {
        return songDao.getCountOfListenedSong(userId)
    }

    suspend fun getSongsByServerId(serverIds: List<Int>, userId: Int): List<SongEntity> {
        return songDao.getSongsByServerId(serverIds, userId)
    }

    suspend fun insertSong(
        title: String,
        artist: String,
        imageUri: Uri,
        audioUri: Uri,
        primaryColor: Int,
        secondaryColor: Int,
        userId: Int,
        serverId: Int? = null,
        isDownloaded: Boolean = true,
    ): Long {
        val imagePath = saveFileToInternalStorage(imageUri, "images/$userId/")
        val audioPath = saveFileToInternalStorage(audioUri, "audio/$userId/")

        val song = SongEntity(
            title = title,
            artist = artist,
            imagePath = imagePath,
            audioPath = audioPath,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            userId = userId,
            serverId = serverId,
            isDownloaded = isDownloaded
        )

        return songDao.insertSong(song)
    }

    suspend fun insertSongs(songs: List<SongEntity>) {
        songDao.insertSongs(songs)
    }

    suspend fun updateSongs(songs: List<SongEntity>) {
        songDao.updateSongs(songs)
    }

    suspend fun insertAndUpdate(
        toInsert: List<SongEntity>,
        toUpdate: List<SongEntity>,
        serverIds: List<Int>,
        userId: Int
    ): List<SongEntity> {
        return songDao.insertAndUpdate(toInsert, toUpdate, serverIds, userId)
    }

    suspend fun updateSongById(
        id: Long,
        title: String,
        artist: String,
        imageUri: String,
        audioUri: String,
        primaryColor: Int,
        secondaryColor: Int,
        userId: Int,
        isLiked: Boolean = false,
        lastPlayed: Long? = null,
        serverId: Int? = null,
        isDownloaded: Boolean = false,
    ) {
        val song = SongEntity(
            id = id,
            title = title,
            artist = artist,
            imagePath = imageUri,
            audioPath = audioUri,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            isLiked = isLiked,
            userId = userId,
            lastPlayed = lastPlayed,
            serverId = serverId,
            isDownloaded = isDownloaded
        )
        songDao.updateSong(song)
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
        songDao.deleteSong(song)
        deleteFile(song.imagePath)
        deleteFile(song.audioPath)
    }

    fun saveThumbnail(uri: Uri, userId: Int): String {
        val imagePath = saveFileToInternalStorage(uri, "images/${userId}/")
        return imagePath
    }

    fun saveAudio(uri: Uri, userId: Int): String {
        val audioPath = saveFileToInternalStorage(uri, "audio/${userId}/")
        return audioPath
    }

    private fun saveFileToInternalStorage(uri: Uri, folderName: String): String {
        val fileName = "${UUID.randomUUID()}.${getFileExtension(uri)}"
        val directory = File(context.filesDir, folderName)

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, fileName)

        val outputStream = FileOutputStream(file)

        try {
            val inputStream = when (uri.scheme) {
                "http", "https" -> URL(uri.toString()).openStream()
                "content", "file" -> context.contentResolver.openInputStream(uri)
                else -> null
            }

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalArgumentException("Unsupported URI scheme: ${uri.scheme}")

            return file.absolutePath
        } catch (e: Exception) {
            file.delete()
            throw e
        }
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

    fun deleteFile(path: String) {
        try {
            File(path).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}