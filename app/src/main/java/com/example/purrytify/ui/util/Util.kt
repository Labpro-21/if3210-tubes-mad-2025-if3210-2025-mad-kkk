package com.example.purrytify.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.purrytify.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

suspend fun extractColorsFromImage(
    context: Context,
    imageUri: Uri?
): List<Color> = withContext(Dispatchers.IO) {
    val bitmap = try {
        when {
            imageUri == null -> null

            imageUri.scheme == "http" || imageUri.scheme == "https" -> {
                val request = ImageRequest.Builder(context)
                    .data(imageUri.toString())
                    .allowHardware(false)
                    .build()

                val result = (ImageLoader(context).execute(request) as? SuccessResult)
                (result?.drawable as? BitmapDrawable)?.bitmap
            }

            else -> {
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } ?: BitmapFactory.decodeResource(context.resources, R.drawable.placeholder)

    processBitmapForColors(bitmap)
}


suspend fun processBitmapForColors(bitmap: Bitmap): List<Color> =
    suspendCancellableCoroutine { continuation ->
        val width = bitmap.width
        val height = bitmap.height

        val topHalf = Bitmap.createBitmap(bitmap, 0, 0, width, height / 2)
        val bottomHalf = Bitmap.createBitmap(bitmap, 0, height / 2, width, height / 2)

        Palette.from(topHalf).generate { topPalette ->
            val primary = topPalette?.dominantSwatch?.rgb ?: android.graphics.Color.WHITE
            val primaryColor = Color(primary)

            Palette.from(bottomHalf).generate { bottomPalette ->
                val secondary = bottomPalette?.dominantSwatch?.rgb ?: android.graphics.Color.WHITE
                val secondaryColor = Color(secondary)

                if (continuation.isActive) {
                    continuation.resume(listOf(primaryColor, secondaryColor))
                }
            }
        }
    }
