package com.example.purrytify.ui.screen

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.purrytify.ui.model.ArtistWithPlayCount
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.LoadImage
import com.example.purrytify.ui.model.ProfileViewModel
import com.example.purrytify.ui.model.SongWithPlayCount
import com.example.purrytify.ui.model.TopArtistViewModel
import com.example.purrytify.ui.model.TopSongViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TopMonthSongScreen(
    globalViewModel: GlobalViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val secondaryColor = Color(0xFF01667A)
    val viewModel: TopSongViewModel = viewModel(
        factory = TopSongViewModel.TopSongViewModelFactory(
            context.applicationContext as Application,
            globalViewModel)
    )
    val scope = rememberCoroutineScope()

    val topSongsState by viewModel.topSongs.collectAsState()
    val currentMonth = YearMonth.now()
    val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val year = currentMonth.year

    Scaffold(
        topBar = {
            TopAppBar(
                title = "Top songs",
                onBackClicked = { navController.popBackStack() }
            )
        },
        modifier = modifier.padding(top = 30.dp),
//        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
//                .background(Color.Black)
                .padding(horizontal = 16.dp)
        ) {
            MonthHeader(month = "$monthName $year")
            val text = buildAnnotatedString {
                append("You listened to ")
                pushStyle(SpanStyle(color = Color(0xFFFFEB3B)))
                append("${topSongsState.size} songs")
                pop()
                append(" this month.")
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Divider(color = Color(0xFF333333), thickness = 1.dp)

            LazyColumn {
                itemsIndexed(topSongsState) { index, song ->
                    SongListItem(
                        position = index + 1,
                        song = song,
                        onClick = {
                            // Navigate to artist details screen
                            // navController.navigate("artist_details/${artist.artist}")
                        }
                    )

                    HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun TopAppBar(title: String, onBackClicked: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
//            .background(Color.Black)
            .padding(start = 8.dp, end = 16.dp)
    ) {
        IconButton(onClick = onBackClicked) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MonthHeader(month: String) {
    Text(
        text = month,
        color = Color(0xFF999999),
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 16.dp)
    )
}

@Composable
private fun SongListItem(
    position: Int,
    song: SongWithPlayCount,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = String.format("%02d", position),
            color = Color(0xFFFFEB3B),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(30.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${song.playCount} plays",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        LoadImage(
            imagePath = song.imagePath,
            contentDescription = song.title,
            modifier = Modifier
                .size(60.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
    }
}