package com.example.purrytify.service

import retrofit2.http.GET
import retrofit2.http.Path

interface OnlineSongService {
    @GET("api/top-songs/global")
    suspend fun topGlobalSongs(): List<OnlineSongResponse>

    @GET("api/top-songs/{country}")
    suspend fun topCountrySongs(@Path("country") country: String): List<OnlineSongResponse>

    @GET("api/song/{songId}")
    suspend fun songById(@Path("songId") songId: Int): OnlineSongResponse
}

data class OnlineSongResponse (
    val id: Int,
    val title: String,
    val artist: String,
    val artwork: String,
    val url: String,
    val duration: String,
    val country: String,
    val rank: Int,
    val createdAt: String,
    val updatedAt: String
)