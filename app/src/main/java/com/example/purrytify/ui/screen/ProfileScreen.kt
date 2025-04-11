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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavHostController
import com.example.purrytify.navigation.PurrytifyNavigationType
import com.example.purrytify.ui.component.BottomNavigationBar
import com.example.purrytify.ui.component.NavigationRailBar
import com.example.purrytify.ui.model.ImageLoader
import com.example.purrytify.R
import com.example.purrytify.ui.model.ProfileViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.service.baseUrl


@Composable
fun ProfileScreen(navController: NavHostController, modifier: Modifier = Modifier) {
//    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
//        Text("Profile Page")
//    }

    var context = LocalContext.current

    val backgroundColor = Color(0xFF121212)
    val secondaryColor = Color(0xFF01667A)
    val surfaceColor = Color(0xFF212121)

    var viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.ProfileViewModelFactory(context.applicationContext as Application)
    )

    val userState by viewModel.userState.collectAsState()
    val songStats by viewModel.songStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Profile header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(
                    colors = listOf(secondaryColor, backgroundColor),
                ))
                .padding(top = 96.dp, bottom = 48.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
//                    ImageLoader.LoadImage(
//                        imagePath = R.drawable.starboy.toString(),
//                        contentDescription = "Profile Image",
//                        modifier = Modifier
//                            .size(150.dp)
//                            .clip(CircleShape),
//                        contentScale = ContentScale.Crop
//                    )
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
                    onClick = {  },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .width(120.dp)
                        .height(36.dp)
                ) {
                    Text(
                        text = "Edit Profile",
                        color = Color.White
                    )
                }
            }
        }

        // Stats row
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