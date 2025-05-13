package com.example.purrytify.ui.screen

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.purrytify.R
import com.example.purrytify.data.model.Song
import com.example.purrytify.navigation.Screen
import com.example.purrytify.ui.component.NoInternetScreen
import com.example.purrytify.ui.component.SongOptionsSheet
import com.example.purrytify.ui.component.TopSongCard
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.TopCountryViewModel
import com.example.purrytify.ui.util.CountryConstant
import com.example.purrytify.worker.LogoutListener
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopFiftyCountryScreen(
    globalViewModel: GlobalViewModel,
    navController: NavController,
    showDetail: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: TopCountryViewModel = viewModel(
        factory = TopCountryViewModel.TopCountryViewModelFactory(
            context.applicationContext as Application,
            globalViewModel
        )
    )
    val isConnected by globalViewModel.isConnected.collectAsState()
    val scope = rememberCoroutineScope()

    var showSongOptionSheet by remember { mutableStateOf(false) }
    val showSongSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var showSong by remember { mutableStateOf<Song?>(null) }
    var showIndex by remember { mutableStateOf<Int?>(null) }

    val userCountry by globalViewModel.userLocation.collectAsState()

    LogoutListener {
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
        globalViewModel.clearUserId()
        globalViewModel.logout()
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            viewModel.loadOnlineSong()
        } else {
            viewModel.isLoading = false
            viewModel.success = false
        }
    }

    if (viewModel.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        if (isConnected && viewModel.success) {
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
                                            0.0f to Color(
                                                CountryConstant.CountryColor[userCountry]?.first
                                                    ?: 0xFF1C8075
                                            ),
                                            0.4f to Color(
                                                CountryConstant.CountryColor[userCountry]?.first
                                                    ?: 0xFF1D4569
                                            ),
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
                                        painter = painterResource(
                                            id = CountryConstant.CountryImage[userCountry]
                                                ?: R.drawable.id
                                        ),
                                        contentDescription = "Playlist Cover",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(223.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .shadow(
                                                elevation = 20.dp,
                                                shape = RoundedCornerShape(4.dp),
                                                clip = false
                                            )
                                    )
                                }
                                Text(
                                    text = "Your daily update of most played tracks in ${CountryConstant.CountryName[userCountry]}",
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
                                    text = "May 2025 • 2h 55min",
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
                                IconButton(onClick = {
                                    // TODO: download all songs
                                }) {
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
                                    IconButton(onClick = {
                                        globalViewModel.playSongs(viewModel.songs)
                                    }) {
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
                    itemsIndexed(viewModel.songs) { index, song ->
                        TopSongCard(index + 1, song, {
                            val songs = viewModel.songs
                            val songSize = songs.size
                            if (songSize == 0) return@TopSongCard

                            val songsToPlay = List(songSize) { i ->
                                songs[(i + index) % songSize]
                            }
                            globalViewModel.playSongs(songsToPlay)
                            showDetail()
                        }, {
                            showSongOptionSheet = true
                            showSong = song
                            showIndex = index
                        })
                    }
                }
            }
            if (showSongOptionSheet && showSong != null) {
                SongOptionsSheet(
                    song = showSong!!,
                    onDismiss = {
                        scope.launch {
                            showSongSheetState.hide()
                            showSongOptionSheet = false
                        }
                    },
                    onEdit = {},
                    onDelete = {},
                    sheetState = showSongSheetState,
                    onAddToQueue = {
                        globalViewModel.addToQueue(showSong!!)
                    },
                    onLiked = {
                        viewModel.toggleLiked(showSong!!)
                    },
                    onStartNewRadio = {
                        val idx = showIndex ?: return@SongOptionsSheet
                        val songs = viewModel.songs
                        val songSize = songs.size
                        if (songSize == 0) return@SongOptionsSheet

                        val songsToPlay = List(songSize) { i ->
                            songs[(i + idx) % songSize]
                        }

                        globalViewModel.playSongs(songsToPlay)
                    },
                    onAddToNext = {
                        globalViewModel.addToNext(showSong!!)
                    }
                )
            }
        } else {
            NoInternetScreen {
                scope.launch {
                    viewModel.loadOnlineSong()
                }
            }
        }
    }
}