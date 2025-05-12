package com.example.purrytify.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.purrytify.R
import com.example.purrytify.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) {
        delay(500)
        navController.navigate(Screen.Login.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }
    Box(modifier
        .fillMaxSize()
        .background(Color.Black), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.logo_3),
            contentDescription = "Logo",
            modifier = Modifier.size(180.dp)
        )
    }
}
