package com.example.purrytify.worker

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.purrytify.data.TokenManager
import com.example.purrytify.service.ApiClient
import com.example.purrytify.service.RefreshRequest

const val TOKEN_MONITOR_WORK_TAG = "token_monitor_worker"

class TokenMonitorWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val tokenManager = TokenManager.getInstance(appContext)

    override suspend fun doWork(): Result {
        val accessToken = tokenManager.getAccessToken()

        if (accessToken != null) {
            val isValid = try {
                val response = ApiClient.authService.validate("Bearer $accessToken")
                response.valid
            } catch (e: Exception) {
                false
            }

            if (!isValid) {
                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken != null) {
                    try {
                        val response = ApiClient.authService.refresh(RefreshRequest(refreshToken))
                        tokenManager.saveAccessToken(response.accessToken)
                        tokenManager.saveRefreshToken(response.refreshToken)
                        return Result.success()
                    } catch (e: Exception) {
                        tokenManager.clearTokens()
                        sendLogoutBroadcast()
                        return Result.failure()
                    }
                } else {
                    tokenManager.clearTokens()
                    sendLogoutBroadcast()
                    return Result.failure()
                }
            }
        }

        return Result.success()
    }

    private fun sendLogoutBroadcast() {
        val intent = Intent("com.purrytify.LOGOUT")
        applicationContext.sendBroadcast(intent)
    }
}

