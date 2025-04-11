package com.example.purrytify.data.model

data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val imagePath: String,
    val audioPath: String,
    val isLiked: Boolean = false,
    val primaryColor: Int,
    val secondaryColor: Int,
)
