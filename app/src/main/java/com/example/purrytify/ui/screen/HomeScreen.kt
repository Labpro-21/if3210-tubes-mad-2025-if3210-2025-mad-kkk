package com.example.purrytify.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.purrytify.R
import com.example.purrytify.data.model.Song
import com.example.purrytify.navigation.Screen
import com.example.purrytify.ui.component.EditSongBottomSheet
import com.example.purrytify.ui.component.NewSongCard
import com.example.purrytify.ui.component.SongCard
import com.example.purrytify.ui.component.SongOptionsSheet
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.HomeViewModel
import com.example.purrytify.ui.theme.Poppins
import com.example.purrytify.ui.util.CountryConstant
import com.example.purrytify.worker.LogoutListener
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    showDetail: () -> Unit,
    globalViewModel: GlobalViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    initialSongId: Int? = null
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        key = "home-view-model",
        factory = HomeViewModel.HomeViewModelFactory(
            context.applicationContext as android.app.Application,
            globalViewModel
        )
    )

    val listState = rememberLazyListState()
    val songs by viewModel.recentlyAddedSongs.collectAsState()
    val recentlyPlayedSongs by viewModel.recentlyPlayedSongs.collectAsState()
    val userCountry by globalViewModel.userLocation.collectAsState()

    var showUploadDialog by remember { mutableStateOf(false) }
    var showSongOptionSheet by remember { mutableStateOf(false) }

    var showSong by remember { mutableStateOf<Song?>(null) }

    val scope = rememberCoroutineScope()

    val uploadSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val showSongSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val currentSong by globalViewModel.currentSong.collectAsState()

    val navigateSongId by globalViewModel.navigateToSongId.collectAsState()

    LaunchedEffect(initialSongId) {
        Log.d("SPLASH_LOG", initialSongId.toString())
        if (initialSongId != null && initialSongId != -1) {
            Log.d("SPLASH_LOG", "Playing song")
            viewModel.playSharedSong(initialSongId, showDetail)
        }
    }

    LaunchedEffect(navigateSongId) {
        Log.d("SPLASH_LOG", navigateSongId.toString())
        if (navigateSongId != null) {
            viewModel.playSharedSong(navigateSongId!!, showDetail)
            globalViewModel.clearDeepLink()
        }
    }

    LogoutListener {
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
        globalViewModel.clearUserId()
        globalViewModel.logout()
    }

    LazyColumn(
        modifier = modifier.padding(top = 30.dp),
        state = listState
    ) {
        item {
            Text(
                text = "Chart",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    bottom = 12.dp,
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp
                ),
                fontFamily = Poppins
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable(onClick = { navController.navigate(Screen.Home.TopFiftyGlobal.route) })
                ) {
                    Image(
                        painter = painterResource(R.drawable.global),
                        contentDescription = "Top Global Cover",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "Your daily update of most played tracks globally",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 2,
                        lineHeight = 16.sp,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = Poppins,
                        letterSpacing = 0.2.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable(onClick = { navController.navigate(Screen.Home.TopFiftyCountry.route) })

                ) {
                    Image(
                        painter = painterResource(
                            CountryConstant.CountryImage[userCountry]
                                ?: R.drawable.id
                        ),
                        contentDescription = "Top Country Cover",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "Your daily update of most played tracks in ${CountryConstant.CountryName[userCountry]}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 2,
                        lineHeight = 16.sp,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = Poppins,
                        letterSpacing = 0.2.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "New songs",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                fontFamily = Poppins
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
            ) {
                items(songs) { song ->
                    NewSongCard(song = song, onClick = {
                        globalViewModel.playSong(song)
                        showDetail()
                    })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recently played",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                fontFamily = Poppins
            )
        }

        items(recentlyPlayedSongs) { song ->
            SongCard(
                song = song, onClick = {
                    globalViewModel.playSong(song)
                    showDetail()
                },
                onLongClick = {
                    showSongOptionSheet = true
                    showSong = song
                })
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
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
            onEdit = {
                scope.launch {
                    showSongSheetState.hide()
                    showSongOptionSheet = false
                    showUploadDialog = true
                }
            },
            onDelete = {
                viewModel.deleteSong(showSong!!)
                scope.launch {
                    showSongSheetState.hide()
                    showSongOptionSheet = false
                }
            },
            sheetState = showSongSheetState,
            onAddToQueue = {
                globalViewModel.addToQueue(showSong!!)
            },
            detail = showSong?.id == currentSong?.id,
            onLiked = {
                viewModel.toggleLiked(showSong!!)
            },
            onStartNewRadio = {
                globalViewModel.playSongs(showSong!!)
            },
            onAddToNext = {
                globalViewModel.addToNext(showSong!!)
            }
        )
    }

    if (showUploadDialog && showSong != null) {
        EditSongBottomSheet(
            song = showSong!!,
            onDismiss = {
                scope.launch {
                    uploadSheetState.hide()
                    showUploadDialog = false
                }
            },
            sheetState = uploadSheetState,
            onUpdate = { id, title, artist, image, audio ->
                if (title.isNotEmpty() && artist.isNotEmpty()) {
                    viewModel.updateSong(id, title, artist, image, audio)
                    scope.launch {
                        uploadSheetState.hide()
                        showUploadDialog = false
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Please fill all fields and select both audio and image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

}