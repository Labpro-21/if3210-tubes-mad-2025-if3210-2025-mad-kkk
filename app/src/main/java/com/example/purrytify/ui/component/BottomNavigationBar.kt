package com.example.purrytify.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.purrytify.navigation.Screen

@Composable
fun BottomNavigationBar(navController: NavHostController, modifier: Modifier = Modifier) {
    val items = listOf(Screen.Home, Screen.Library, Screen.Profile)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(
        containerColor = Color(0xFF121212),
        contentColor = Color.White
    ) {
        for (nav in items) {
            NavigationBarItem(
                selected = currentRoute == nav.route,
                onClick = { navController.navigate(nav.route) },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            nav.icon,
                            contentDescription = nav.title,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            nav.title,
                            fontSize = 10.sp,
                            color = if (currentRoute == nav.route) Color.White else Color.Gray
                        )
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                ),
                alwaysShowLabel = false,
            )
        }
    }
}