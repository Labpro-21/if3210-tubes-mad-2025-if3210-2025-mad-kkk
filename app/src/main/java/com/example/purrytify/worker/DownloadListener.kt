package com.example.purrytify.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.purrytify.ui.model.GlobalViewModel

@Composable
fun DownloadListener(
    globalViewModel: GlobalViewModel
) {
    val context = LocalContext.current
    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                Log.d("DOWNLOAD_SERVICE", "Receive something")
                if (intent?.action == "com.example.purrytify.DOWNLOAD_TOP_GLOBAL_COMPLETE") {
                    Log.d("DOWNLOAD_SERVICE", "Receiving Top Global Broadcast")
                    globalViewModel.onTopGlobalDownloadComplete()
                }

                if (intent?.action == "com.example.purrytify.DOWNLOAD_TOP_COUNTRY_COMPLETE") {
                    Log.d("DOWNLOAD_SERVICE", "Receiving Top Country Broadcast")
                    globalViewModel.onTopCountryDownloadComplete()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        Log.d("DOWNLOAD_SERVICE", "registering receiver")
        val filter = IntentFilter().apply {
            addAction("com.example.purrytify.DOWNLOAD_TOP_GLOBAL_COMPLETE")
            addAction("com.example.purrytify.DOWNLOAD_TOP_COUNTRY_COMPLETE")
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        Log.d("DOWNLOAD_SERVICE", "Local broadcast receiver registered")

        onDispose {
            Log.d("DOWNLOAD_SERVICE", "Unregistering local broadcast receiver")
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
    }
}