package com.example.purrytify.ui.screen

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.R
import com.example.purrytify.ui.model.ProfileViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.navigation.Screen
import com.example.purrytify.service.baseUrl
import com.example.purrytify.ui.model.GlobalViewModel
import kotlinx.coroutines.launch


@Composable
fun ProfileScreen(
    globalViewModel: GlobalViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val backgroundColor = Color(0xFF121212)
    val secondaryColor = Color(0xFF01667A)
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.ProfileViewModelFactory(context.applicationContext as Application)
    )
    val scope = rememberCoroutineScope()
    val userState by viewModel.userState.collectAsState()
    val songStats by viewModel.songStats.collectAsState()
    val isConnected by globalViewModel.isConnected.collectAsState()

    LaunchedEffect(isConnected) {
        if (isConnected) {
            viewModel.loadUserProfile()
            viewModel.loadSongStats()
        } else {
            viewModel.isLoading = false
        }
    }

    if (viewModel.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        if (isConnected && viewModel.success) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(secondaryColor, backgroundColor),
                            )
                        )
                        .padding(top = 96.dp, bottom = 48.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data("${baseUrl}uploads/profile-picture/${userState.profilePhoto}")
                                    .crossfade(true)
                                    .build(),
                                placeholder = painterResource(R.drawable.starboy),
                                contentDescription = "Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(CircleShape),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = userState.username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = userState.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.logout(onComplete = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true } // Clear backstack
                                    }
                                }, clearUserId = {
                                    globalViewModel.clearUserId()
                                })
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            enabled = !viewModel.isLoggingOut,
                            modifier = Modifier
                                .width(120.dp)
                                .height(36.dp)
                        ) {
                            Text(
                                text = "Log Out",
                                color = Color.White
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(value = songStats.totalSongs.toString(), label = "SONGS")
                    StatItem(value = songStats.likedSongs.toString(), label = "LIKED")
                    StatItem(value = songStats.listenedSongs.toString(), label = "LISTENED")
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        } else {
            NoInternetScreen {
                scope.launch {
                    viewModel.loadUserProfile()
                    viewModel.loadSongStats()
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp
        )
        Text(
            text = label,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NoInternetScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = "No internet",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = "No Internet Connection",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Failed to load your profile. Please check your connection.",
                color = Color.Gray,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text(text = "Retry", color = Color.White)
            }
        }
    }
}
