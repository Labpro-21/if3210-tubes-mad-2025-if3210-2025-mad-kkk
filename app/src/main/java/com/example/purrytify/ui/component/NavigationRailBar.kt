package com.example.purrytify.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.purrytify.navigation.Screen

@Composable
fun NavigationRailBar(navController: NavHostController, modifier: Modifier = Modifier) {
    val items = listOf(Screen.Home, Screen.Library, Screen.Profile)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationRail(modifier = modifier) {
        items.forEach { screen ->
            NavigationRailItem(
                selected = currentRoute == screen.route,
                onClick = { navController.navigate(screen.route) },
                icon = { Icon(screen.icon, contentDescription = screen.title) },
//                label = { Text(screen.title) }
            )
        }
    }
}
