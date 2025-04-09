package com.example.purrytify

import android.app.Application
import com.example.purrytify.data.TokenManager

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
