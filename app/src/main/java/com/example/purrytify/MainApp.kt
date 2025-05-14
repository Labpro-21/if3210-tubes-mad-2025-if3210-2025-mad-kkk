package com.example.purrytify

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.purrytify.data.TokenManager
import com.example.purrytify.worker.TOKEN_MONITOR_WORK_TAG
import com.example.purrytify.worker.TokenMonitorWorker
import java.util.concurrent.TimeUnit

class PurrytifyApplication : Application() {
    companion object {
        lateinit var tokenManager: TokenManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager.getInstance(applicationContext)
    }
}
