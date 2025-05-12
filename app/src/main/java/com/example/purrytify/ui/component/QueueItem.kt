package com.example.purrytify.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.ui.model.ImageLoader
import com.example.purrytify.ui.theme.Poppins

@Composable
fun QueueItem(
    song: com.example.purrytify.data.model.Song,
    currSong: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(24.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            ImageLoader.LoadImage(
                imagePath = song.imagePath,
                contentDescription = "${song.title} Album Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = Poppins,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (currSong) Color.Green else Color.White
            )

            Text(
                text = song.artist,
                fontSize = 12.sp,
                color = Color.Gray,
                fontFamily = Poppins,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.Dehaze,
            contentDescription = "Drag Handle",
            tint = Color.White,
            modifier = Modifier
                .size(20.dp)
                .then(modifier)
        )
        Spacer(modifier = Modifier.width(24.dp))
    }
}