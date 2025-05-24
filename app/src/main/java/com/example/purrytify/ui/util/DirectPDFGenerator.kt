package com.example.purrytify.ui.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.purrytify.R
import androidx.core.content.res.ResourcesCompat
import com.example.purrytify.service.Profile
import com.example.purrytify.ui.model.ListeningStreak
import com.example.purrytify.ui.model.MonthlySoundCapsule
import com.example.purrytify.ui.model.SongStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class DirectPDFGenerator(private val context: Context) {

    suspend fun generateProfilePDF(
        userState: Profile?, // Replace with your UserState type
        songStats: SongStats?, // Replace with your SongStats type
        monthlyCapsules: List<MonthlySoundCapsule>, // Replace with MonthlySoundCapsule
        streaks: List<ListeningStreak?>, // Replace with ListeningStreak?
        fileName: String = "sound_capsule_${System.currentTimeMillis()}.pdf"
    ): File? = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            // Create paints for different text styles
            val titlePaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 24f
                typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 18f
                typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 14f
                isAntiAlias = true
                typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
            }

            val grayPaint = Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 12f
                isAntiAlias = true
                typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
            }

            val accentPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#4CAF50")
                textSize = 16f
                typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
                isAntiAlias = true
            }

            val artistPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#4A90E2")
                textSize = 16f
                typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
                isAntiAlias = true
            }

            val songPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#FFEB3B")
                textSize = 16f
                typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold)
                isAntiAlias = true
            }

            val cardPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#1E1E1E")
                isAntiAlias = true
            }

            var currentPage = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Set dark background
            canvas.drawColor(android.graphics.Color.parseColor("#121212"))

            var yPosition = 60f
            val margin = 40f
            val cardMargin = 20f

            drawDrawableInTopRight(canvas, pageWidth, margin)

            // Title
            canvas.drawText("Your Sound Capsule", margin, yPosition, titlePaint)
            yPosition += 40f

            // Date
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            canvas.drawText("Generated on ${dateFormat.format(Date())}", margin, yPosition, grayPaint)
            yPosition += 50f

            // Profile Header Section
            canvas.drawText("Profile Overview", margin, yPosition, headerPaint)
            yPosition += 30f

            // Profile info (you'll need to extract actual data from userState)
            canvas.drawText("Username: " + (userState?.username ?: "No username"), margin + 20f, yPosition, bodyPaint)
            yPosition += 25f
            canvas.drawText("Location: " + (userState?.location ?: "No location"), margin + 20f, yPosition, bodyPaint)
            yPosition += 40f

            // Music Statistics
            canvas.drawText("Music Statistics", margin, yPosition, headerPaint)
            yPosition += 30f

            // Draw stats cards (you'll need to extract actual data from songStats)
            val statsY = yPosition
            val cardWidth = (pageWidth - 3 * margin) / 3f
            val cardHeight = 60f

            // Total Songs
            val rect1 = RectF(margin, statsY, margin + cardWidth, statsY + cardHeight)
            canvas.drawRoundRect(rect1, 8f, 8f, cardPaint)
            canvas.drawText("SONGS", margin + 10f, statsY + 20f, grayPaint)
            canvas.drawText(songStats?.totalSongs.toString(), margin + 10f, statsY + 45f, bodyPaint) // Extract from songStats

            // Liked Songs  
            val rect2 = RectF(margin + cardWidth + 10f, statsY, margin + 2 * cardWidth + 10f, statsY + cardHeight)
            canvas.drawRoundRect(rect2, 8f, 8f, cardPaint)
            canvas.drawText("LIKED", margin + cardWidth + 20f, statsY + 20f, grayPaint)
            canvas.drawText(songStats?.likedSongs.toString(), margin + cardWidth + 20f, statsY + 45f, bodyPaint) // Extract from songStats

            // Listened Songs
            val rect3 = RectF(margin + 2 * cardWidth + 20f, statsY, pageWidth - margin, statsY + cardHeight)
            canvas.drawRoundRect(rect3, 8f, 8f, cardPaint)
            canvas.drawText("LISTENED", margin + 2 * cardWidth + 30f, statsY + 20f, grayPaint)
            canvas.drawText(songStats?.listenedSongs.toString(), margin + 2 * cardWidth + 30f, statsY + 45f, bodyPaint) // Extract from songStats

            yPosition = statsY + cardHeight + 40f

            // Monthly Capsules
            monthlyCapsules.forEach { capsule ->
                // Check if we need a new page
                if (yPosition + 225f > pageHeight - 60f) {
                    pdfDocument.finishPage(page)
                    currentPage++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    canvas.drawColor(android.graphics.Color.parseColor("#121212"))
                    yPosition = 60f
                }

                drawMonthlyCapsule(canvas, capsule, margin, yPosition, pageWidth - 2 * margin, cardPaint, headerPaint, grayPaint, accentPaint, artistPaint, songPaint)
                yPosition += 205f
            }

            // Listening Streaks
            streaks.filterNotNull().forEach { streak ->
                // Check if we need a new page
                if (yPosition + 130f > pageHeight - 60f) {
                    pdfDocument.finishPage(page)
                    currentPage++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    canvas.drawColor(android.graphics.Color.parseColor("#121212"))
                    yPosition = 60f
                }

                drawListeningStreak(canvas, streak, margin, yPosition, pageWidth - 2 * margin, cardPaint, headerPaint, bodyPaint, grayPaint)
                yPosition += 120f
            }

            pdfDocument.finishPage(page)

            // Save to file
            val documentsDir = File(context.getExternalFilesDir(null), "Documents")
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            val file = File(documentsDir, fileName)
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

    private fun drawMonthlyCapsule(
        canvas: Canvas,
        capsule: MonthlySoundCapsule, // Replace with MonthlySoundCapsule
        x: Float,
        y: Float,
        width: Float,
        cardPaint: Paint,
        headerPaint: Paint,
        grayPaint: Paint,
        accentPaint: Paint,
        artistPaint: Paint,
        songPaint: Paint
    ) {
        val cardHeight = 185f
        val cardRect = RectF(x, y, x + width, y + cardHeight)
        canvas.drawRoundRect(cardRect, 12f, 12f, cardPaint)

        // Month title (extract from capsule)
        canvas.drawText("Monthly Capsule - ${capsule.month}" , x + 16f, y + 25f, headerPaint)

        // Time listened section
        val timeRect = RectF(x + 16f, y + 40f, x + width - 16f, y + 80f)
        canvas.drawRoundRect(timeRect, 8f, 8f, Paint().apply {
            color = android.graphics.Color.parseColor("#2E2E2E")
        })

        val artistRect = RectF(x + 16f, y + 85f, x + width - 16f, y + 125f)
        canvas.drawRoundRect(artistRect, 8f, 8f, Paint().apply {
            color = android.graphics.Color.parseColor("#2E2E2E")
        })

        val songRect = RectF(x + 16f, y + 130f, x + width - 16f, y + 170f)
        canvas.drawRoundRect(songRect, 8f, 8f, Paint().apply {
            color = android.graphics.Color.parseColor("#2E2E2E")
        })

        canvas.drawText("Time listened", x + 24f, y + 55f, grayPaint)
        canvas.drawText("${capsule.totalListeningMinutes} minutes", x + 24f, y + 72f, accentPaint)

        // Top Artist and Song
        canvas.drawText("Top Artist", x + 24f, y + 100f, grayPaint)
        canvas.drawText(capsule.topArtist?.name.toString(), x + 24f, y + 117f, artistPaint)

        canvas.drawText("Top Song", x + 24f, y + 145f, grayPaint)
        canvas.drawText(capsule.topSong?.title.toString(), x + 24f, y + 162f, songPaint)

        // Note: You'll need to replace the bracketed text with actual data extraction
        // Example: capsule.month, capsule.totalListeningMinutes, etc.
    }

    private fun drawListeningStreak(
        canvas: Canvas,
        streak: ListeningStreak, // Replace with ListeningStreak
        x: Float,
        y: Float,
        width: Float,
        cardPaint: Paint,
        headerPaint: Paint,
        bodyPaint: Paint,
        grayPaint: Paint
    ) {
        val cardHeight = 100f
        val cardRect = RectF(x, y, x + width, y + cardHeight)
        canvas.drawRoundRect(cardRect, 12f, 12f, cardPaint)

        // Streak title (extract from streak)
        canvas.drawText("You had a ${streak.dayCount}-day streak", x + 16f, y + 25f, headerPaint)

        // Song details (extract from streak)
        canvas.drawText("${streak.trackDetails?.title} by ${streak.trackDetails?.artist}", x + 16f, y + 50f, bodyPaint)
//        canvas.drawText("day after day. You were on fire!", x + 16f, y + 70f, bodyPaint)

        // Date range (extract from streak)
        val startDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(streak.startDate))
        canvas.drawText("$startDate", x + 16f, y + 75f, grayPaint)

        // Note: You'll need to replace the bracketed text with actual data extraction
        // Example: streak.dayCount, streak.trackDetails.title, etc.
    }

    private fun drawDrawableInTopRight(
        canvas: Canvas,
        pageWidth: Int,
        margin: Float
    ) {
        try {
            // Get drawable from resources - replace R.drawable.your_logo with your actual drawable
            val drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.logo_3, null)

            drawable?.let { d ->
                // Define logo size
                val logoSize = 120 // Adjust size as needed

                // Calculate position for top right
                val logoX = pageWidth - margin - logoSize
                val logoY = margin

                // Set bounds for the drawable
                d.setBounds(
                    logoX.toInt(),
                    logoY.toInt(),
                    (logoX + logoSize).toInt(),
                    (logoY + logoSize).toInt()
                )

                // Draw the drawable
                d.draw(canvas)
            }
        } catch (e: Exception) {
            e.printStackTrace()

        }
    }
}

// Usage example with proper data extraction
//suspend fun exportProfileToPDFDirect(
//    context: Context,
//    userState: Any?, // Your actual UserState type
//    songStats: Any?, // Your actual SongStats type
//    monthlyCapsules: List<Any>, // Your actual MonthlySoundCapsule type
//    streaks: List<Any?>, // Your actual ListeningStreak type
//    onSuccess: (File) -> Unit,
//    onError: (String) -> Unit
//) {
//    try {
//        val pdfGenerator = DirectPDFGenerator(context)
//        val file = pdfGenerator.generateProfilePDF(
//            userState = userState,
//            songStats = songStats,
//            monthlyCapsules = monthlyCapsules,
//            streaks = streaks
//        )
//
//        if (file != null) {
//            onSuccess(file)
//        } else {
//            onError("Failed to generate PDF")
//        }
//    } catch (e: Exception) {
//        onError(e.message ?: "Unknown error")
//    }
//}