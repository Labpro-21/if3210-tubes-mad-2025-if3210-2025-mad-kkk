package com.example.purrytify.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.purrytify.navigation.PurrytifyNavigationType
import com.example.purrytify.navigation.Screen
import com.example.purrytify.ui.component.BottomNavigationBar
import com.example.purrytify.ui.component.NavigationRailBar

@Composable
fun HomeScreen(
    navController: NavHostController,
    navigationType: PurrytifyNavigationType,
    modifier: Modifier = Modifier
) {
    val dummySongs = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15")

    Box(modifier = modifier) {
        Row(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = navigationType == PurrytifyNavigationType.NAVIGATION_RAIL) {
                NavigationRailBar(navController)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.inverseOnSurface)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp).weight(1f)
                ) {
                    items(dummySongs) { songId ->
                        Text(
                            text = "Song $songId",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(Screen.SongDetail.createRoute(songId))
                                }
                                .padding(12.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = navigationType == PurrytifyNavigationType.BOTTOM_NAVIGATION) {
                    BottomNavigationBar(navController)
                }
            }
        }
    }
}
