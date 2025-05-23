package com.example.purrytify.ui.screen

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toIntSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.*
import com.example.purrytify.R
import com.example.purrytify.ui.model.EditProfileViewModel
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.TopGlobalViewModel
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.IOException

// Enum to track which part of the crop box is being interacted with
enum class DragHandle {
    NONE,
    INSIDE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(
    globalViewModel: GlobalViewModel,
    navController: NavController,
    imageUriString: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val viewModel: EditProfileViewModel = viewModel(
        factory = EditProfileViewModel.EditProfileViewModelFactory(
            context.applicationContext as Application,
            globalViewModel
        )
    )

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var isProcessing = viewModel.isLoading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Crop box state
    var cropBoxPosition by remember { mutableStateOf(Offset.Zero) }
    var cropBoxSize by remember { mutableStateOf(Size.Zero) }
    var activeDragHandle by remember { mutableStateOf(DragHandle.NONE) }

    // Image bounds for constraining the crop box
    var imageBounds by remember { mutableStateOf(Rect.Zero) }

    // Min size for crop box (in pixels)
    val minCropSize = with(density) { 100.dp.toPx() }

    var isResizing by remember { mutableStateOf(false) }

    // Decode the URI string
    LaunchedEffect(imageUriString) {
        imageUriString?.let { uriString ->
            try {
                imageUri = Uri.decode(uriString).let { Uri.parse(it) }
                // Load bitmap from URI with proper orientation
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(imageUri!!)?.use { inputStream ->
                        // First decode just the bounds to get orientation info
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeStream(inputStream, null, options)

                        // Reset the stream
                        context.contentResolver.openInputStream(imageUri!!)?.use { resetStream ->
                            // Now fully decode the image
                            val bitmap = BitmapFactory.decodeStream(resetStream)
                            bitmap?.let {
                                // Fix orientation if needed
                                val rotatedBitmap = fixImageRotation(context, bitmap, imageUri!!)
                                imageBitmap = rotatedBitmap.asImageBitmap()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Initialize crop box (centered square, 80% of the smaller dimension, within image bounds)
    LaunchedEffect(canvasSize, imageBitmap) {
        if (canvasSize != Size.Zero && cropBoxSize == Size.Zero && imageBitmap != null) {

            // Calculate image dimensions
            val imageAspect = imageBitmap!!.width.toFloat() / imageBitmap!!.height.toFloat()
            val canvasAspect = canvasSize.width / canvasSize.height

            val imageSize: Size
            val imageOffset: Offset

            if (imageAspect > canvasAspect) {
                // Image is wider than canvas (relative to their heights)
                val height = canvasSize.width / imageAspect
                imageSize = Size(canvasSize.width, height)
                imageOffset = Offset(0f, (canvasSize.height - height) / 2f)
            } else {
                // Image is taller than canvas (relative to their widths)
                val width = canvasSize.height * imageAspect
                imageSize = Size(width, canvasSize.height)
                imageOffset = Offset((canvasSize.width - width) / 2f, 0f)
            }

            // Define image bounds
            val imageBounds = Rect(imageOffset, imageSize)

            // Calculate crop box size (80% of the smaller image dimension)
            val size = minOf(imageSize.width, imageSize.height) * 0.8f
            cropBoxSize = Size(size, size)

            // Center the crop box within the image
            cropBoxPosition = Offset(
                imageOffset.x + (imageSize.width - size) / 2f,
                imageOffset.y + (imageSize.height - size) / 2f
            )
        }
    }

    // Show error dialog if there's an error
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Image Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            imageBitmap?.let { bitmap ->
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->

                                    isResizing = true

                                    // Determine which part of the crop box is being dragged
                                    val handleSize = 40f

                                    val cropRect = Rect(cropBoxPosition, cropBoxSize)

                                    // Check if we're on a corner handle
                                    val topLeft = cropBoxPosition
                                    val topRight = Offset(cropBoxPosition.x + cropBoxSize.width, cropBoxPosition.y)
                                    val bottomLeft = Offset(cropBoxPosition.x, cropBoxPosition.y + cropBoxSize.height)
                                    val bottomRight = Offset(cropBoxPosition.x + cropBoxSize.width, cropBoxPosition.y + cropBoxSize.height)

                                    activeDragHandle = when {
                                        // Corners (check first as they have priority)
                                        (offset - topLeft).getDistance() <= handleSize -> DragHandle.TOP_LEFT
                                        (offset - topRight).getDistance() <= handleSize -> DragHandle.TOP_RIGHT
                                        (offset - bottomLeft).getDistance() <= handleSize -> DragHandle.BOTTOM_LEFT
                                        (offset - bottomRight).getDistance() <= handleSize -> DragHandle.BOTTOM_RIGHT

                                        // Edges
                                        abs(offset.x - topLeft.x) <= handleSize &&
                                                offset.y in topLeft.y..bottomLeft.y -> DragHandle.LEFT

                                        abs(offset.x - topRight.x) <= handleSize &&
                                                offset.y in topRight.y..bottomRight.y -> DragHandle.RIGHT

                                        abs(offset.y - topLeft.y) <= handleSize &&
                                                offset.x in topLeft.x..topRight.x -> DragHandle.TOP

                                        abs(offset.y - bottomLeft.y) <= handleSize &&
                                                offset.x in bottomLeft.x..bottomRight.x -> DragHandle.BOTTOM

                                        // Inside the crop area
                                        cropRect.contains(offset) -> DragHandle.INSIDE

                                        // Nothing
                                        else -> DragHandle.NONE
                                    }
                                },
                                onDragEnd = {
                                    isResizing = false
                                    activeDragHandle = DragHandle.NONE
                                    // Perform final bounds check after drag ends
                                    val (finalPos, finalSize) = constrainCropBox(
                                        cropBoxPosition,
                                        cropBoxSize,
                                        imageBounds,
                                        minCropSize
                                    )
                                    cropBoxPosition = finalPos
                                    cropBoxSize = finalSize
                                },
                                onDragCancel = {
                                    activeDragHandle = DragHandle.NONE
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()

                                    when (activeDragHandle) {
                                        DragHandle.INSIDE -> {
                                            // Move the entire crop box
                                            val newPosition = cropBoxPosition + dragAmount

                                            // Keep the crop box within image bounds
                                            cropBoxPosition = Offset(
                                                x = newPosition.x.coerceIn(imageBounds.left, imageBounds.right - cropBoxSize.width),
                                                y = newPosition.y.coerceIn(imageBounds.top, imageBounds.bottom - cropBoxSize.height)
                                            )
                                        }

                                        DragHandle.TOP_LEFT -> {
                                            // Resize from top-left (maintain square aspect ratio)
                                            val delta = maxOf(dragAmount.x, dragAmount.y)
                                            val newSize = cropBoxSize.width - delta
                                            val newX = cropBoxPosition.x + delta
                                            val newY = cropBoxPosition.y + delta

                                            // Ensure minimum size and keep within image bounds
                                            if (newSize >= minCropSize &&
                                                newX >= imageBounds.left && newY >= imageBounds.top) {
                                                cropBoxSize = Size(newSize, newSize)
                                                cropBoxPosition = Offset(newX, newY)
                                            }
                                        }

                                        DragHandle.TOP_RIGHT -> {
                                            // Resize from top-right (maintain square aspect ratio)
                                            val deltaX = -dragAmount.x
                                            val deltaY = dragAmount.y
                                            val delta = maxOf(deltaX, deltaY)
                                            val newSize = cropBoxSize.width - delta
                                            val newY = cropBoxPosition.y + delta

                                            // Ensure minimum size and keep within image bounds
                                            if (newSize >= minCropSize &&
                                                cropBoxPosition.x + newSize <= imageBounds.right &&
                                                newY >= imageBounds.top) {
                                                cropBoxSize = Size(newSize, newSize)
                                                cropBoxPosition = Offset(cropBoxPosition.x, newY)
                                            }
                                        }

                                        DragHandle.BOTTOM_LEFT -> {
                                            // Resize from bottom-left (maintain square aspect ratio)
                                            val deltaX = dragAmount.x
                                            val deltaY = -dragAmount.y
                                            val delta = maxOf(deltaX, deltaY)
                                            val newSize = cropBoxSize.width - delta
                                            val newX = cropBoxPosition.x + delta

                                            // Ensure minimum size and keep within image bounds
                                            if (newSize >= minCropSize &&
                                                newX >= imageBounds.left &&
                                                cropBoxPosition.y + newSize <= imageBounds.bottom) {
                                                cropBoxSize = Size(newSize, newSize)
                                                cropBoxPosition = Offset(newX, cropBoxPosition.y)
                                            }
                                        }

                                        DragHandle.BOTTOM_RIGHT -> {
                                            // Resize from bottom-right (maintain square aspect ratio)
                                            val delta = minOf(dragAmount.x, dragAmount.y)
                                            val newSize = cropBoxSize.width + delta

                                            // Ensure minimum size and keep within image bounds
                                            if (newSize >= minCropSize &&
                                                cropBoxPosition.x + newSize <= imageBounds.right &&
                                                cropBoxPosition.y + newSize <= imageBounds.bottom) {
                                                cropBoxSize = Size(newSize, newSize)
                                            }
                                        }

                                        DragHandle.LEFT -> {
                                            // Resize from left edge (maintain square aspect ratio)
                                            val newSize = cropBoxSize.width - dragAmount.x
                                            val newX = cropBoxPosition.x + dragAmount.x

                                            // Check if we have room to grow/shrink vertically within image bounds
                                            val canResizeVertically = newSize >= minCropSize &&
                                                    cropBoxPosition.y + newSize <= imageBounds.bottom

                                            if (newSize >= minCropSize && newX >= imageBounds.left && canResizeVertically) {
                                                cropBoxSize = Size(newSize, newSize)
                                                cropBoxPosition = Offset(newX, cropBoxPosition.y)
                                            }
                                        }

                                        DragHandle.RIGHT -> {
                                            // Resize from right edge (maintain square aspect ratio)
                                            val newSize = cropBoxSize.width + dragAmount.x

                                            // Check if we have room to grow/shrink vertically within image bounds
                                            val canResizeVertically = newSize >= minCropSize &&
                                                    cropBoxPosition.y + newSize <= imageBounds.bottom

                                            if (newSize >= minCropSize &&
                                                cropBoxPosition.x + newSize <= imageBounds.right &&
                                                canResizeVertically) {
                                                cropBoxSize = Size(newSize, newSize)
                                            }
                                        }

                                        DragHandle.TOP -> {
                                            // Resize from top edge (maintain square aspect ratio)
                                            val newSize = cropBoxSize.height - dragAmount.y
                                            val newY = cropBoxPosition.y + dragAmount.y

                                            // Check if we have room to grow/shrink horizontally within image bounds
                                            val canResizeHorizontally = newSize >= minCropSize &&
                                                    cropBoxPosition.x + newSize <= imageBounds.right

                                            if (newSize >= minCropSize && newY >= imageBounds.top && canResizeHorizontally) {
                                                cropBoxSize = Size(newSize, newSize)
                                                cropBoxPosition = Offset(cropBoxPosition.x, newY)
                                            }
                                        }

                                        DragHandle.BOTTOM -> {
                                            // Resize from bottom edge (maintain square aspect ratio)
                                            val newSize = cropBoxSize.height + dragAmount.y

                                            // Check if we have room to grow/shrink horizontally within image bounds
                                            val canResizeHorizontally = newSize >= minCropSize &&
                                                    cropBoxPosition.x + newSize <= imageBounds.right

                                            if (newSize >= minCropSize &&
                                                cropBoxPosition.y + newSize <= imageBounds.bottom &&
                                                canResizeHorizontally) {
                                                cropBoxSize = Size(newSize, newSize)
                                            }
                                        }

                                        DragHandle.NONE -> {
                                            // Do nothing if no handle is active
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    canvasSize = size

                    // Calculate image dimensions to fit the canvas while maintaining aspect ratio
                    val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val canvasAspect = size.width / size.height

                    val imageSize: Size
                    val imageOffset: Offset

                    if (imageAspect > canvasAspect) {
                        // Image is wider than canvas (relative to their heights)
                        val height = size.width / imageAspect
                        imageSize = Size(size.width, height)
                        imageOffset = Offset(0f, (size.height - height) / 2f)
                    } else {
                        // Image is taller than canvas (relative to their widths)
                        val width = size.height * imageAspect
                        imageSize = Size(width, size.height)
                        imageOffset = Offset((size.width - width) / 2f, 0f)
                    }

                    // Store the image bounds for constraining the crop box
                    imageBounds = Rect(imageOffset, imageSize)

                    // Ensure crop box stays within image bounds (when canvas is resized)
                    if (cropBoxSize != Size.Zero && !imageBounds.isEmpty) {
                        // Adjust position if needed
//                        val newPos = Offset(
//                            x = cropBoxPosition.x.coerceIn(imageBounds.left, imageBounds.right - cropBoxSize.width),
//                            y = cropBoxPosition.y.coerceIn(imageBounds.top, imageBounds.bottom - cropBoxSize.height)
//                        )
//
//                        if (newPos != cropBoxPosition) {
//                            cropBoxPosition = newPos
//                        }
//
//                        // Adjust size if needed (if crop box extends beyond image)
//                        val newWidth = min(cropBoxSize.width, imageBounds.right - cropBoxPosition.x)
//                        val newHeight = min(cropBoxSize.height, imageBounds.bottom - cropBoxPosition.y)
//                        val newSize = Size(min(newWidth, newHeight), min(newWidth, newHeight)) // Keep square
//
//                        if (newSize != cropBoxSize) {
//                            cropBoxSize = newSize
//                        }
                        val maxAllowedWidth = imageBounds.width
                        val maxAllowedHeight = imageBounds.height

                        // Constrain the crop box size first
                        val constrainedSize = Size(
                            width = cropBoxSize.width.coerceAtMost(maxAllowedWidth),
                            height = cropBoxSize.height.coerceAtMost(maxAllowedHeight)
                        )

                        // Update size if it was constrained
                        if (constrainedSize != cropBoxSize) {
                            cropBoxSize = constrainedSize
                        }

                        // Now safely constrain the position
                        // Calculate the valid range for position
                        val maxX = imageBounds.right - cropBoxSize.width
                        val maxY = imageBounds.bottom - cropBoxSize.height

                        // Only constrain if we have a valid range
                        val newPos = if (maxX >= imageBounds.left && maxY >= imageBounds.top) {
                            Offset(
                                x = cropBoxPosition.x.coerceIn(imageBounds.left, maxX),
                                y = cropBoxPosition.y.coerceIn(imageBounds.top, maxY)
                            )
                        } else {
                            // Fallback: center the crop box if constraints are invalid
                            Offset(
                                x = imageBounds.left + (imageBounds.width - cropBoxSize.width) / 2f,
                                y = imageBounds.top + (imageBounds.height - cropBoxSize.height) / 2f
                            )
                        }

                        if (newPos != cropBoxPosition) {
                            cropBoxPosition = newPos
                        }
                    }

                    drawImage(
                        image = bitmap,
                        dstOffset = imageOffset.toIntOffset(),
                        dstSize = imageSize.toIntSize()
                    )

                    // Draw darkened overlay only outside the crop area
                    // Top rectangle (above crop box)
                    if (cropBoxPosition.y > 0) {
                        drawRect(
                            color = Color.Black.copy(alpha = 0.7f),
                            topLeft = Offset.Zero,
                            size = Size(size.width, cropBoxPosition.y)
                        )
                    }

                    // Bottom rectangle (below crop box)
                    if (cropBoxPosition.y + cropBoxSize.height < size.height) {
                        drawRect(
                            color = Color.Black.copy(alpha = 0.7f),
                            topLeft = Offset(0f, cropBoxPosition.y + cropBoxSize.height),
                            size = Size(size.width, size.height - cropBoxPosition.y - cropBoxSize.height)
                        )
                    }

                    // Left rectangle (left of crop box)
                    if (cropBoxPosition.x > 0) {
                        drawRect(
                            color = Color.Black.copy(alpha = 0.7f),
                            topLeft = Offset(0f, cropBoxPosition.y),
                            size = Size(cropBoxPosition.x, cropBoxSize.height)
                        )
                    }

                    // Right rectangle (right of crop box)
                    if (cropBoxPosition.x + cropBoxSize.width < size.width) {
                        drawRect(
                            color = Color.Black.copy(alpha = 0.7f),
                            topLeft = Offset(cropBoxPosition.x + cropBoxSize.width, cropBoxPosition.y),
                            size = Size(size.width - cropBoxPosition.x - cropBoxSize.width, cropBoxSize.height)
                        )
                    }

                    // Draw crop border
                    drawRect(
                        color = Color.White,
                        topLeft = cropBoxPosition,
                        size = cropBoxSize,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Draw corner indicators
                    drawCropCorners(cropBoxPosition, cropBoxSize)

                    // Draw grid lines
                    drawCropGrid(cropBoxPosition, cropBoxSize)
                }
            }
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button
            TextButton(
                onClick = { navController.popBackStack() }
            ) {
                Text(
                    text = "Cancel",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            // Reset button
            TextButton(
                onClick = {
                    // Reset crop box to initial state (centered square within the image)
                    val imageAspect = imageBitmap?.width?.toFloat()?.div(imageBitmap!!.height.toFloat()) ?: 1f
                    val canvasAspect = canvasSize.width / canvasSize.height

                    val imageSize: Size
                    val imageOffset: Offset

                    if (imageAspect > canvasAspect) {
                        // Image is wider than canvas
                        val height = canvasSize.width / imageAspect
                        imageSize = Size(canvasSize.width, height)
                        imageOffset = Offset(0f, (canvasSize.height - height) / 2f)
                    } else {
                        // Image is taller than canvas
                        val width = canvasSize.height * imageAspect
                        imageSize = Size(width, canvasSize.height)
                        imageOffset = Offset((canvasSize.width - width) / 2f, 0f)
                    }

                    // Calculate crop box size (80% of the smaller image dimension)
                    val size = minOf(imageSize.width, imageSize.height) * 0.8f
                    cropBoxSize = Size(size, size)

                    // Center the crop box within the image
                    cropBoxPosition = Offset(
                        imageOffset.x + (imageSize.width - size) / 2f,
                        imageOffset.y + (imageSize.height - size) / 2f
                    )
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = "Reset",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Done button with API upload
            TextButton(
                onClick = {
                    scope.launch {

                        try {
                            // Process and save the cropped image
                            imageBitmap?.let { bitmap ->
                                val croppedBitmap = cropImage(
                                    bitmap = bitmap.asAndroidBitmap(),
                                    cropRect = Rect(
                                        offset = cropBoxPosition,
                                        size = cropBoxSize
                                    ),
                                    canvasSize = canvasSize
                                )

                                // Save the cropped image temporarily
                                withContext(Dispatchers.IO) {
                                    val file = File(context.cacheDir, "cropped_profile_${System.currentTimeMillis()}.jpg")

                                    FileOutputStream(file).use { out ->
                                        // Compress with quality 85 to reduce file size
                                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                    }

                                    Log.d("EDIT PROFILE", "File created: ${file.name}, Size: ${file.length()} bytes")

                                    // Ensure file exists and is readable
                                    if (file.exists() && file.canRead()) {
                                        withContext(Dispatchers.Main) {
                                            viewModel.uploadProfilePhoto(file) {
                                                // On success callback
                                                navController.popBackStack()
                                            }
                                        }
                                    } else {
                                        Log.e("EDIT PROFILE", "File not accessible")
//                                        navController.popBackStack()
//                                        isProcessing = false
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = "An error occurred: ${e.message}"
                            e.printStackTrace()
                        } finally {

                        }
                    }
                },
                enabled = !isProcessing.value
            ) {
                if (isProcessing.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Done",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
    uploadError?.let { error ->
        AlertDialog(
            onDismissRequest = { /* Handle dismiss */ },
            title = { Text("Upload Failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { /* Handle retry */ }) {
                    Text("OK")
                }
            }
        )
    }
}

// Modified function to save as File instead of Uri
//private suspend fun saveCroppedImageAsFile(context: android.content.Context, bitmap: Bitmap): File =
//    withContext(Dispatchers.IO) {
//        val file = File(context.cacheDir, "cropped_profile_${System.currentTimeMillis()}.jpg")
//        FileOutputStream(file).use { out ->
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
//        }
//        file
//    }

private fun DrawScope.drawCropCorners(cropOffset: Offset, cropSize: Size) {
    val cornerLength = 20.dp.toPx()
    val cornerWidth = 3.dp.toPx()

    // Top-left corner
    drawLine(
        color = Color.White,
        start = cropOffset,
        end = cropOffset.copy(x = cropOffset.x + cornerLength),
        strokeWidth = cornerWidth
    )
    drawLine(
        color = Color.White,
        start = cropOffset,
        end = cropOffset.copy(y = cropOffset.y + cornerLength),
        strokeWidth = cornerWidth
    )

    // Top-right corner
    val topRight = cropOffset.copy(x = cropOffset.x + cropSize.width)
    drawLine(
        color = Color.White,
        start = topRight,
        end = topRight.copy(x = topRight.x - cornerLength),
        strokeWidth = cornerWidth
    )
    drawLine(
        color = Color.White,
        start = topRight,
        end = topRight.copy(y = topRight.y + cornerLength),
        strokeWidth = cornerWidth
    )

    // Bottom-left corner
    val bottomLeft = cropOffset.copy(y = cropOffset.y + cropSize.height)
    drawLine(
        color = Color.White,
        start = bottomLeft,
        end = bottomLeft.copy(x = bottomLeft.x + cornerLength),
        strokeWidth = cornerWidth
    )
    drawLine(
        color = Color.White,
        start = bottomLeft,
        end = bottomLeft.copy(y = bottomLeft.y - cornerLength),
        strokeWidth = cornerWidth
    )

    // Bottom-right corner
    val bottomRight = cropOffset.copy(
        x = cropOffset.x + cropSize.width,
        y = cropOffset.y + cropSize.height
    )
    drawLine(
        color = Color.White,
        start = bottomRight,
        end = bottomRight.copy(x = bottomRight.x - cornerLength),
        strokeWidth = cornerWidth
    )
    drawLine(
        color = Color.White,
        start = bottomRight,
        end = bottomRight.copy(y = bottomRight.y - cornerLength),
        strokeWidth = cornerWidth
    )
}

private fun DrawScope.drawCropGrid(cropOffset: Offset, cropSize: Size) {
    val gridWidth = 1.dp.toPx()

    // Vertical lines
    for (i in 1..2) {
        val x = cropOffset.x + (cropSize.width / 3f) * i
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(x, cropOffset.y),
            end = Offset(x, cropOffset.y + cropSize.height),
            strokeWidth = gridWidth
        )
    }

    // Horizontal lines
    for (i in 1..2) {
        val y = cropOffset.y + (cropSize.height / 3f) * i
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(cropOffset.x, y),
            end = Offset(cropOffset.x + cropSize.width, y),
            strokeWidth = gridWidth
        )
    }
}

private suspend fun fixImageRotation(context: android.content.Context, bitmap: Bitmap, uri: Uri): Bitmap =
    withContext(Dispatchers.IO) {
        try {
            // Get the input stream from the URI
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)

            // Try to read EXIF data (will only work for JPEG images)
            val exifInterface = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    inputStream?.let { ExifInterface(it) }
                } else {
                    // For older Android versions, we need a file path
                    val filePath = uri.path
                    if (filePath != null) {
                        ExifInterface(filePath)
                    } else null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                // Close the input stream
                inputStream?.close()
            }

            // Get orientation from EXIF data
            val orientation = exifInterface?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            // Create a matrix to apply the rotation
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.preScale(-1f, 1f)
                }
                else -> return@withContext bitmap // No rotation needed
            }

            // Apply the rotation to the bitmap
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            // If we created a new bitmap, recycle the old one to free memory
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }

            rotatedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap // Return original if anything goes wrong
        }
    }

// Helper extension functions for coordinate conversion
private fun Offset.getDistance(): Float {
    return sqrt(x * x + y * y)
}

private fun Offset.toIntOffset(): IntOffset = IntOffset(x.roundToInt(), y.roundToInt())
private fun Size.toIntSize(): IntSize = IntSize(width.roundToInt(), height.roundToInt())

private suspend fun cropImage(
    bitmap: Bitmap,
    cropRect: Rect,
    canvasSize: Size
): Bitmap = withContext(Dispatchers.Default) {
    // Calculate the ratio between original image dimensions and how it's displayed on canvas
    val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    val canvasAspect = canvasSize.width / canvasSize.height

    val imageSize: Size
    val imageOffset: Offset

    if (imageAspect > canvasAspect) {
        // Image is wider than canvas (relative to their heights)
        val height = canvasSize.width / imageAspect
        imageSize = Size(canvasSize.width, height)
        imageOffset = Offset(0f, (canvasSize.height - height) / 2f)
    } else {
        // Image is taller than canvas (relative to their widths)
        val width = canvasSize.height * imageAspect
        imageSize = Size(width, canvasSize.height)
        imageOffset = Offset((canvasSize.width - width) / 2f, 0f)
    }

    // Calculate actual crop coordinates in the original image
    val scaleX = bitmap.width / imageSize.width
    val scaleY = bitmap.height / imageSize.height

    val cropX = ((cropRect.left - imageOffset.x) * scaleX).coerceIn(0f, bitmap.width.toFloat())
    val cropY = ((cropRect.top - imageOffset.y) * scaleY).coerceIn(0f, bitmap.height.toFloat())
    val cropWidth = (cropRect.width * scaleX).coerceAtMost(bitmap.width - cropX)
    val cropHeight = (cropRect.height * scaleY).coerceAtMost(bitmap.height - cropY)

    // Create the cropped bitmap
    Bitmap.createBitmap(
        bitmap,
        cropX.toInt(),
        cropY.toInt(),
        cropWidth.toInt(),
        cropHeight.toInt()
    )
}

private suspend fun saveCroppedImage(context: android.content.Context, bitmap: Bitmap): Uri =
    withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "cropped_profile_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file)
    }

private fun constrainCropBox(
    position: Offset,
    size: Size,
    bounds: Rect,
    minSize: Float
): Pair<Offset, Size> {
    // First ensure size doesn't exceed bounds
    val maxWidth = bounds.width
    val maxHeight = bounds.height
    val constrainedSize = Size(
        width = size.width.coerceIn(minSize, maxWidth),
        height = size.height.coerceIn(minSize, maxHeight)
    )

    // Then constrain position based on the constrained size
    val maxX = bounds.right - constrainedSize.width
    val maxY = bounds.bottom - constrainedSize.height

    val constrainedPosition = if (maxX >= bounds.left && maxY >= bounds.top) {
        Offset(
            x = position.x.coerceIn(bounds.left, maxX),
            y = position.y.coerceIn(bounds.top, maxY)
        )
    } else {
        // Center if bounds are too small
        Offset(
            x = bounds.left + (bounds.width - constrainedSize.width) / 2f,
            y = bounds.top + (bounds.height - constrainedSize.height) / 2f
        )
    }

    return constrainedPosition to constrainedSize
}