package com.example.purrytify.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [Index(value = ["serverId", "userId"], unique = true)]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: Int?,  // Nullable
    val title: String,
    val artist: String,
    val imagePath: String,
    val audioPath: String,
    val remoteImagePath: String? = null,
    val remoteAudioPath: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayed: Long? = null,
    val isLiked: Boolean = false,
    val primaryColor: Int,
    val secondaryColor: Int,
    val userId: Int,
    val isDownloaded: Boolean = true,
)