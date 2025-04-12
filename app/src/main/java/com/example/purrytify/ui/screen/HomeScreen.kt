package com.example.purrytify.ui.screen

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.purrytify.data.model.Song
import com.example.purrytify.navigation.Screen
import com.example.purrytify.ui.component.EditSongBottomSheet
import com.example.purrytify.ui.component.SongOptionsSheet
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.HomeViewModel
import com.example.purrytify.ui.model.ImageLoader
import com.example.purrytify.ui.theme.Poppins
import com.example.purrytify.worker.LogoutListener
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    showDetail: () -> Unit,
    globalViewModel: GlobalViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.HomeViewModelFactory(
            context.applicationContext as android.app.Application,
            globalViewModel
        )
    )

    val songs by viewModel.recentlyAddedSongs.collectAsState(emptyList())
    val recentlyPlayedSongs by viewModel.recentlyPlayedSongs.collectAsState(emptyList())

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

    LogoutListener {
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
        globalViewModel.clearUserId()
        globalViewModel.logout()
    }

    LazyColumn(
        modifier = modifier
            .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 6.dp)
    ) {
        item {
            Text(
                text = "New songs",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
                fontFamily = Poppins
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(songs) { song ->
                    NewSongItem(song = song, onClick = {
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
                modifier = Modifier.padding(bottom = 12.dp),
                fontFamily = Poppins
            )
        }

        items(recentlyPlayedSongs) { song ->
            RecentlyPlayedItem(
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
            Spacer(modifier = Modifier.height(70.dp))
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
                if (title.isNotEmpty() && artist.isNotEmpty() && image != null && audio != null) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentlyPlayedItem(song: Song, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 8.dp)
    ) {
        ImageLoader.LoadImage(
            imagePath = song.imagePath,
            contentDescription = "${song.title} album cover",
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
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
                overflow = TextOverflow.Ellipsis,
                fontFamily = Poppins,
            )

            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = Poppins
            )
        }
        Spacer(modifier = Modifier.weight(1f))
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

@Composable
fun NewSongItem(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        ImageLoader.LoadImage(
            imagePath = song.imagePath,
            contentDescription = "${song.title} album cover",
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = Poppins
        )

        Text(
            text = song.artist,
            color = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = Poppins
        )
    }
}
