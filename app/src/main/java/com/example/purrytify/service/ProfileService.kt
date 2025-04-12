package com.example.purrytify.service

import retrofit2.http.GET
import retrofit2.http.Header

interface ProfileService {

    @GET("api/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Profile

}

data class Profile(
    val id: Int = 1,
    val username: String = "13522xxx",
    val email: String = "13522xxx@std.stei.itb.ac.id",
    val profilePhoto: String = "None",
    val location: String = "ID",
    val createdAt: String = "None",
    val updatedAt: String = "None"
)