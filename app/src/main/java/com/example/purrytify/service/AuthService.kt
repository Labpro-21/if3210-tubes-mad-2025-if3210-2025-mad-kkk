package com.example.purrytify.service

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/verify-token")
    suspend fun validate(@Header("Authorization") token: String): ValidateResponse

    @POST("api/refresh-token")
    suspend fun refresh(@Body request: RefreshRequest): LoginResponse
}

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val accessToken: String, val refreshToken: String)

data class RefreshRequest(val refreshToken: String)
data class ValidateResponse(
    val valid: Boolean,
    val user: User
)

data class User(
    val id: Int,
    val username: String
)
