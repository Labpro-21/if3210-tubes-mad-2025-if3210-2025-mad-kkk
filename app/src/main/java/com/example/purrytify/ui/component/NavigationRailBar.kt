package com.example.purrytify.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.purrytify.navigation.Screen

@Composable
fun NavigationRailBar(navController: NavHostController, modifier: Modifier = Modifier) {
    val items = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isInHomeSection = currentRoute?.startsWith("home") == true
    val isInProfileSection = currentRoute?.startsWith("profile") == true

    NavigationRail(modifier = modifier.statusBarsPadding()) {
        items.forEach { screen ->
            val isSelected = when (screen) {
                Screen.Home -> isInHomeSection
                Screen.Profile -> isInProfileSection
                else -> currentRoute == screen.route
            }
            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .width(176.dp)
                    ) {
                        Icon(
                            screen.icon,
                            contentDescription = screen.title,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = screen.title, fontSize = 16.sp)
                    }
                },
                alwaysShowLabel = false,
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
