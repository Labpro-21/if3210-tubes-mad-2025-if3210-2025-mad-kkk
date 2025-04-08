package com.example.purrytify.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.purrytify.navigation.PurrytifyNavigationType
import com.example.purrytify.navigation.Screen
import com.example.purrytify.ui.component.BottomNavigationBar
import com.example.purrytify.ui.component.NavigationRailBar
import com.example.purrytify.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val coverResId: Int
)


@Composable
fun HomeScreen(
    navController: NavHostController,
    navigationType: PurrytifyNavigationType,
    modifier: Modifier = Modifier
) {
//    val dummySongs = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15")
    val newSongs = listOf(
        Song("1", "Starboy", "The Weeknd", R.drawable.starboy),
        Song("2", "Here Comes The Sun", "The Beatles", R.drawable.here_comes),
        Song("3", "Midnight Pretenders", "Tomoko Aran", R.drawable.midnight),
        Song("4", "Violent Crimes", "Kanye West", R.drawable.violent)
    )

    val recentlyPlayed = listOf(
        Song("5", "Jazz is for ordinary people", "berlioz", R.drawable.jazz),
        Song("6", "Loose", "Daniel Caesar", R.drawable.starboy),
        Song("7", "Nights", "Frank Ocean", R.drawable.starboy),
        Song("8", "Kiss of Life", "Sade", R.drawable.starboy),
        Song("9", "BEST INTEREST", "Tyler, The Creator", R.drawable.starboy)
    )

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
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "New songs",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // New songs horizontal scrolling row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(newSongs) { song ->
                                NewSongItem(song = song, onClick = {
                                    navController.navigate(Screen.SongDetail.createRoute(song.id))
                                })
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Recently played",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Recently played list
                    items(recentlyPlayed) { song ->
                        RecentlyPlayedItem(song = song, onClick = {
                            navController.navigate(Screen.SongDetail.createRoute(song.id))
                        })
                    }

                    item {
                        // Add spacing before bottom bar
                        Spacer(modifier = Modifier.height(70.dp))
                    }
                }

                AnimatedVisibility(visible = navigationType == PurrytifyNavigationType.BOTTOM_NAVIGATION) {
                    BottomNavigationBar(navController)
                }
            }
        }
    }
}

@Composable
fun RecentlyPlayedItem(song: Song, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(id = song.coverResId),
            contentDescription = "${song.title} album cover",
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NewSongItem(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(id = song.coverResId),
            contentDescription = "${song.title} album cover",
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = song.artist,
            color = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
