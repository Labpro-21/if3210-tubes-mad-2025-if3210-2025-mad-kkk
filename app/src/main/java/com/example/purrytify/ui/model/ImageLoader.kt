package com.example.purrytify.ui.model

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.purrytify.R
import java.io.File

object ImageLoader {
    @Composable
    fun LoadImage(
        imagePath: String?,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        contentScale: ContentScale = ContentScale.Crop
    ) {
        val context = LocalContext.current
        val defaultImage = R.drawable.starboy

        val painter = when {
            imagePath.isNullOrBlank() -> {
                painterResource(id = defaultImage)
            }

            imagePath.toIntOrNull() != null -> {
                painterResource(id = imagePath.toInt())
            }

            imagePath.startsWith("http://") || imagePath.startsWith("https://") -> {
                rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(imagePath)
                        .crossfade(true)
                        .error(defaultImage)
                        .build()
                )
            }

            else -> {
                val file = File(imagePath)
                if (file.exists()) {
                    rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(file)
                            .error(defaultImage)
                            .build()
                    )
                } else {
                    painterResource(id = defaultImage)
                }
            }
        }

        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}