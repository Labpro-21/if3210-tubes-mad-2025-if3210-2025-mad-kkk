package com.example.purrytify.ui.component

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

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

    val shareUrl = "purrytify://song/${song.serverId}"
    val context = LocalContext.current

    LaunchedEffect(song.serverId) {
        if (song.serverId != null) {
            try {
                qrCodeBitmap = generateQRCode(shareUrl)
            } catch (e: Exception) {
                Toast.makeText(context, "Error generating QR code", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
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
                    Spacer(modifier = Modifier.width(48.dp))

                    Text(
                        text = "Share Song",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Poppins
                    )

                    IconButton(onClick = onDismiss) {
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
                                modifier = Modifier.fillMaxSize(),
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
    try {
        val writer = QRCodeWriter()
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.MARGIN, 0)
        }

        val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 300, 300, hints)
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
        bitmap
    } catch (e: WriterException) {
        null
    }
}