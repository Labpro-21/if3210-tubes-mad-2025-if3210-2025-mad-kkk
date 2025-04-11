package com.example.purrytify.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.purrytify.R
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.palette.graphics.Palette
import com.example.purrytify.navigation.Screen
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.ImageLoader
import com.example.purrytify.ui.model.SongDetailViewModel
import com.example.purrytify.ui.theme.Poppins
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private fun extractColorsFromImage(
    context: Context,
    argImagePath: String?,
    onColorsExtracted: (List<Color>) -> Unit
) {
    val imagePath = argImagePath ?: R.drawable.starboy.toString()
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        val bitmap: Bitmap? = when {
            imagePath.toIntOrNull() != null -> {
                val resourceId = imagePath.toIntOrNull() ?: R.drawable.starboy
                BitmapFactory.decodeResource(context.resources, resourceId)
            }
            File(imagePath).exists() -> {
                BitmapFactory.decodeFile(imagePath)
            }
            else -> {
                BitmapFactory.decodeResource(context.resources, R.drawable.starboy)
            }
        }

        if (bitmap == null) {
            val defaultBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.starboy)
            processBitmap(defaultBitmap, onColorsExtracted)
            executor.shutdown()
            return@execute
        }

        processBitmap(bitmap, onColorsExtracted)
        executor.shutdown()
    }
}

private fun processBitmap(bitmap: Bitmap, onColorsExtracted: (List<Color>) -> Unit) {
    val width = bitmap.width
    val height = bitmap.height

    val topHalfBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height / 2)
    val bottomHalfBitmap = Bitmap.createBitmap(bitmap, 0, height / 2, width, height / 2)

    Palette.from(topHalfBitmap).generate { palette ->
        val dominantColor = palette?.dominantSwatch?.rgb ?: android.graphics.Color.WHITE
        val composeDominantColor = Color(dominantColor)

        Palette.from(bottomHalfBitmap).generate { palette ->
            val secondaryColor = palette?.dominantSwatch?.rgb ?: android.graphics.Color.WHITE
            val composeSecondaryColor = Color(secondaryColor)
            Handler(Looper.getMainLooper()).post {
                onColorsExtracted(listOf(composeDominantColor, composeSecondaryColor))
            }
        }
    }
}

private fun formatTime(seconds: Double): String {
    val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
    val remainingSeconds = seconds.toLong() - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(songId: String, navController: NavHostController, modifier: Modifier = Modifier, globalViewModel: GlobalViewModel) {

    val context = LocalContext.current
    val viewModel: SongDetailViewModel = viewModel(
        factory = SongDetailViewModel.SongDetailViewModelFactory(context.applicationContext as android.app.Application)
    )

    LaunchedEffect(songId) {
        viewModel.loadSong(songId)
    }

    val song by viewModel.currentSong.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()

    val isPlaying by globalViewModel.isPlaying.collectAsState()
    val sliderPosition by globalViewModel.currentPosition.collectAsState()
    val duration by globalViewModel.duration.collectAsState()

    val validDuration = remember(duration) {
        maxOf(0.1, duration).toFloat()
    }

    val validPosition = remember(sliderPosition, validDuration) {
        sliderPosition.toFloat().coerceIn(0f, validDuration)
    }

    var gradientColors by remember { mutableStateOf(listOf(Color(0x0064B5F6), Color(0x000D47A1))) }
    val scrollState = rememberScrollState()

    LaunchedEffect(song) {
        song?.let {
            Log.d("LOG SONG", it.title)
            extractColorsFromImage(context, song!!.imagePath) { extractedColors ->
                gradientColors = extractedColors + Color(0xFF101510)
            }
        }
    }

    Box(
        modifier = modifier
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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { /* More options menu */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    // Use ImageLoader instead of Image
                    ImageLoader.LoadImage(
                        imagePath = song?.imagePath,
                        contentDescription = "${song?.title} Album Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

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

                    IconButton(onClick = { viewModel.toggleLikedStatus() }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isLiked) Color(0xFFFF4081) else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Slider(
                        value = validPosition,
                        onValueChange = { },
                        // onValueChange = { sliderPosition = it },
                        valueRange = 0f..validDuration,
                        thumb = {
                            Spacer(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color.White, CircleShape),
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

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            var prevId = globalViewModel.playPreviousSong()

                            if (prevId != 0L) {
                                navController.navigate(Screen.SongDetail.createRoute(prevId.toString())) {
                                    launchSingleTop = true
//                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
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
                        onClick = { /* isPlaying = !isPlaying */  },
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
                            var nextId = globalViewModel.playNextSong()

                            if (nextId != 0L) {
                                navController.navigate(Screen.SongDetail.createRoute(nextId.toString())) {
                                    launchSingleTop = true
                                }
                            }

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
                }

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}