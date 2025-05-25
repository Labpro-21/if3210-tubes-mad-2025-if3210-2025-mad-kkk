package com.example.purrytify.ui.screen

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.core.content.FileProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.R
import com.example.purrytify.ui.model.ListeningStreak
import com.example.purrytify.ui.model.LoadImage
import com.example.purrytify.ui.model.MonthlySoundCapsule
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Month
import java.util.*
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.purrytify.ui.model.ArtistWithPlayCount
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.ShareMonthlyCapsuleViewModel
import com.example.purrytify.ui.model.SongWithPlayCount
import com.example.purrytify.ui.model.TopArtistViewModel

@Composable
fun ShareMonthlyCapsuleScreen(
    globalViewModel: GlobalViewModel,
    navController: NavController,
    month: Int,
    year: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val viewModel: ShareMonthlyCapsuleViewModel = viewModel(
        factory = ShareMonthlyCapsuleViewModel.ShareMonthlyCapsuleViewModelFactory(
            context.applicationContext as Application,
            globalViewModel)
    )

    // Get the full capsule data from your ViewModel
    val topSongsState by viewModel.topSongs.collectAsState()
    val topArtistsState by viewModel.topArtists.collectAsState()

    LaunchedEffect(capturedBitmap) {
        capturedBitmap?.let { bitmap ->
            shareImage(context, bitmap, "Check out my ${getMonthName(month)} $year Sound Capsule!")
//            navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 32.dp)
//            .background(Color.Black)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Share",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }

//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(24.dp)) {
//                Icon(
//                    painter = painterResource(id = R.drawable.ic_close),
//                    contentDescription = "Close",
//                    tint = Color.White
//                )
//            }
//
//            Text(
//                text = "Share",
//                color = Color.White,
//                fontWeight = FontWeight.Medium,
//                fontSize = 18.sp
//            )
//
//            Spacer(modifier = Modifier.width(48.dp))
//        }

        // Shareable content - Fixed positioning with more height
        Row(
            modifier = Modifier
                .fillMaxSize()
//                .fillMaxHeight(0.8f) // Take up more of the screen height
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            MonthlyCapsuleShareContent(
                month = month,
                year = year,
                topArtists = topArtistsState,
                topSongs = topSongsState,
                onBitmapCaptured = { bitmap ->
                    capturedBitmap = bitmap
                }
            )
        }
    }
}

@Composable
fun MonthlyCapsuleShareContent(
    month: Int,
    year: Int,
    topArtists: List<ArtistWithPlayCount>,
    topSongs: List<SongWithPlayCount>,
    onBitmapCaptured: (Bitmap) -> Unit
) {
    Column(
        modifier = Modifier
//            .aspectRatio(0.6f) // Changed from 0.75f to make it taller
            .clip(RoundedCornerShape(16.dp))
            .capturable(onBitmapCaptured)
    ) {
        Column (
            modifier = Modifier.fillMaxSize()
        ) {
            // Background with top song image
            LoadImage(
                imagePath = (if (topSongs.isEmpty()) {
                    R.drawable.placeholder
                } else {
                    topSongs[0].imagePath
                }).toString(),
                contentDescription = "Album Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            // Dark overlay
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(
//                    Brush.verticalGradient(
//                        colors = listOf(
//                            Color.Black.copy(alpha = 0.3f),
//                            Color.Black.copy(alpha = 0.8f)
//                        ),
//                        startY = 0f,
//                        endY = Float.POSITIVE_INFINITY
//                    )
//                )
//        )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF212121)),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section with logo and date
                Column (modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.logo_3),
                            contentDescription = "Purritify",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Purritify",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = getCurrentFormattedDate(),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "My ${getMonthName(month)} Sound Capsule",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp
                    )
                }

//                Spacer(modifier = Modifier.height(16.dp))

                // Middle section with top artists and songs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Top artists",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        topArtists.take(5).forEachIndexed { index, artist ->
                            Text(
                                text = "${index + 1} ${artist.artist}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Top songs",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        topSongs.take(5).forEachIndexed { index, song ->
                            Text(
                                text = "${index + 1} ${song.title}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Bottom section - Time listened
                Column(modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)) {
                    Text(
                        text = "Time listened",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "862 minutes", // You can pass this as parameter or calculate from data
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    )
                }
            }
        }
    }
}

// Fixed bitmap capture extension
@Composable
fun Modifier.capturable(onBitmapCaptured: (Bitmap) -> Unit): Modifier {
    val context = LocalContext.current

    return this.drawWithContent {
        drawContent()

        // Create bitmap after drawing
        val bitmap = Bitmap.createBitmap(
            size.width.toInt(),
            size.height.toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)

        // Draw the content onto the bitmap canvas
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val checkPoint = nativeCanvas.save()

        try {
            drawIntoCanvas { canvas ->
                draw(this, layoutDirection, canvas, size) {
                    this@drawWithContent.drawContent()
                }
            }
            onBitmapCaptured(bitmap)
        } finally {
            nativeCanvas.restoreToCount(checkPoint)
        }
    }
}

// Helper function to get month name
private fun getMonthName(month: Int): String {
    return Month.of(month).name.lowercase().replaceFirstChar { it.uppercase() }
}

// Share functionality - Fixed with correct authority
fun shareImage(context: Context, bitmap: Bitmap, title: String) {
    try {
        val file = File(context.cacheDir, "share_image_${System.currentTimeMillis()}.png")
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()

        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, title)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share via"))

    } catch (e: Exception) {
        Log.e("ShareImage", "Error sharing image", e)
    }
}

// Helper function
private fun getCurrentFormattedDate(): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(Date())
}