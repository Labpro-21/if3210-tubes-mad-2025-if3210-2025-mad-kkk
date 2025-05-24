package com.example.purrytify

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.purrytify.navigation.PurrytifyNavigationType
import com.example.purrytify.navigation.Screen
import com.example.purrytify.ui.component.BottomNavigationBar
import com.example.purrytify.ui.component.CurrentSongPlayerCard
import com.example.purrytify.ui.component.NavigationRailBar
import com.example.purrytify.ui.component.SongDetailSheet
import com.example.purrytify.ui.component.SongOptionsSheet
import com.example.purrytify.ui.model.AudioDeviceViewModel
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.screen.DailyBarData
import com.example.purrytify.ui.screen.DailyChartScreen
import com.example.purrytify.ui.screen.HomeScreen
import com.example.purrytify.ui.screen.ImageCropScreen
import com.example.purrytify.ui.screen.LibraryScreen
import com.example.purrytify.ui.screen.LoginScreen
import com.example.purrytify.ui.screen.MonthlyBarData
import com.example.purrytify.ui.screen.ProfileScreen
import com.example.purrytify.ui.screen.TopFiftyCountryScreen
import com.example.purrytify.ui.screen.TopFiftyGlobalScreen
import com.example.purrytify.ui.screen.TopMonthArtistScreen
import com.example.purrytify.ui.screen.TopMonthSongScreen
import com.example.purrytify.ui.screen.QRCodeScannerScreen
import com.example.purrytify.ui.screen.YAxisConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurrytifyApp(
    windowSize: WindowWidthSizeClass,
    globalViewModel: GlobalViewModel,
    audioDeviceViewModel: AudioDeviceViewModel,
    startDestination: Screen = Screen.Login,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val navigationType: PurrytifyNavigationType = when (windowSize) {
        WindowWidthSizeClass.Compact -> {
            PurrytifyNavigationType.BOTTOM_NAVIGATION
        }

        WindowWidthSizeClass.Medium -> {
            PurrytifyNavigationType.NAVIGATION_RAIL
        }

        WindowWidthSizeClass.Expanded -> {
            PurrytifyNavigationType.NAVIGATION_RAIL
        }

        else -> {
            PurrytifyNavigationType.BOTTOM_NAVIGATION
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
//    val hasNavbar = when (currentRoute) {
//        Screen.Home.Main.route, Screen.Home.TopFiftyGlobal.route, Screen.Home.TopFiftyCountry.route,
//        Screen.Library.route, Screen.Profile.Main.route, Screen.Profile.TopArtist.route, Screen.Profile.TopSong.route, Screen.Profile.TimeListened.route -> true
//        else -> false
//    }

    val hasNavbar = when {
        currentRoute == Screen.Home.Main.route ||
                currentRoute == Screen.Home.TopFiftyGlobal.route ||
                currentRoute == Screen.Home.TopFiftyCountry.route ||
                currentRoute == Screen.Library.route ||
                currentRoute == Screen.Profile.Main.route ||
                currentRoute?.startsWith(Screen.Profile.TopArtist.route) == true ||
                currentRoute?.startsWith(Screen.Profile.TopSong.route) == true ||
                currentRoute == Screen.Profile.TimeListened.route -> true
        else -> false
    }

    val showPlayer = currentRoute != Screen.QrCodeScanner.route && currentRoute != Screen.Login.route && currentRoute?.startsWith(Screen.Profile.CropImage.route) == false

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
            snackbarHostState.showSnackbar(
                message = "No internet connection",
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite
            )
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
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
                    startDestination = startDestination.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Screen.Login.route) {
                        LoginScreen(navController, globalViewModel)
                    }

                    composable(Screen.Library.route) {
                        LibraryScreen(
                            {
                                showDetailSheet = true
                            },
                            globalViewModel,
                            navController
                        )
                    }
                    composable(Screen.QrCodeScanner.route) {
                        QRCodeScannerScreen(navController = navController, globalViewModel = globalViewModel)
                    }

                    navigation(
                        startDestination = Screen.Profile.Main.route,
                        route = Screen.Profile.route,
                    ) {
                        composable(Screen.Profile.Main.route) {
                            ProfileScreen(
                                globalViewModel,
                                navController
                            )
                        }
                        composable(
                            route = Screen.Profile.TopArtist.route + "?month={month}&year={year}",
                            arguments = listOf(
                                navArgument("month") {
                                    type = NavType.IntType
                                    nullable = false
                                },
                                navArgument("year") {
                                    type = NavType.IntType
                                    nullable = false
                                }
                            )
                        ) { backStackEntry ->
                            val month = backStackEntry.arguments?.getInt("month") ?: 3
                            val year = backStackEntry.arguments?.getInt("year") ?: 2025
                            TopMonthArtistScreen(
                                globalViewModel,
                                navController,
                                month = month,
                                year = year
                            )
                        }
                        composable(
                            route = Screen.Profile.TopSong.route + "?month={month}&year={year}",
                            arguments = listOf(
                                navArgument("month") {
                                    type = NavType.IntType
                                    nullable = false
                                },
                                navArgument("year") {
                                    type = NavType.IntType
                                    nullable = false
                                }
                            )
                        ) { backStackEntry ->
                            val month = backStackEntry.arguments?.getInt("month") ?: 3
                            val year = backStackEntry.arguments?.getInt("year") ?: 2025
                            TopMonthSongScreen(
                                globalViewModel,
                                navController,
                                month = month,
                                year = year
                            )
                        }
                        composable(Screen.Profile.TimeListened.route) {
                            DailyChartScreen(globalViewModel, navController, 5, 2025)
                        }
                        composable(
                            route = Screen.Profile.CropImage.route,
                            arguments = listOf(
                                navArgument("imageUri") {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val imageUri = backStackEntry.arguments?.getString("imageUri")
                            ImageCropScreen(
                                globalViewModel,
                                navController,
                                imageUri
                            )
                        }
                    }

                    navigation(
                        startDestination = Screen.Home.Main.route,
                        route = Screen.Home.route,
                    ) {
                        composable(
                            Screen.Home.Main.route,
                            deepLinks = listOf(navDeepLink { uriPattern = "purrytify://song/{songId}" }),
                            arguments = listOf(navArgument("songId") {
                                type = NavType.IntType
                                defaultValue = -1
                            }),
                        ) { backStackEntry ->
                            val songId = backStackEntry.arguments?.getInt("songId")
                            HomeScreen(
                                { showDetailSheet = true },
                                globalViewModel,
                                navController,
                                initialSongId = songId
                            )
                        }

                        composable(Screen.Home.TopFiftyGlobal.route) {
                            TopFiftyGlobalScreen(
                                globalViewModel,
                                navController
                            ) {
                                showDetailSheet = true
                            }
                        }

                        composable(Screen.Home.TopFiftyCountry.route) {
                            TopFiftyCountryScreen(
                                globalViewModel,
                                navController
                            ) {
                                showDetailSheet = true
                            }
                        }
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
                            onLikeClick = {
                                globalViewModel.toggleLikedStatus()
                            },
                            onPlayPauseClick = {
                                globalViewModel.togglePlayPause()
                            },
                            onSwipeNext = {
                                globalViewModel.playNextSong()
                            },
                            onSwipePrev = {
                                globalViewModel.playPreviousSong()
                            },
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
                            .height(72.dp)
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
                    audioDeviceViewModel = audioDeviceViewModel,
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
                    },
                    onLiked = {
                        globalViewModel.toggleLikedStatus()
                    },
                    onStartNewRadio = {
                        globalViewModel.playSongs(currentSong!!)
                    },
                    onAddToNext = {
                        globalViewModel.addToNext(currentSong!!)
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