package com.example.purrytify.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.example.purrytify.R
import com.example.purrytify.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    val songs = remember {
        mutableStateListOf(
            Song("1", "Starboy", "The Weeknd, Daft Punk", R.drawable.starboy),
            Song("2", "Here Comes The Sun", "The Beatles", R.drawable.starboy),
            Song("3", "Midnight Pretenders", "Tomoko Aran", R.drawable.midnight),
            Song("4", "Violent Crimes", "Kanye West", R.drawable.violent),
            Song("5", "DENIAL IS A RIVER", "Doechii", R.drawable.starboy),
            Song("6", "Doomsday", "MF DOOM, Pebbles The Invisible Girl", R.drawable.midnight),
            Song("7", "Doomsday", "MF DOOM, Pebbles The Invisible Girl", R.drawable.midnight),
            Song("8", "Doomsday", "MF DOOM, Pebbles The Invisible Girl", R.drawable.midnight),
            Song("9", "Doomsday", "MF DOOM, Pebbles The Invisible Girl", R.drawable.midnight)
        )
    }

    var showUploadDialog by remember { mutableStateOf(false) }
    var currentPlayingSong by remember { mutableStateOf(songs[0]) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val accentGreen = Color(0xFF1DB954)

    Box(
        modifier = Modifier
            .fillMaxWidth().padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Library",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { showUploadDialog = true }
                )
            }

            Row(
                modifier = Modifier
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (true) accentGreen else Color.DarkGray)
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "All",
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.DarkGray)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Liked",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Song list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 16.dp)
            ) {
                items(songs) { song ->
                    RecentlyPlayedItem(song = song, onClick = {
                        navController.navigate(Screen.SongDetail.createRoute(song.id))
                    })
                }
            }
        }

        // Upload song dialog
        if (showUploadDialog) {
            UploadSongDialog(
                onDismiss = { showUploadDialog = false },
                onSave = { title, artist ->
                    if (title.isNotEmpty() && artist.isNotEmpty()) {
                        songs.add(Song((songs.size + 1).toString(), title, artist, R.drawable.starboy))
                        showUploadDialog = false
                    }
                },
                sheetState = sheetState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadSongDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    sheetState: SheetState,
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
//    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
//    val selectedAudioUri by viewModel.selectedAudioUri.collectAsState()
//
//    val imagePickerLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent()
//    ) { uri: Uri? ->
//        uri?.let { viewModel.setSelectedImageUri(it) }
//    }
//
//    val audioPickerLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent()
//    ) { uri: Uri? ->
//        uri?.let { viewModel.setSelectedAudioUri(it) }
//    }

    ModalBottomSheet (
        onDismissRequest = onDismiss,
        tonalElevation = 16.dp,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Upload Song",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            // Upload buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Upload Photo button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray),
//                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
//                        if (selectedImageUri != null) {
//                            Image(
//                                painter = rememberAsyncImagePainter(selectedImageUri),
//                                contentDescription = "Selected Image",
//                                modifier = Modifier.fillMaxSize(),
//                                contentScale = ContentScale.Crop
//                            )
//                        } else {
//                            Icon(
//                                imageVector = Icons.Outlined.Image,
//                                contentDescription = "Upload Photo",
//                                tint = Color.Gray,
//                                modifier = Modifier.size(48.dp)
//                            )
//                        }
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "Upload Photo",
                            tint = Color.Gray,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Text(
                        text = "Upload Photo",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Upload File button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray),
//                            .clickable { audioPickerLauncher.launch("audio/*") },
                        contentAlignment = Alignment.Center
                    ) {
//                        if (selectedAudioUri != null) {
//                            Icon(
//                                imageVector = Icons.Default.AudioFile,
//                                contentDescription = "Audio Selected",
//                                tint = Color(0xFF1DB954),
//                                modifier = Modifier.size(48.dp)
//                            )
//                        } else {
//                            // Simple waveform icon
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.SpaceEvenly,
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                for (i in 1..5) {
//                                    Box(
//                                        modifier = Modifier
//                                            .width(6.dp)
//                                            .height((20 + (i % 3) * 10).dp)
//                                            .background(Color.Gray)
//                                    )
//                                }
//                            }
//                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..3) {
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height((10 + i * 10).dp)
                                        .background(Color.Gray)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height((10 + 2 * 10).dp)
                                    .background(Color.Gray)
                            )
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height((10 + 1 * 10).dp)
                                    .background(Color.Gray)
                            )
                        }
                    }
                    Text(
                        text = "Upload File",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Title field
            Text(
                text = "Title",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.DarkGray,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            // Artist field
            Text(
                text = "Artist",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                placeholder = { Text("Artist", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.DarkGray,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cancel button
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.Gray),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }

                // Save button
                Button(
                    onClick = { onSave(title, artist) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text("Save")
                }
            }
        }
    }
}
