package com.example.purrytify.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.purrytify.navigation.Screen

@Composable
fun BottomNavigationBar(navController: NavHostController, modifier: Modifier = Modifier) {
    val items = listOf(Screen.Home, Screen.Library, Screen.Profile)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(modifier = modifier) {
        for (nav in items) {
            NavigationBarItem(
                selected = currentRoute == nav.route,
                onClick = { navController.navigate(nav.route) },
                icon = { Icon(nav.icon, contentDescription = nav.title) },
//                label = { Text(nav.title) }
            )
        }
    }
}