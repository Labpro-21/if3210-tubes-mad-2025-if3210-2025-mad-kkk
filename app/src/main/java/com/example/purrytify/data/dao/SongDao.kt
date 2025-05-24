package com.example.purrytify.data.dao

import androidx.room.*
import com.example.purrytify.data.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE userId = :userId ORDER BY id DESC")
    fun getAllSongs(userId: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isLiked = 1 AND userId = :userId ORDER BY id DESC")
    fun getLikedSongs(userId: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE userId = :userId AND isDownloaded = 1 AND (title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%') ORDER BY id DESC")
    fun searchAllSongs(query: String, userId: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE userId = :userId AND isLiked = 1 AND (title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%') ORDER BY id DESC")
    fun searchAllLikedSongs(query: String, userId: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE userId = :userId AND lastPlayed IS NOT NULL ORDER BY lastPlayed DESC LIMIT 12")
    fun getRecentlyPlayedSongs(userId: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE userId = :userId AND isDownloaded = 1 ORDER BY dateAdded DESC LIMIT 8")
    fun getRecentlyAddedSongs(userId: Int): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE userId = :userId AND lastPlayed IS NOT NULL ORDER BY lastPlayed DESC LIMIT 1")
    fun getLastPlayedSong(userId: Int): Flow<SongEntity?>

    @Query("SELECT COUNT(id) FROM songs WHERE userId = :userId AND isDownloaded = 1")
    fun getNumberOfSong(userId: Int): Flow<Int>

    @Query("SELECT COUNT(id) FROM songs WHERE userId = :userId AND lastPlayed is NOT NULL")
    fun getCountOfListenedSong(userId: Int): Flow<Int>

    @Query("SELECT * FROM songs where id = :songId")
    fun getSong(songId: Long): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE serverId IN (:serverIds) AND userId = :userId")
    suspend fun getSongsByServerId(serverIds: List<Int>, userId: Int): List<SongEntity>

    @Query("SELECT * FROM songs WHERE serverId = :serverId AND userId = :userId")
    suspend fun getSongByServerId(serverId: Int, userId: Int): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Update
    suspend fun updateSongs(songs: List<SongEntity>)

    @Transaction
    suspend fun insertAndUpdate(
        toInsert: List<SongEntity>,
        toUpdate: List<SongEntity>,
        serverIds: List<Int>,
        userId: Int
    ): List<SongEntity> {
        insertSongs(toInsert)
        updateSongs(toUpdate)
        return getSongsByServerId(serverIds, userId)
    }

    @Transaction
    suspend fun insertAndGetSong(song: SongEntity, serverId: Int, userId: Int): SongEntity? {
        insertSong(song)
        return getSongByServerId(serverId, userId)
    }

    @Transaction
    suspend fun updateAndGetSong(song: SongEntity, serverId: Int, userId: Int): SongEntity? {
        updateSong(song)
        return getSongByServerId(serverId, userId)
    }

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikedStatus(songId: Long, isLiked: Boolean)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    @Query("UPDATE songs SET lastPlayed = :timestamp WHERE id = :id")
    suspend fun updateLastPlayed(id: Long, timestamp: Long)

    @Query("""
        SELECT * FROM songs
        WHERE artist IN (:artists)
        AND userId = :userId -- Ensure songs belong to the specific user
        AND isLiked = 0 -- Exclude already liked songs
        AND id NOT IN (
            SELECT id FROM song_logs
            WHERE userId = :userId AND at >= :recentThreshold -- Exclude recently played songs
        )
        ORDER BY dateAdded DESC, title ASC -- Prioritize newer songs, then alphabetically
    """)
    suspend fun getCandidateRecommendationSongsByArtists(
        userId: Int,
        artists: List<String>,
        recentThreshold: Long
    ): List<SongEntity>
}