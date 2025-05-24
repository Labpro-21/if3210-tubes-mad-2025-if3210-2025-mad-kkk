package com.example.purrytify

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.purrytify.navigation.Screen
import com.example.purrytify.service.MediaPlaybackService
import com.example.purrytify.ui.model.AudioDeviceViewModel
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.worker.TOKEN_MONITOR_WORK_TAG
import com.example.purrytify.worker.TokenMonitorWorker
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var globalViewModel: GlobalViewModel
    private lateinit var audioDeviceViewModel: AudioDeviceViewModel
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private lateinit var workManager: WorkManager
    private var startDestination by mutableStateOf<Screen?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        globalViewModel = ViewModelProvider(
            this,
            GlobalViewModel.GlobalViewModelFactory(application)
        )[GlobalViewModel::class.java]

        audioDeviceViewModel = ViewModelProvider(
            this,
            AudioDeviceViewModel.AudioDeviceViewModelFactory(application)
        )[AudioDeviceViewModel::class.java]

        val sessionToken = SessionToken(
            applicationContext,
            ComponentName(applicationContext, MediaPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(applicationContext, sessionToken).buildAsync()
        controllerFuture.addListener({
            val mediaController = controllerFuture.get()
            globalViewModel.bindMediaController(mediaController)
        }, MoreExecutors.directExecutor())

        setupTokenMonitorWorker()
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            startDestination == null
        }
        checkLoginStatus()
        enableEdgeToEdge()
        setContent {
            PurrytifyTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                ) {
                    val windowSize = calculateWindowSizeClass(this)
                    startDestination?.let {
                        PurrytifyApp(
                            windowSize = windowSize.widthSizeClass,
                            globalViewModel,
                            startDestination = it,
                            audioDeviceViewModel = audioDeviceViewModel
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requestNotificationPermissionIfNeeded()
    }

    @SuppressLint("ImplicitSamInstance")
    override fun onDestroy() {
        super.onDestroy()
        MediaController.releaseFuture(controllerFuture)
        workManager.cancelUniqueWork(TOKEN_MONITOR_WORK_TAG)
        stopService(Intent(this, MediaPlaybackService::class.java))
    }

    private fun setupTokenMonitorWorker() {
        val workRequest = PeriodicWorkRequestBuilder<TokenMonitorWorker>(
            5, TimeUnit.MINUTES
        )
            .addTag(TOKEN_MONITOR_WORK_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager = WorkManager.getInstance(this)

        workManager.enqueueUniquePeriodicWork(
            TOKEN_MONITOR_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("PERMISSION", "Notification permission granted")
            } else {
                Toast.makeText(this, "Notification permission is required for downloads", Toast.LENGTH_LONG).show()
            }
        }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme == "purrytify" && data.host == "song") {
            val songId = data.lastPathSegment?.toIntOrNull()
            if (songId != null) {
                globalViewModel.setDeepLinkSongId(songId)
            }
        }
    }


    private fun checkLoginStatus() {
        lifecycleScope.launch {
            val tokenManager = PurrytifyApplication.tokenManager
            val isValidToken = globalViewModel.validateToken(tokenManager)
            if (isValidToken) {
                Log.i("SPLASH_LOG", "Token valid")
                startDestination = Screen.Home
            } else {
                Log.i("SPLASH_LOG", "Token invalid")
                startDestination = Screen.Login
            }
        }
    }
}