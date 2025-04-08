package com.example.purrytify

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val hasNavbar = when (currentRoute) {
        Screen.Home.route, Screen.Library.route, Screen.Profile.route -> true
        else -> false
    }

    Box(modifier = modifier) {
        Row(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = (navigationType == PurrytifyNavigationType.NAVIGATION_RAIL && hasNavbar)) {
                NavigationRailBar(navController)
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
                AnimatedVisibility(visible = navigationType == PurrytifyNavigationType.BOTTOM_NAVIGATION && hasNavbar) {
                    BottomNavigationBar(navController, modifier = Modifier.height(86.dp))
                }
            }
        }
    }
}