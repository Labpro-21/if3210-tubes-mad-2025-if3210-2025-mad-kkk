package com.example.purrytify.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private val retrofit = Retrofit.Builder().baseUrl("http://34.101.226.132:3000/")
        .addConverterFactory(GsonConverterFactory.create()).build()

    val authService: AuthService = retrofit.create(AuthService::class.java)
}