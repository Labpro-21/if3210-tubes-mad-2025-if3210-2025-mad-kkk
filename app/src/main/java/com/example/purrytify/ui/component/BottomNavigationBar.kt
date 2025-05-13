package com.example.purrytify.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
fun BottomNavigationBar(navController: NavHostController, modifier: Modifier = Modifier) {
    val items = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isInHomeSection = currentRoute?.startsWith("home") == true

    NavigationBar(
        containerColor = Color(0xFF121212),
        contentColor = Color.White,
        modifier = modifier
    ) {
        for (nav in items) {
            val isSelected = when (nav) {
                Screen.Home -> isInHomeSection
                else -> currentRoute == nav.route
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (nav == Screen.Home && isInHomeSection) {
                        if (currentRoute != Screen.Home.Main.route) {
                            navController.navigate(Screen.Home.Main.route) {
                                popUpTo(Screen.Home.route) {
                                    inclusive = false
                                    saveState = true
                                }
                            }
                        }
                    } else if (currentRoute != nav.route) {
                        navController.navigate(nav.route) {
                            popUpTo(Screen.Home.Main.route) {
                                inclusive = false
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = nav.icon,
                            contentDescription = nav.title,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = nav.title,
                            fontSize = 10.sp,
                            color = if (isSelected) Color.White else Color.Gray
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