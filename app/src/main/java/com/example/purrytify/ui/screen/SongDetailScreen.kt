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
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

private fun extractColorsFromImage(
    context: Context,
    onColorsExtracted: (List<Color>) -> Unit
) {
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.starboy)

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
        executor.shutdown()
    }
}

@Composable
fun SongDetailScreen(songId: String, navController: NavHostController, modifier: Modifier = Modifier) {

    var gradientColors by remember { mutableStateOf(listOf(Color(0x0064B5F6), Color(0x000D47A1))) }
    var u = 2
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(u) {
        extractColorsFromImage(context) { colors ->
            gradientColors = colors
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

                IconButton(onClick = {  }) {
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
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.starboy),
                    contentDescription = "Album Cover",
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
                        text = "Starboy",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "The Weeknd, Daft Punk",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }

                IconButton(onClick = { /* Toggle favorite */ }) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}