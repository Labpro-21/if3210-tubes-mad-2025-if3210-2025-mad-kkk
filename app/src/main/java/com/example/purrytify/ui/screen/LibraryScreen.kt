package com.example.purrytify.ui.screen

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.purrytify.R
import com.example.purrytify.data.model.Song
import com.example.purrytify.navigation.Screen
import com.example.purrytify.ui.component.EditSongBottomSheet
import com.example.purrytify.ui.component.SongOptionsSheet
import com.example.purrytify.ui.component.UploadSongBottomSheet
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.LibraryViewModel
import com.example.purrytify.worker.LogoutListener
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    showDetail: () -> Unit,
    globalViewModel: GlobalViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.LibraryViewModelFactory(
            context.applicationContext as android.app.Application,
            globalViewModel
        )
    )

    val songs by viewModel.songs.collectAsState()
    val filterType by viewModel.filterType.collectAsState()

    var showUploadDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val accentGreen = Color(0xFF1DB954)

    val searchQuery by viewModel.searchQuery.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    val focusManager = LocalFocusManager.current

    var showEditDialog by remember { mutableStateOf(false) }
    var showSongOptionSheet by remember { mutableStateOf(false) }
    var showSong by remember { mutableStateOf<Song?>(null) }
    val editSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val songOptionSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val currentSong by globalViewModel.currentSong.collectAsState()

    LogoutListener {
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
        globalViewModel.clearUserId()
        globalViewModel.logout()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .zIndex(10f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Library",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    placeholder = { Text("Search songs or artists") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Gray
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                        focusedContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                        cursorColor = accentGreen,
                        focusedBorderColor = accentGreen,
                        unfocusedBorderColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
                )

                Box(modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth()
                    .zIndex(10f))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .zIndex(10f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (filterType == LibraryViewModel.FilterType.ALL) accentGreen else Color.DarkGray)
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clickable {
                                viewModel.setFilter(
                                    LibraryViewModel.FilterType.ALL
                                )
                            }
                    ) {
                        Text(
                            text = "All",
                            color = if (filterType == LibraryViewModel.FilterType.ALL) Color.Black else Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (filterType == LibraryViewModel.FilterType.LIKED) accentGreen else Color.DarkGray)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable {
                                viewModel.setFilter(
                                    LibraryViewModel.FilterType.LIKED
                                )
                            }
                    ) {
                        Text(
                            text = "Liked",
                            color = if (filterType == LibraryViewModel.FilterType.LIKED) Color.Black else Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            AndroidView(
                factory = { context ->
                    RecyclerView(context).apply {
                        id = View.generateViewId()
                        layoutManager = LinearLayoutManager(context)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        clipToPadding = false
                        setPadding(0, 0, 0, 100)
                        setOnTouchListener { view, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                focusManager.clearFocus()
                            }
                            if (event.action == MotionEvent.ACTION_UP) {
                                view.performClick()
                            }
                            false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    },
                update = { recyclerView ->
                    val adapter = SongAdapter(
                        songs,
                        context,
                        { song ->
                            globalViewModel.playSong(song)
                            showDetail()
                        },
                        { song ->
                            showSong = song
                            showSongOptionSheet = true
                        }
                    )
                    recyclerView.adapter = adapter
                }
            )
        }

        if (showUploadDialog) {
            UploadSongBottomSheet(
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showUploadDialog = false
                    }
                },
                onSave = { title, artist ->
                    if (title.isNotEmpty() && artist.isNotEmpty() && viewModel.selectedAudioUri.value != null && viewModel.selectedImageUri.value != null) {
                        viewModel.uploadSong(title, artist)
                        scope.launch {
                            sheetState.hide()
                            showUploadDialog = false
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Please fill all fields and select both audio and image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                sheetState = sheetState,
                viewModel = viewModel
            )
        }

        if (showSongOptionSheet && showSong != null) {
            SongOptionsSheet(
                song = showSong!!,
                onDismiss = {
                    scope.launch {
                        songOptionSheetState.hide()
                        showSongOptionSheet = false
                    }
                },
                onEdit = {
                    scope.launch {
                        songOptionSheetState.hide()
                        showSongOptionSheet = false
                        showEditDialog = true
                    }
                },
                onDelete = {
                    viewModel.deleteSong(showSong!!)
                    scope.launch {
                        songOptionSheetState.hide()
                        showSongOptionSheet = false
                    }
                },
                sheetState = songOptionSheetState,
                onAddToQueue = {
                    globalViewModel.addToQueue(showSong!!)
                },
                detail = showSong?.id == currentSong?.id,
                onLiked = {
                    viewModel.toggleLiked(showSong!!)
                },
                onStartNewRadio = {
                    globalViewModel.playSongs(showSong!!)
                },
                onAddToNext = {
                    globalViewModel.addToNext(showSong!!)
                }
            )
        }

        if (showEditDialog && showSong != null) {
            EditSongBottomSheet(
                song = showSong!!,
                onDismiss = {
                    scope.launch {
                        editSheetState.hide()
                        showEditDialog = false
                    }
                },
                sheetState = editSheetState,
                onUpdate = { id, title, artist, image, audio ->
                    if (title.isNotEmpty() && artist.isNotEmpty()) {
                        viewModel.updateSong(id, title, artist, image, audio)
                        scope.launch {
                            editSheetState.hide()
                            showEditDialog = false
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Please fill all fields and select both audio and image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }
}


class SongAdapter(
    private val songs: List<Song>,
    val context: Context,
    private val onItemClick: (Song) -> Unit,
    private val onSongOptionClick: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songImage: ImageView = itemView.findViewById(R.id.song_image)
        val songTitle: TextView = itemView.findViewById(R.id.song_title)
        val songArtist: TextView = itemView.findViewById(R.id.song_artist)
        val moreOptionsButton: ImageButton = itemView.findViewById(R.id.more_options)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]

        holder.songTitle.text = song.title
        holder.songArtist.text = song.artist
        val path = song.imagePath
        val img = when {
            path.toIntOrNull() != null -> {
                val resID = path.toIntOrNull() ?: R.drawable.starboy
                BitmapFactory.decodeResource(context.resources, resID)
            }

            File(path).exists() -> {
                BitmapFactory.decodeFile(path)
            }

            else -> {
                BitmapFactory.decodeResource(context.resources, R.drawable.starboy)
            }
        }

        // Load image using Coil
        holder.songImage.load(img) {
            crossfade(true)
//            placeholder(R.drawable.placeholder_image)
//            error(R.drawable.error_image)
        }

        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(song)
        }

        holder.moreOptionsButton.setOnClickListener {
            onSongOptionClick(song)
        }

        holder.itemView.setOnLongClickListener {
            onSongOptionClick(song)
            true
        }
    }

    override fun getItemCount(): Int = songs.size
}