package com.example.purrytify.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Splash : Screen("splash", "Splash", Icons.Default.Home)
    data object Login : Screen("login", "Login", Icons.Default.Person)
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Library : Screen("library", "Your Library", Icons.Default.LibraryMusic)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
}