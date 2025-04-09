package com.example.purrytify.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.purrytify.ui.model.ImageLoader
import com.example.purrytify.ui.model.SongDetailViewModel
import com.example.purrytify.ui.theme.Poppins
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

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
@Composable
fun SongDetailScreen(songId: String, navController: NavHostController, modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val viewModel: SongDetailViewModel = viewModel(
        factory = SongDetailViewModel.SongDetailViewModelFactory(context.applicationContext as android.app.Application)
    )

    LaunchedEffect(songId) {
        viewModel.loadSong(songId)
    }

    val song by viewModel.currentSong.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()

    var gradientColors by remember { mutableStateOf(listOf(Color(0x0064B5F6), Color(0x000D47A1))) }
    val scrollState = rememberScrollState()

    LaunchedEffect(song) {
        song?.let {
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
            }
        }
    }
}