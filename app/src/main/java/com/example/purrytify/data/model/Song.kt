package com.example.purrytify.data.model

data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val imagePath: String? = null,
    val audioPath: String? = null,
    val isLiked: Boolean = false
)
