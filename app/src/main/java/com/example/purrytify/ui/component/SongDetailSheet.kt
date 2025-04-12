package com.example.purrytify.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.Divider
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.ImageLoader
import com.example.purrytify.ui.theme.Poppins
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


private fun formatTime(seconds: Double): String {
    val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
    val remainingSeconds = seconds.toLong() - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailSheet(onDismiss: () -> Unit, sheetState: SheetState, globalViewModel: GlobalViewModel, modifier: Modifier = Modifier, onOpenOption: () -> Unit) {
    val song by globalViewModel.currentSong.collectAsState()
    val isPlaying by globalViewModel.isPlaying.collectAsState()
    val sliderPosition by globalViewModel.currentPosition.collectAsState()
    val duration by globalViewModel.duration.collectAsState()
    var dragPosition by remember { mutableStateOf(0f) }

    val queueList by globalViewModel.queue.collectAsState()

    val scope = rememberCoroutineScope()
    var showQueueSheet by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val validDuration = remember(duration) {
        maxOf(0.1, duration).toFloat()
    }

    val validPosition = remember(sliderPosition, validDuration) {
        sliderPosition.toFloat().coerceIn(0f, validDuration)
    }

    val gradientColors by remember (song) { mutableStateOf(listOf(Color(song?.primaryColor?:0x0064B5F6), Color( song?.secondaryColor?: 0x000D47A1), Color(0xFF101510))) }
    val scrollState = rememberScrollState()

    val isRepeatEnabled by globalViewModel.isRepeat.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = null, contentWindowInsets = {WindowInsets(0)}
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = gradientColors
                    )
                )
        ) {
            if (song == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = onOpenOption) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(50.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        ImageLoader.LoadImage(
                            imagePath = song?.imagePath,
                            contentDescription = "${song?.title} Album Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(96.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song?.title ?: "",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Poppins
                            )
                            Text(
                                text = song?.artist ?: "",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                fontFamily = Poppins
                            )
                        }

                        IconButton(onClick = { globalViewModel.toggleLikedStatus() }) {
                            Icon(
                                imageVector = if (song?.isLiked == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (song!!.isLiked) Color(0xFFFF4081) else Color.White
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Slider(
                            value = sliderPosition.toFloat(),
                            onValueChange = { newPosition ->
                                dragPosition = newPosition
                            },
                            onValueChangeFinished = {
                                globalViewModel.seekTo(dragPosition.toInt())
                            },
                            valueRange = 0f..validDuration,
                            thumb = {
                                Spacer(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color.White, CircleShape)
                                )
                            },
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(validPosition.toDouble()),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontFamily = Poppins
                            )

                            Text(
                                text = formatTime(validDuration.toDouble()),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontFamily = Poppins
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            globalViewModel.shuffle()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                globalViewModel.playPreviousSong()
                                Log.d("PREVCLICKED", "PREVCLICKED")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                globalViewModel.togglePlayPause()
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                globalViewModel.playNextSong()
                                Log.d("NEXTCLICKED", "NEXTCLICKED")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                globalViewModel.toggleRepeat()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = when (isRepeatEnabled) {
                                    1 -> Color(0xFFFF4081)
                                    2 -> Color(0xFF3DC2AC)
                                    else -> Color.White
                                },
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(
                            onClick = { showQueueSheet = true },
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "View Queue",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    if (showQueueSheet) {
        QueueSheet(
            queueList = queueList,
            onDismiss = {
                scope.launch {
                    queueSheetState.hide()
                    showQueueSheet = false
                }
            },
            onSongClick = { song ->
                globalViewModel.playSong(song)
                scope.launch {
                    queueSheetState.hide()
                    showQueueSheet = false
                }
            },
            sheetState = queueSheetState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queueList: List<com.example.purrytify.data.model.Song>,
    onDismiss: () -> Unit,
    onSongClick: (com.example.purrytify.data.model.Song) -> Unit,
    sheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Queue",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Poppins
                )

                Text(
                    text = "${queueList.size} songs",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontFamily = Poppins
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            if (queueList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Queue is empty",
                        color = Color.Gray,
                        fontFamily = Poppins
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    items(queueList) { song ->
                        QueueItem(
                            song = song,
                            onClick = { onSongClick(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueueItem(
    song: com.example.purrytify.data.model.Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            ImageLoader.LoadImage(
                imagePath = song.imagePath,
                contentDescription = "${song.title} Album Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = Poppins,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                fontSize = 14.sp,
                color = Color.Gray,
                fontFamily = Poppins,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (song.isLiked) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Liked",
                tint = Color(0xFFFF4081),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}