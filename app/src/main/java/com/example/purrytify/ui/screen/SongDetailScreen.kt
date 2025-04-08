package com.example.purrytify.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun SongDetailScreen(songId: String, navController: NavHostController, modifier: Modifier = Modifier) {
    Column (modifier = modifier.padding(16.dp).fillMaxSize()) {
        Text("Song Detail", style = TextStyle(fontSize = 36.sp))
        Spacer(Modifier.height(8.dp))
        Text("Now Playing: Song ID $songId", style = TextStyle(fontSize = 28.sp))
        // Add more: Cover image, play/pause, etc.
    }
}
