package com.example.purrytify

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.purrytify.navigation.PurrytifyNavigationType
import com.example.purrytify.navigation.Screen
import com.example.purrytify.ui.component.BottomNavigationBar
import com.example.purrytify.ui.component.NavigationRailBar
import com.example.purrytify.ui.screen.HomeScreen
import com.example.purrytify.ui.screen.LibraryScreen
import com.example.purrytify.ui.screen.LoginScreen
import com.example.purrytify.ui.screen.ProfileScreen
import com.example.purrytify.ui.screen.SongDetailScreen
import com.example.purrytify.ui.screen.SplashScreen
import com.example.purrytify.data.model.Song
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.HomeViewModel
import com.example.purrytify.ui.model.ImageLoader

@Composable
fun CurrentSongPlayerCard(
    song: Song,
    onCardClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    isPlaying: Boolean,
    progress: Float = 0.3f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
//            .padding(horizontal = 8.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            .clickable(onClick = onCardClick)
    ) {
        // Content row with album cover, song info, and buttons
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album cover
            ImageLoader.LoadImage(
                imagePath = song.imagePath,
                contentDescription = "${song.title} album cover",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )

            // Song info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = song.title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = song.artist,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Add to playlist button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    .clickable { /* Add to playlist action */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Add to playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Play/Pause button
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    .clickable(onClick = onPlayPauseClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Progress indicator at the bottom of the card
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun PurrytifyApp(
    windowSize: WindowWidthSizeClass,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    // TODO: add view model for song state
    val navigationType: PurrytifyNavigationType = when (windowSize) {
        WindowWidthSizeClass.Compact -> {
            PurrytifyNavigationType.BOTTOM_NAVIGATION
        }

        WindowWidthSizeClass.Medium -> {
            PurrytifyNavigationType.NAVIGATION_RAIL
        }

        WindowWidthSizeClass.Expanded -> {
            // TODO: change to PurrytifyNavigationType.PERMANENT_NAVIGATION_DRAWER
            PurrytifyNavigationType.NAVIGATION_RAIL
        }

        else -> {
            PurrytifyNavigationType.BOTTOM_NAVIGATION
        }
    }

    // Current playing song (this should eventually come from a ViewModel)
//    var currentPlayingSong = remember {
//        mutableStateOf(
//            Song(
//                id = 1,
//                title = "Starboy",
//                artist = "The Weeknd",
//                imagePath = R.drawable.starboy.toString()
//            )
//        )
//    }

    val isPlaying by remember { mutableStateOf(true) }
    val songProgress by remember { mutableStateOf(0.3f) }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val hasNavbar = when (currentRoute) {
        Screen.Home.route, Screen.Library.route, Screen.Profile.route -> true
        else -> false
    }

    // Determine if we should show the player (show on all screens except splash and login)
    val showPlayer = currentRoute != Screen.Splash.route && currentRoute != Screen.Login.route && currentRoute != Screen.SongDetail.route

    val globalViewModel: GlobalViewModel = viewModel(
        factory = GlobalViewModel.GlobalViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    )

    val currentSong = globalViewModel.currentlyPlayingSong.collectAsState().value

    Box(modifier = modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = (navigationType == PurrytifyNavigationType.NAVIGATION_RAIL && hasNavbar)) {
                NavigationRailBar(navController)
            }
            AnimatedVisibility(visible = (navigationType == PurrytifyNavigationType.NAVIGATION_RAIL && hasNavbar)) {
                VerticalDivider(modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues()))
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Screen.Splash.route) {
                        SplashScreen(navController)
                    }
                    composable(Screen.Login.route) { LoginScreen(navController) }
                    composable(Screen.Home.route) {
                        HomeScreen(
                            navController,
                            globalViewModel
                        )
                    }
                    composable(Screen.Library.route) {
                        LibraryScreen(
                            navController,
                        )
                    }
                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            navController,
                        )
                    }
                    composable(
                        route = Screen.SongDetail.route,
                        arguments = listOf(navArgument("songId") { type = NavType.StringType })
                    ) {
                        val songId = it.arguments?.getString("songId") ?: ""
                        SongDetailScreen(songId, navController)
                    }
                }

                // Player card - visible on all screens except splash and login
                if (showPlayer) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .background(Color.Transparent)
                    ) {
                        CurrentSongPlayerCard(
                            song = currentSong ?: Song(id = 1, title = "No Title", artist = "No Artist"),
                            onCardClick = {
                                navController.navigate(Screen.SongDetail.createRoute(currentSong?.id.toString()))
                            },
                            onPlayPauseClick = { /* Toggle playback */ },
                            isPlaying = isPlaying,
                            progress = songProgress
                        )
                    }
                }

                // Bottom navigation bar at the very bottom
                AnimatedVisibility(visible = navigationType == PurrytifyNavigationType.BOTTOM_NAVIGATION && hasNavbar) {
                    BottomNavigationBar(navController, modifier = Modifier.height(92.dp).offset(y = 5.dp))
                }
            }
        }
    }
}