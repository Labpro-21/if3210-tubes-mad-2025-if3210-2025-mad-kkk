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

@Composable
fun LogoutListener(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val logoutReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.purrytify.LOGOUT") {
                    onLogout()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_NOT_EXPORTED
        } else {
            0
        }

        context.registerReceiver(
            logoutReceiver,
            IntentFilter("com.purrytify.LOGOUT"),
            flags
        )

        onDispose {
            context.unregisterReceiver(logoutReceiver)
        }
    }
}
