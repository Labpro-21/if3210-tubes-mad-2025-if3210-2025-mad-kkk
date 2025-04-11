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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.purrytify.data.model.Song
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.ImageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(song: Song, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, sheetState: SheetState, detail: Boolean = false, onAddToQueue: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        var context = LocalContext.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ImageLoader.LoadImage(
                    imagePath = song.imagePath,
                    contentDescription = "${song.title} album cover",
                    Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(song.title, fontWeight = FontWeight.Bold)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SheetOption(icon = Icons.Default.QueueMusic, text = "Add to Queue") {
                onAddToQueue()
                Toast.makeText(context, "Added To Queue", Toast.LENGTH_SHORT).show()
                onDismiss()
            } // TODO: change later
            SheetOption(icon = Icons.Default.Favorite, text = "Add to Like") { onDismiss() } //  TODO: change later
            if (!detail) {
                SheetOption(icon = Icons.Default.Delete, text = "Delete Song") { onDelete() }
                SheetOption(icon = Icons.Default.Edit, text = "Edit Song") { onEdit() }
            }
        }
    }
}

@Composable
fun SheetOption(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text)
    }
}
