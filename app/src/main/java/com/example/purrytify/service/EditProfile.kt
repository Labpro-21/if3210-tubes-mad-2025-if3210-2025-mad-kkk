package com.example.purrytify.service

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part

interface EditProfile {
    @Multipart
    @PATCH("api/profile")
    suspend fun editProfilePicture(
        @Header("Authorization") token: String,
        @Part profilePhoto : MultipartBody.Part
    )
}