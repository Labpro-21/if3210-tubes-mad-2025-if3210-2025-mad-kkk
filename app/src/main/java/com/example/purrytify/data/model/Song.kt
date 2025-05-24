package com.example.purrytify.data.model

data class Song(
    val id: Long = 0,
    val serverId: Int?,
    val title: String,
    val artist: String,
    val imagePath: String,
    val audioPath: String,
    val remoteImagePath: String? = null,
    val remoteAudioPath: String? = null,
    val isLiked: Boolean = false,
    val lastPlayed: Long? = null,
    val primaryColor: Int,
    val secondaryColor: Int,
    val userId: Int,
    val isDownloaded: Boolean = false,
)
