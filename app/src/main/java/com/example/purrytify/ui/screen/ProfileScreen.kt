package com.example.purrytify.ui.screen

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.purrytify.R
import com.example.purrytify.navigation.Screen
import com.example.purrytify.service.baseUrl
import com.example.purrytify.ui.component.NoInternetScreen
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.LoadImage
import com.example.purrytify.ui.model.ListeningStreak
import com.example.purrytify.ui.model.MonthlySoundCapsule
import com.example.purrytify.ui.model.ProfileViewModel
import com.example.purrytify.worker.LogoutListener
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date


@Composable
fun ProfileScreen(
    globalViewModel: GlobalViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val secondaryColor = Color(0xFF01667A)
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.ProfileViewModelFactory(context.applicationContext as Application)
    )
    val scope = rememberCoroutineScope()
    val userState by viewModel.userState.collectAsState()
    val songStats by viewModel.songStats.collectAsState()
    val isConnected by globalViewModel.isConnected.collectAsState()

    val monthlyCapsules by viewModel.monthlyCapsules.collectAsState()
    val streaks by viewModel.streaks.collectAsState()

    LogoutListener {
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
        globalViewModel.clearUserId()
        globalViewModel.logout()
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            viewModel.loadUserProfile(
                onLogout = {
                    viewModel.logout(onComplete = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        globalViewModel.clearUserId()
                        globalViewModel.logout()
                    })
                },
                onSuccess = {
                    viewModel.loadSongStats()
                    viewModel.loadSoundCapsules()
                }
            )
        } else {
            viewModel.isLoading = false
        }
    }


    if (viewModel.isLoading || userState == null || songStats == null) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        if (isConnected && viewModel.success) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    secondaryColor,
                                    MaterialTheme.colorScheme.background
                                ),
                            )
                        )
                        .padding(top = 48.dp, bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            LoadImage(
                                "${baseUrl}uploads/profile-picture/${userState!!.profilePhoto}",
                                contentDescription = "Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = userState!!.username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = userState!!.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.logout(onComplete = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                    globalViewModel.clearUserId()
                                    globalViewModel.logout()
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
                    StatItem(value = songStats!!.totalSongs.toString(), label = "SONGS")
                    StatItem(value = songStats!!.likedSongs.toString(), label = "LIKED")
                    StatItem(value = songStats!!.listenedSongs.toString(), label = "LISTENED")
                }

                // Monthly Sound Capsules
                if (monthlyCapsules.isNotEmpty()) {
                    if (monthlyCapsules.size != streaks.size) {
                        Log.d("FATAL", "MONTHLY CAPSULED AND STREAKS DOES NOT HAVE THE SAME SIZE")
                    }
                    for (i in 0 until monthlyCapsules.size) {
                        MonthlySoundCapsuleSection(
                            capsule = monthlyCapsules[i],
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            onClickArtist = {
                                navController.navigate(Screen.Profile.TopArtist.route)
                            },
                            onClickSong = {
                                navController.navigate(Screen.Profile.TopSong.route)
                            }
                        )
                        if (streaks[i] != null) {
                            ListeningStreakItem(
                                streak = streaks[i]!!,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Listening Streaks
//                if (streaks.isNotEmpty()) {
//                    streaks.take(3).forEach { streak ->
//                        ListeningStreakItem(
//                            streak = streak,
//                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
//                        )
//                    }
//                }

//                Spacer(modifier = Modifier.height(80.dp))
                Spacer(modifier = Modifier.weight(1f))
            }
        } else {
            NoInternetScreen {
                scope.launch {
                    viewModel.loadUserProfile(
                        onLogout = {
                            viewModel.logout(onComplete = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                                globalViewModel.clearUserId()
                                globalViewModel.logout()
                            })
                        },
                        onSuccess = {
                            viewModel.loadSongStats()
                        }
                    )
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
fun MonthlySoundCapsuleSection(
    capsule: MonthlySoundCapsule,
    modifier: Modifier = Modifier,
    onClickArtist: () -> Unit,
    onClickSong: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121212))
    ) {
// Header with month and share icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = capsule.month,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_share), // Make sure to have this icon
                contentDescription = "Share",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Time Listened Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E1E))
                .padding(vertical = 12.dp)
        ) {
            Column (modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Time listened",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "${capsule.totalListeningMinutes} minutes",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top Artist and Top Song Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                // Top Artist
                Column (modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClickArtist )) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = "See More",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Artist Image
                        LoadImage(
                            capsule.topArtist?.imagePath ?: R.drawable.starboy.toString(),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = capsule.topArtist?.name ?: "No Data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4A90E2),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Top Song
            Box(
                modifier = Modifier
                    .weight(1f) // Added weight to ensure equal sizing
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(vertical = 12.dp, horizontal = 16.dp) // Added padding inside box
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClickSong )) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top song",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = "See More",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Song Image
                        LoadImage(
                            capsule.topSong?.imagePath ?: R.drawable.starboy.toString(),
                            contentDescription = "Song Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = capsule.topSong?.title ?: "No Data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFEB3B),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun ListeningStreakItem(
    streak: ListeningStreak,
    modifier: Modifier = Modifier
) {
    val startDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(streak.startDate))
    val endDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(streak.endDate))
    val dateRange = if (startDate == endDate) startDate else "$startDate - $endDate"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .padding(16.dp)
    ) {
        // Album art for the streak

        LoadImage(
            streak.trackDetails?.imagePath ?: R.drawable.starboy.toString(),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(240.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Streak title and description
        Text(
            text = "You had a ${streak.dayCount}-day streak",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Song details
        streak.trackDetails?.let { track ->
            Text(
                text = "You played ${track.title} by ${track.artist} day after day. You were on fire",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date range with share button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateRange,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_share),
                contentDescription = "Share",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

