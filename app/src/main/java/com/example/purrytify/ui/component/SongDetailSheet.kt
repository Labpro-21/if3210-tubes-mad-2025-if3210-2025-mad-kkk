package com.example.purrytify.ui.component

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.mediarouter.media.MediaRouter.RouteInfo
import com.example.purrytify.ui.model.AudioDeviceViewModel
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.LoadImage
import com.example.purrytify.ui.theme.Poppins
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit


private fun formatTime(seconds: Double): String {
    val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
    val remainingSeconds = seconds.toLong() - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    globalViewModel: GlobalViewModel,
    audioDeviceViewModel: AudioDeviceViewModel,
    modifier: Modifier = Modifier,
    onOpenOption: () -> Unit
) {
    val song by globalViewModel.currentSong.collectAsState()
    val currIdx by globalViewModel.currIdx.collectAsState()
    val isPlaying by globalViewModel.isPlaying.collectAsState()
    val sliderPosition by globalViewModel.currentPosition.collectAsState()
    val duration by globalViewModel.duration.collectAsState()
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val context = LocalContext.current
    val queueList by globalViewModel.queue.collectAsState()

    val scope = rememberCoroutineScope()
    var showQueueSheet by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState()
    var showDeviceSheet by remember { mutableStateOf(false) }
    val deviceSheetState = rememberModalBottomSheetState()
    val selectedDevice by audioDeviceViewModel.selectedDevice
    val validDuration = remember(duration) {
        maxOf(0.1, duration).toFloat()
    }

    val validPosition = remember(sliderPosition, validDuration) {
        sliderPosition.toFloat().coerceIn(0f, validDuration)
    }

    val gradientColors by remember(song) {
        mutableStateOf(
            listOf(
                Color(
                    song?.primaryColor ?: 0x0064B5F6
                ), Color(song?.secondaryColor ?: 0x000D47A1), Color(0xFF101510)
            )
        )
    }
    val scrollState = rememberScrollState()

    val isRepeatEnabled by globalViewModel.isRepeat.collectAsState()
    val isShuffled by globalViewModel.shuffled.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
        modifier = modifier
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
                        .padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 16.dp),
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

                    Spacer(modifier = Modifier.height(32.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        LoadImage(
                            imagePath = song?.imagePath,
                            contentDescription = "${song?.title} Album Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song?.title ?: "",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Poppins,
                                    letterSpacing = 0.2.sp
                                )
                                Text(
                                    text = song?.artist ?: "",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp,
                                    fontFamily = Poppins,
                                    letterSpacing = 0.2.sp
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
                        ) {
                            Slider(
                                value = sliderPosition.toFloat(),
                                onValueChange = { newPosition ->
                                    globalViewModel.dragTo(newPosition.toInt())
                                    dragPosition = newPosition
                                },
                                onValueChangeFinished = {
                                    globalViewModel.seekTo(dragPosition.toInt())
                                },
                                valueRange = 0f..validDuration,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(),
                                thumb = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .offset(y = (3).dp)
                                            .background(Color.White, CircleShape)
                                            .padding(0.dp)
                                    )
                                },
                                track = { sliderState ->
                                    SliderDefaults.Track(
                                        sliderState = sliderState,
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                        ),
                                        modifier = Modifier.height(3.dp),
                                        thumbTrackGapSize = 0.dp
                                    )
                                },
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                globalViewModel.shuffle()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (isShuffled) Color(0xFF3DC2AC) else Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    globalViewModel.playPreviousSong()
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
                                    globalViewModel.repeat()
                                }
                            ) {
                                Icon(
                                    imageVector = when (isRepeatEnabled) {
                                        0, 2 -> Icons.Default.Repeat
                                        else -> Icons.Default.RepeatOne
                                    },
                                    contentDescription = "Repeat",
                                    tint = when (isRepeatEnabled) {
                                        1, 2 -> Color(0xFF3DC2AC)
                                        else -> Color.White
                                    },
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    showDeviceSheet = true
                                }
                            ) {
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (selectedDevice?.deviceType) {
                                            RouteInfo.DEVICE_TYPE_WIRED_HEADSET, RouteInfo.DEVICE_TYPE_WIRED_HEADPHONES, RouteInfo.DEVICE_TYPE_BLUETOOTH_A2DP, RouteInfo.DEVICE_TYPE_BLE_HEADSET, RouteInfo.DEVICE_TYPE_USB_HEADSET -> Icons.Default.Headphones
                                            RouteInfo.DEVICE_TYPE_TV -> Icons.Default.Monitor
                                            RouteInfo.DEVICE_TYPE_CAR -> Icons.Default.DirectionsCar
                                            0 -> Icons.Default.Devices
                                            null -> Icons.Default.Devices
                                            else -> Icons.Default.Speaker
                                        },
                                        contentDescription = "View Devices",
                                        tint = if (selectedDevice == null || selectedDevice?.deviceType == 0) Color.White else Color.Green,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Text(
                                    text = if (selectedDevice == null || selectedDevice?.deviceType == 0) "" else selectedDevice?.name ?: "",
                                    color = if (selectedDevice == null || selectedDevice?.deviceType == 0) Color.White else Color.Green,
                                    fontSize = 11.sp,
                                    lineHeight = 11.sp,
                                    maxLines = 1,
                                    letterSpacing = 0.2.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (song!!.serverId != null) {
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Check out this song on Purrytify!\n\npurrytify://song/${song!!.serverId}"
                                                )
                                                type = "text/plain"
                                            }
                                            val chooser =
                                                Intent.createChooser(
                                                    shareIntent,
                                                    "Share song via..."
                                                )
                                            context.startActivity(chooser)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { showQueueSheet = true },
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = "View Queue",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
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
            onSongClick = { songCurr ->
                scope.launch {
                    globalViewModel.playSongIndex(songCurr)
                }
            },
            sheetState = queueSheetState,
            currIdx = currIdx,
            onMove = { from, to ->
                globalViewModel.moveQueue(from, to)
            }
        )
    }

    if (showDeviceSheet) {
        DeviceSheet(
            viewModel = audioDeviceViewModel,
            onDismiss = {
                scope.launch {
                    deviceSheetState.hide()
                    showDeviceSheet = false
                }
            },
            sheetState = deviceSheetState
        )
    }
}