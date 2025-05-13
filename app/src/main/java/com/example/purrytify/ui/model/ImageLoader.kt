package com.example.purrytify.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.R
import java.io.File

@Composable
fun LoadImage(
    imagePath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imagePath)
            .crossfade(true)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .build(),
        placeholder = painterResource(R.drawable.placeholder),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

