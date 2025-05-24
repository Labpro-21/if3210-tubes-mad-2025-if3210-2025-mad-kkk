package com.example.purrytify.ui.component

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.purrytify.ui.model.LoadImage
import com.example.purrytify.data.model.Song
import com.example.purrytify.ui.theme.Poppins
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.widget.Toast

private const val TAG = "QRShareSheet" // logcat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRShareSheet(
    song: Song,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier
) {
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val fileProviderAuthority = "${LocalContext.current.packageName}.provider"

    val shareUrl = "purrytify://song/${song.serverId}"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(song.serverId) {
        if (song.serverId != null) {
            try {
                qrCodeBitmap = generateQRCode(shareUrl)
                Log.d(TAG, "QR code generation successful.")
            } catch (e: Exception) {
                Toast.makeText(context, "Error generating QR code", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
            Log.w(TAG, "song.serverId null, QR code cannot be generated.")
        }
    }

    val shareQrImage: (Bitmap?) -> Unit = { bitmap ->
        if (bitmap != null) {
            coroutineScope.launch {
                try {
                    val cachePath = File(context.cacheDir, "images")
                    cachePath.mkdirs()

                    val fileName = "qr_code_${song.serverId}.png"
                    val file = File(cachePath, fileName)
                    Log.d(TAG, "Attempting to save QR image to file: ${file.absolutePath}")

                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    Log.d(TAG, "QR image saved successfully.")

                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        fileProviderAuthority,
                        file
                    )
                    Log.d(TAG, "FileProvider URI generated: $uri")

                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "image/png"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    Log.d(TAG, "Share Intent created.")
                    context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
                    Log.d(TAG, "Share chooser launched.")
                } catch (e: Exception) {
                    Toast.makeText(context, "Error sharing QR code", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.w(TAG, "Attempted to share QR code, but bitmap was null.")
            Toast.makeText(context, "QR Code not ready for sharing", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(Color(0xFF1C1C1E))
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isShareButtonEnabled = qrCodeBitmap != null && !isLoading && song.serverId != null

                    IconButton(
                        onClick = {
                            Log.d(TAG, "Share button clicked.")
                            shareQrImage(qrCodeBitmap)
                        },
                        enabled = isShareButtonEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share QR Image",
                            tint = if (isShareButtonEnabled) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    }

                    Text(
                        text = "Share Song",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Poppins
                    )

                    IconButton(onClick = {
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        LoadImage(
                            imagePath = song.imagePath,
                            contentDescription = "${song.title} Album Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = Poppins,
                            maxLines = 1
                        )
                        Text(
                            text = song.artist,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontFamily = Poppins,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (song.serverId == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "QR Code not available",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontFamily = Poppins,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "This song is not available for sharing",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontFamily = Poppins,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (isLoading) {
                    Log.d(TAG, "Loading for QR code.")
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(
                                Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (qrCodeBitmap != null) {
                    Log.d(TAG, "Display QR code.")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrCodeBitmap!!.asImageBitmap(),
                                contentDescription = "QR Code for ${song.title}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Scan to listen on Purrytify",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontFamily = Poppins,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Failed to generate QR code",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontFamily = Poppins,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private suspend fun generateQRCode(text: String): Bitmap? = withContext(Dispatchers.IO) {
    Log.d(TAG, "generateQRCode generating: $text")
    try {
        val writer = QRCodeWriter()
        val qrCodeSize = 400
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.MARGIN, 1)
        }

        val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
                )
            }
        }
        Log.d(TAG, "QR code bitmap created. Dims: ${width}x${height}")
        bitmap
    } catch (e: WriterException) {
        Log.e(TAG, "WriterException during QR code generation: ${e.message}", e)
        null
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected error during QR code generation: ${e.message}", e)
        null
    }
}