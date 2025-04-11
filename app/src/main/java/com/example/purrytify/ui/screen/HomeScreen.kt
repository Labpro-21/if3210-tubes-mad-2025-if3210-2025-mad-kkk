package com.example.purrytify.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.ui.model.HomeViewModel
import com.example.purrytify.ui.theme.Poppins
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.purrytify.ui.model.ImageLoader
import com.example.purrytify.data.model.Song
import com.example.purrytify.ui.component.EditSongBottomSheet
import com.example.purrytify.ui.component.UploadSongBottomSheet
import com.example.purrytify.ui.model.GlobalViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    showDetail: () -> Unit,
    globalViewModel: GlobalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.HomeViewModelFactory(context.applicationContext as android.app.Application)
    )

    val songs by viewModel.recentlyAddedSongs.collectAsState(emptyList())
    val recentlyPlayedSongs by viewModel.recentlyPlayedSongs.collectAsState(emptyList())

    var showUploadDialog by remember { mutableStateOf(false) }
    var showSong by remember { mutableStateOf<Song?>(null) }

    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // This is key to making it open fully
    )


    LazyColumn(
        modifier = modifier
            .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 6.dp)
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
                    showUploadDialog = true
                    showSong = song
                    Log.d("LONG CLICKED", "LLLLLL")
                })
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
    if (showUploadDialog && showSong != null) {
        EditSongBottomSheet(
            song = showSong!!,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    showUploadDialog = false
                }
            },
            sheetState = sheetState,
            onUpdate = { id, title, artist, image, audio ->
                viewModel.updateSong(id, title, artist, image, audio)
                scope.launch {
                    sheetState.hide()
                    showUploadDialog = false
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
