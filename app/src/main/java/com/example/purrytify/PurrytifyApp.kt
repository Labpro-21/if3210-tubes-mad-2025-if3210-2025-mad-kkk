package com.example.purrytify

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.purrytify.navigation.PurrytifyNavigationType
import com.example.purrytify.navigation.Screen
import com.example.purrytify.ui.component.BottomNavigationBar
import com.example.purrytify.ui.component.NavigationRailBar
import com.example.purrytify.ui.screen.HomeScreen
import com.example.purrytify.ui.screen.LibraryScreen
import com.example.purrytify.ui.screen.LoginScreen
import com.example.purrytify.ui.screen.ProfileScreen
import com.example.purrytify.ui.screen.SplashScreen
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.ui.component.CurrentSongPlayerCard
import com.example.purrytify.ui.component.SongDetailSheet
import com.example.purrytify.ui.component.SongOptionsSheet
import com.example.purrytify.ui.model.GlobalViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurrytifyApp(
    windowSize: WindowWidthSizeClass,
    globalViewModel: GlobalViewModel,
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

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val hasNavbar = when (currentRoute) {
        Screen.Home.route, Screen.Library.route, Screen.Profile.route -> true
        else -> false
    }

    val showPlayer = currentRoute != Screen.Splash.route && currentRoute != Screen.Login.route

    val currentSong by globalViewModel.currentSong.collectAsState()
    val duration by globalViewModel.duration.collectAsState()
    val currPosition by globalViewModel.currentPosition.collectAsState()
    val isPlaying by globalViewModel.isPlaying.collectAsState()
    var showDetailSheet by remember { mutableStateOf(false) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var showSongOptionSheet by remember { mutableStateOf(false) }
    val showSongOptionSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val isConnected by globalViewModel.isConnected.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            snackbarHostState.showSnackbar(message = "No internet connection", withDismissAction = true, duration = SnackbarDuration. Indefinite)
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

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
                    composable(Screen.Login.route) { LoginScreen(navController, globalViewModel) }
                    composable(Screen.Home.route) {
                        HomeScreen({
                                showDetailSheet = true
                            },
                            globalViewModel
                        )
                    }
                    composable(Screen.Library.route) {
                        LibraryScreen({
                                showDetailSheet = true
                            },
                            globalViewModel
                        )
                    }
                    composable(Screen.Profile.route) {
                        ProfileScreen(globalViewModel, navController)
                    }
                }

                if (showPlayer && currentSong != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .background(Color.Transparent)
                    ) {
                        CurrentSongPlayerCard(
                            song = currentSong!!,
                            onCardClick = {
                                showDetailSheet = true
                            },
                            onPlayPauseClick = { /* Toggle playback */ },
                            isPlaying = isPlaying,
                            currProgress = currPosition,
                            duration = duration
                        )
                    }
                }

                AnimatedVisibility(visible = navigationType == PurrytifyNavigationType.BOTTOM_NAVIGATION && hasNavbar) {
                    BottomNavigationBar(
                        navController,
                        modifier = Modifier
                            .height(92.dp)
                            .offset(y = 5.dp)
                    )
                }
            }

            if (showDetailSheet) {
                SongDetailSheet(
                    onDismiss = {
                        scope.launch {
                            detailSheetState.hide()
                            showDetailSheet = false
                        }
                    },
                    globalViewModel = globalViewModel,
                    sheetState = detailSheetState,
                    onOpenOption = {
                        showSongOptionSheet = true
                    }
                )
            }

            if (showSongOptionSheet && currentSong != null) {
                SongOptionsSheet(
                    song = currentSong!!,
                    onDismiss = {
                        scope.launch {
                            showSongOptionSheetState.hide()
                            showSongOptionSheet = false
                        }
                    },
                    onEdit = {},
                    onDelete = {},
                    sheetState = showSongOptionSheetState,
                    detail = true,
                    onAddToQueue = {
                        globalViewModel.addToQueue(currentSong!!)
                    }
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}