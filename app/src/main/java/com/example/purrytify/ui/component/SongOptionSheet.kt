package com.example.purrytify.ui.component

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.purrytify.data.model.Song
import com.example.purrytify.ui.model.ImageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
    song: Song,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    sheetState: SheetState,
    detail: Boolean = false,
    onAddToQueue: () -> Unit,
    onLiked: () -> Unit,
    onStartNewRadio: () -> Unit,
    onAddToNext: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Song") },
            text = { Text("Are you sure you want to delete \"${song.title}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                    onDismiss()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ImageLoader.LoadImage(
                    imagePath = song.imagePath,
                    contentDescription = "${song.title} album cover",
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(song.title)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SheetOption(icon = Icons.AutoMirrored.Filled.QueueMusic, text = "Add to Queue") {
                onAddToQueue()
                Toast.makeText(context, "Added To Queue", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            SheetOption(icon = Icons.Default.LibraryMusic, text = "Add to Next") {
                onAddToNext()
                Toast.makeText(context, "Added To Next", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            SheetOption(icon = Icons.Default.Radio, text = "Start New Radio") {
                onStartNewRadio()
                Toast.makeText(context, "Starting new radio", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
            SheetOption(
                icon = Icons.Default.Favorite,
                text = if (song.isLiked) "Remove from like" else "Add to like",
                onClick = {
                    onLiked()
                    onDismiss()
                },
                tint = if (song.isLiked) Color(0xFFFF4081) else Color.White
            )
            if (!detail) {
                SheetOption(icon = Icons.Default.Delete, text = "Delete Song") {
                    showDeleteConfirmation = true
                }
                SheetOption(icon = Icons.Default.Edit, text = "Edit Song", onClick = onEdit)
            }
        }
    }
}

@Composable
fun SheetOption(
    icon: ImageVector,
    text: String,
    tint: Color = LocalContentColor.current,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = tint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text)
    }
}
