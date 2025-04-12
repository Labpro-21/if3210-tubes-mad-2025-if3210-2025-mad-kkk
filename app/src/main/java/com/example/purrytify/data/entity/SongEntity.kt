package com.example.purrytify.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Duration

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val imagePath: String,
    val audioPath: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayed: Long? = null,
    val isLiked: Boolean = false,
    val primaryColor: Int,
    val secondaryColor: Int,
    val userId: Int
)
