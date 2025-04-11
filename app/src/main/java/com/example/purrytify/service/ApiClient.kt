package com.example.purrytify.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val baseUrl = "http://34.101.226.132:3000/"

object ApiClient {
    private val retrofit = Retrofit.Builder().baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create()).build()

    val authService: AuthService = retrofit.create(AuthService::class.java)
    val profileService: ProfileService = retrofit.create(ProfileService::class.java)

}