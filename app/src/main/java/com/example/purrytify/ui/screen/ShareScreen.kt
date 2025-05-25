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

    val viewModel: ShareMonthlyCapsuleViewModel = viewModel(
        factory = ShareMonthlyCapsuleViewModel.ShareMonthlyCapsuleViewModelFactory(
            context.applicationContext as Application,
            globalViewModel,
            month,
            year
        )
    )

    // Get the full capsule data from your ViewModel
    val topSongsState by viewModel.topSongs.collectAsState()
    val topArtistsState by viewModel.topArtists.collectAsState()
    val totalListening by viewModel.totalListening.collectAsState()

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

        // Shareable content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            MonthlyCapsuleShareContent(
                month = month,
                year = year,
                topArtists = topArtistsState,
                topSongs = topSongsState,
                totalListening = totalListening,
                modifier = Modifier.weight(1f)
            )

            // Share button
            Button(
                onClick = {
                    // Create and share bitmap programmatically
                    createAndShareBitmap(
                        context = context,
                        month = month,
                        year = year,
                        topArtists = topArtistsState,
                        topSongs = topSongsState
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text(
                    text = "Share My Sound Capsule",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MonthlyCapsuleShareContent(
    month: Int,
    year: Int,
    topArtists: List<ArtistWithPlayCount>,
    topSongs: List<SongWithPlayCount>,
    totalListening: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF212121)),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section with logo and date
                Column(modifier = Modifier
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
                        text = "$totalListening minutes",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    )
                }
            }
        }
    }
}

// Create bitmap programmatically without relying on Compose drawing
fun createAndShareBitmap(
    context: Context,
    month: Int,
    year: Int,
    topArtists: List<ArtistWithPlayCount>,
    topSongs: List<SongWithPlayCount>
) {
    try {
        // Create bitmap with specific dimensions
        val width = 800
        val height = 1200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Set up paint objects
        val backgroundPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#212121")
        }

        val whitePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 48f
            isAntiAlias = true
        }

        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 64f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subtitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 40f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 32f
            isAntiAlias = true
        }

        val greenPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#4CAF50")
            textSize = 72f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // Draw background
        canvas.drawRect(0f, 150f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw header section
        var yPos = 200f
        canvas.drawText("Purritify", 60f, yPos, whitePaint)
        canvas.drawText(getCurrentFormattedDate(), width - 250f, yPos, textPaint)

        yPos += 100f
        canvas.drawText("My ${getMonthName(month)} Sound Capsule", 60f, yPos, titlePaint)

        // Draw top artists section
        yPos += 150f
        canvas.drawText("Top artists", 60f, yPos, subtitlePaint)
        yPos += 60f

        topArtists.take(5).forEachIndexed { index, artist ->
            canvas.drawText("${index + 1} ${artist.artist}", 60f, yPos, textPaint)
            yPos += 50f
        }

        // Draw top songs section
        yPos = 350f + 150f + 60f // Reset to align with artists
        canvas.drawText("Top songs", width/2f + 40f, yPos, subtitlePaint)
        yPos += 60f

        topSongs.take(5).forEachIndexed { index, song ->
            canvas.drawText("${index + 1} ${song.title}", width/2f + 40f, yPos, textPaint)
            yPos += 50f
        }

        // Draw time listened section
        yPos = height - 200f
        canvas.drawText("Time listened", 60f, yPos, subtitlePaint)
        yPos += 80f
        canvas.drawText("862 minutes", 60f, yPos, greenPaint)

        // Share the bitmap
        shareImage(context, bitmap, "Check out my ${getMonthName(month)} $year Sound Capsule!")

    } catch (e: Exception) {
        Log.e("CreateBitmap", "Error creating bitmap", e)
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