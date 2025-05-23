package com.example.purrytify.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocalSee
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Splash : Screen("splash", "Splash", Icons.Default.Home)
    data object Login : Screen("login", "Login", Icons.Default.Person)
    data object Library : Screen("library", "Your Library", Icons.Default.LibraryMusic)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person) {
        data object Main: Screen("profile/main", "Profile", Icons.Default.Person)
        data object TopArtist : Screen("profile/topArtist", "Top Artist", Icons.Default.BarChart) {
            fun createRoute(month: Int, year: Int): String = "profile/topArtist?month=$month&year=$year"
        }
        data object TopSong : Screen("profile/topSong", "Top Song", Icons.Default.MusicNote) {
            fun createRoute(month: Int, year: Int): String = "profile/topSong?month=$month&year=$year"
        }
        data object TimeListened: Screen("profile/timeListened", "Time Listened", Icons.Default.LockClock)
        data object CropImage : Screen("crop_image/{imageUri}", "Crop Image", Icons.Default.Crop) {
            fun createRoute(imageUri: String): String = "crop_image/${Uri.encode(imageUri)}"
        }
    }
    data object Home : Screen("home", "Home", Icons.Default.Home) {
        data object TopFiftyGlobal: Screen("home/topFiftyGlobal", "Top 50 Global", Icons.Default.LocationOn)
        data object TopFiftyCountry: Screen("home/topFiftyCountry", "Top 50 Country", Icons.Default.LocalSee)
        data object Main: Screen("home/main", "Home Main", Icons.Default.Home)
    }
}