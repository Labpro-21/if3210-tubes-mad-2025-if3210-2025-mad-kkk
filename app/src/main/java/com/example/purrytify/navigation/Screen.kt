package com.example.purrytify.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocalSee
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Splash : Screen("splash", "Splash", Icons.Default.Home)
    data object Login : Screen("login", "Login", Icons.Default.Person)
    data object Library : Screen("library", "Your Library", Icons.Default.LibraryMusic)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person) {
        data object Main: Screen("profile/main", "Profile", Icons.Default.Person)
        data object TopArtist: Screen("profile/topArtist", "Top Artist", Icons.Default.BarChart)
        data object TopSong: Screen("profile/topSong", "Top Song", Icons.Default.PieChart)
    }
    data object Home : Screen("home", "Home", Icons.Default.Home) {
        data object TopFiftyGlobal: Screen("home/topFiftyGlobal", "Top 50 Global", Icons.Default.LocationOn)
        data object TopFiftyCountry: Screen("home/topFiftyCountry", "Top 50 Country", Icons.Default.LocalSee)
        data object Main: Screen("home/main", "Home Main", Icons.Default.Home)
    }
}