package com.example.purrytify.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.graphics.Typeface
import androidx.compose.ui.unit.Density

class PDFGenerator(private val context: Context) {

    suspend fun generatePDFFromComposables(
        composables: List<@Composable () -> Unit>,
        fileName: String = "profile_report.pdf",
        pageWidth: Int = 595, // A4 width in points
        pageHeight: Int = 842  // A4 height in points
    ): File? = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint().apply {
                isAntiAlias = true
            }

            var currentPage = 1
            var currentYPosition = 50f
            val marginTop = 50f
            val marginBottom = 50f
            val marginLeft = 50f
            val marginRight = 50f
            val availableHeight = pageHeight - marginTop - marginBottom

            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Set background color
            canvas.drawColor(Color(0xFF121212).toArgb())

            for (composable in composables) {
                val bitmap = captureComposable(composable)
                bitmap?.let { bmp ->
                    val scaledBitmap = scaleBitmapToFitWidth(bmp, (pageWidth - marginLeft - marginRight).toInt())

                    // Check if we need a new page
                    if (currentYPosition + scaledBitmap.height > availableHeight) {
                        // Finish current page and start new one
                        pdfDocument.finishPage(page)
                        currentPage++
                        currentYPosition = marginTop

                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        canvas.drawColor(Color(0xFF121212).toArgb())
                    }

                    // Draw the bitmap
                    canvas.drawBitmap(scaledBitmap, marginLeft, currentYPosition, paint)
                    currentYPosition += scaledBitmap.height + 20f // Add some spacing

                    bmp.recycle()
                    scaledBitmap.recycle()
                }
            }

            // Finish the last page
            pdfDocument.finishPage(page)

            // Save to file
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            val fileOutputStream = FileOutputStream(file)
            pdfDocument.writeTo(fileOutputStream)

            fileOutputStream.close()
            pdfDocument.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun captureComposable(
        composable: @Composable () -> Unit
    ): Bitmap? = withContext(Dispatchers.Main) {
        try {
            val composeView = ComposeView(context).apply {
                setContent {
                    Box(
                        modifier = Modifier.size(400.dp, 300.dp) // Adjust size as needed
                    ) {
                        composable()
                    }
                }
            }

            // Measure and layout the view
            val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
                (400 * context.resources.displayMetrics.density).toInt(),
                android.view.View.MeasureSpec.EXACTLY
            )
            val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
                (300 * context.resources.displayMetrics.density).toInt(),
                android.view.View.MeasureSpec.AT_MOST
            )

            composeView.measure(widthSpec, heightSpec)
            composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

            // Capture as bitmap
            composeView.drawToBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun scaleBitmapToFitWidth(bitmap: Bitmap, targetWidth: Int): Bitmap {
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val targetHeight = (targetWidth * aspectRatio).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
