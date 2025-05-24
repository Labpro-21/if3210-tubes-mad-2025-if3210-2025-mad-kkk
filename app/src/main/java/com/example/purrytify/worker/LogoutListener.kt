package com.example.purrytify.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.localbroadcastmanager.content.LocalBroadcastManager

@Composable
fun LogoutListener(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val logoutReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.purrytify.LOGOUT") {
                    onLogout()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(logoutReceiver, IntentFilter("com.example.purrytify.LOGOUT"))

        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(logoutReceiver)
        }
    }
}
