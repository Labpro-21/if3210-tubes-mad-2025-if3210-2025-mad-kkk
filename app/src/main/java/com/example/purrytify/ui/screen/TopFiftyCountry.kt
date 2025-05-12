package com.example.purrytify.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.purrytify.R
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.theme.Poppins

@Composable
fun TopFiftyCountryScreen(globalViewModel: GlobalViewModel, navController: NavController) {
    val tracks = listOf(
        Track("Jazz is for ordinary people", "berlioz", R.drawable.starboy),
        Track("Loose", "Daniel Caesar", R.drawable.starboy),
        Track("Nights", "Starboy The Weeknd", R.drawable.starboy),
        Track("Jazz is for ordinary people", "berlioz", R.drawable.starboy),
        Track("Loose", "Daniel Caesar", R.drawable.starboy),
        Track("Nights", "Starboy The Weeknd", R.drawable.starboy),
        Track("Jazz is for ordinary people", "berlioz", R.drawable.starboy),
        Track("Loose", "Daniel Caesar", R.drawable.starboy),
        Track("Nights", "Starboy The Weeknd", R.drawable.starboy),
        Track("Jazz is for ordinary people", "berlioz", R.drawable.starboy),
        Track("Loose", "Daniel Caesar", R.drawable.starboy),
        Track("Nights", "Starboy The Weeknd", R.drawable.starboy),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0xFF1C8075),
                                    0.4f to Color(0xFF1D4569),
                                    1.0f to Color(0xFF121212)
                                ),
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 40.dp,
                                start = 16.dp,
                                end = 16.dp,
                            )
                    ) {
                        Row {
                            IconButton(onClick = {
                                navController.popBackStack()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(), horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.top_global_cover),
                                contentDescription = "Playlist Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(223.dp)
                            )
                        }
                        Text(
                            text = "Your daily update of most played tracks in your country",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 14.dp),
                            lineHeight = 16.sp
                        )
                        Row(
                            modifier = Modifier.padding(top = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.logo_3),
                                tint = Color.White,
                                contentDescription = "Purrytify",
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Purrytify",
                                color = Color.White,
                                fontSize = 12.sp,
                            )
                        }
                        Text(
                            text = "Apr 2025 • 2h 55min",
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 1.dp, bottom = 8.dp),
                            fontSize = 12.sp,
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.download_icon),
                                tint = Color.White.copy(0.7f),
                                contentDescription = "Download All",
                                modifier = Modifier
                                    .size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF30B454)),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play & Pause",
                                    tint = Color(0xFF121212),
                                    modifier = Modifier
                                        .size(30.dp)
                                )
                            }
                        }
                    }
                }
            }
            itemsIndexed(tracks) { index, track ->
                TopSongCard(index + 1, track, {}, {})
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopSongCard(rank: Int, track: Track, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 4.dp)
    ) {

        Text(
            text = rank.toString(),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .width(34.dp)
                .padding(start = 6.dp)
        )
        Image(
            painter = painterResource(id = track.albumArtResId),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp),
        ) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = Poppins,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = track.artist,
                color = Color.Gray,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = Poppins
            )
        }
        IconButton(onClick = onLongClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}