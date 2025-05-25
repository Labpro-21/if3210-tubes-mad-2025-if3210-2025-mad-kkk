package com.example.purrytify.ui.screen

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.purrytify.ui.model.DailyChartViewModel
import com.example.purrytify.ui.model.GlobalViewModel
import com.example.purrytify.ui.model.MonthDataValue
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// Data class for individual bar data
data class DailyBarData(
    val day: String,           // e.g., "Mon", "Tue", "Wed" (not used anymore but kept for compatibility)
    val date: String,          // e.g., "May 11", "May 12" - this will be displayed
    val value: Float,          // The actual value (e.g., screen time in minutes)
    val displayValue: String,  // Formatted display value (e.g., "3h 40m")
    val color: Color = Color(0xFF4A90E2)
)

// Data class for Y-axis configuration
data class YAxisConfig(
    val intervals: List<Float>,     // e.g., [60f, 120f, 180f] for 1h, 2h, 3h
    val labels: List<String>        // e.g., ["1h", "2h", "3h"]
)

// Data class for the entire month's data
data class MonthlyBarData(
    val title: String,                    // e.g., "Daily average screen time"
    val subtitle: String,                 // e.g., "May 11 - May 17 (Week 20)"
    val averageValue: String,             // e.g., "3 h 40 m"
    val averageValueActual: Float,        // e.g., 220f (actual average value for red line positioning)
    val dailyData: List<DailyBarData>,    // List of daily data (1-30 items)
    val yAxisConfig: YAxisConfig          // Y-axis configuration
)

@Composable
fun DailyChartScreen(
    globalViewModel: GlobalViewModel,
    navController: NavController,
    month: Int,
    year: Int,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val secondaryColor = Color(0xFF01667A)
    val viewModel: DailyChartViewModel = viewModel(
        factory = DailyChartViewModel.DailyChartViewModelFactory(
            context.applicationContext as Application,
            globalViewModel,
            month,
            year)
    )

    val density = LocalDensity.current
    val yAxisWidth = 30.dp
    val barWidth = 40.dp
    val barSpacing = 12.dp
    val textHeight = 20f // Reserved space for text labels

    val monthData by viewModel.monthData.collectAsState()
    val currentMonth = YearMonth.of(year, month)
    val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val year = currentMonth.year
    val nowMonth = YearMonth.now()

    val monthlyData = remember(monthData) {
        generateMonthlyBarData(monthData, "Daily average screen time")
    }

    val sumMinutes = remember(monthData) {
        monthData.sumOf { it.value }.toInt().div(60)
    }

    val dailyAverage = remember(monthData) {
        monthData.map {it.value}.average().toInt().div(60)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header section
        TopAppBar(title = "Daily chart", onBackClicked = { navController.popBackStack() })

        MonthHeader(month = "$monthName $year")

        val suffixTitle = when {
            (nowMonth.month == currentMonth.month
                    && nowMonth.year == currentMonth.year) -> " this month."
            else -> " last $monthName."
        }
        val text = buildAnnotatedString {
            append("You listened to music for ")
            pushStyle(SpanStyle(color = Color(0xFF4A90E2)))
            append("$sumMinutes minutes ")
            pop()
            append(suffixTitle)
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Daily average: $dailyAverage minutes",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Thin,
        )

        Spacer(modifier = Modifier.height(20.dp))

//        Text(
//            text = monthlyData.subtitle,
//            fontSize = 16.sp,
//            color = Color.Gray,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Text(
//            text = monthlyData.averageValue,
//            fontSize = 36.sp,
//            fontWeight = FontWeight.Bold,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Text(
//            text = monthlyData.title,
//            fontSize = 16.sp,
//            color = Color.Gray,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(32.dp))

        // Chart section with fixed Y-axis and scrollable bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // Fixed Y-axis on the left
            YAxisComponent(
                yAxisConfig = monthlyData.yAxisConfig,
                textHeight = textHeight,
                modifier = Modifier
                    .width(yAxisWidth)
                    .fillMaxHeight()
            )

            // Scrollable bar chart with text labels
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Background with grid lines and average line
                ChartBackground(
                    yAxisConfig = monthlyData.yAxisConfig,
                    averageValue = monthlyData.averageValueActual,
                    textHeight = textHeight,
                    modifier = Modifier.fillMaxSize()
                )

                // Bars and text labels on top of background
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(barSpacing)
                ) {
                    items(monthlyData.dailyData) { dayData ->
                        SingleBar(
                            dayData = dayData,
                            maxValue = monthlyData.yAxisConfig.intervals.maxOrNull() ?: 360f,
                            barWidth = barWidth,
                            textHeight = textHeight,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }

                // Average line on top of everything
                AverageLineOverlay(
                    averageValue = monthlyData.averageValueActual,
                    maxValue = monthlyData.yAxisConfig.intervals.maxOrNull() ?: 360f,
                    textHeight = textHeight,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

    }
}

@Composable
fun YAxisComponent(
    yAxisConfig: YAxisConfig,
    textHeight: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val chartHeight = size.height - textHeight // Reserve space for text labels
        val maxValue = yAxisConfig.intervals.maxOrNull() ?: 360f

        // Draw Y-axis line
        drawLine(
            color = Color.White,
            start = Offset(size.width - 1f, 0f),
            end = Offset(size.width - 1f, chartHeight),
            strokeWidth = 2f
        )

        // Draw horizontal grid lines and labels
        yAxisConfig.intervals.forEachIndexed { index, interval ->
            val y = chartHeight - (interval / maxValue) * chartHeight

            // Draw horizontal grid line
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(size.width - 10f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )

            // Draw Y-axis label
            drawContext.canvas.nativeCanvas.drawText(
                yAxisConfig.labels[index],
                size.width - 15f,
                y + 5f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 12.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }
    }
}

@Composable
fun ChartBackground(
    yAxisConfig: YAxisConfig,
    averageValue: Float,
    textHeight: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val chartHeight = size.height - textHeight // Reserve space for text labels
        val maxValue = yAxisConfig.intervals.maxOrNull() ?: 360f

        // Draw horizontal grid lines (background)
        yAxisConfig.intervals.forEach { interval ->
            val gridY = chartHeight - (interval / maxValue) * chartHeight
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(0f, gridY),
                end = Offset(size.width, gridY),
                strokeWidth = 1f
            )
        }

        // Draw continuous average line (red dashed line)
//        val averageY = chartHeight - (averageValue / maxValue) * chartHeight
//        drawLine(
//            color = Color.Red.copy(alpha = 0.7f),
//            start = Offset(0f, averageY),
//            end = Offset(size.width, averageY),
//            strokeWidth = 2f,
//            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
//        )
    }
}

@Composable
fun SingleBar(
    dayData: DailyBarData,
    maxValue: Float,
    barWidth: androidx.compose.ui.unit.Dp,
    textHeight: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.width(barWidth)
    ) {
        val chartHeight = size.height - textHeight // Reserve space for text labels
        val barHeight = (dayData.value / maxValue) * chartHeight
        val y = chartHeight - barHeight

        val barWidthPixels = size.width * 0.8f
        val barLeft = (size.width - barWidthPixels) / 2f
        val circleRadius = barWidthPixels / 2f

        if (barHeight > circleRadius) {
            // Draw rectangular bottom part (no rounded corners)
            drawRect(
                color = dayData.color,
                topLeft = Offset(barLeft, y + circleRadius),
                size = Size(barWidthPixels, barHeight - circleRadius)
            )

            // Draw circular top part
            drawCircle(
                color = dayData.color,
                radius = circleRadius,
                center = Offset(barLeft + circleRadius, y + circleRadius)
            )
        } else {
            // If bar is very short, just draw a partial circle
            drawCircle(
                color = dayData.color,
                radius = circleRadius,
                center = Offset(barLeft + circleRadius, chartHeight - circleRadius)
            )
        }

        // Draw the date text below the bar (within the reserved space)
        drawContext.canvas.nativeCanvas.drawText(
            dayData.date,
            size.width / 2f, // Center the text horizontally
            chartHeight + textHeight + 20f, // Position text in the reserved space
            android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 12.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER // Center align the text
            }
        )
    }
}

@Composable
fun AverageLineOverlay(
    averageValue: Float,
    maxValue: Float,
    textHeight: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val chartHeight = size.height - textHeight // Reserve space for text labels

        // Draw continuous average line (red dashed line) on top
        val averageY = chartHeight - (averageValue / maxValue) * chartHeight
        drawLine(
            color = Color.Red.copy(alpha = 0.8f),
            start = Offset(0f, averageY),
            end = Offset(size.width, averageY),
            strokeWidth = 3f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        )
    }
}

@Composable
private fun TopAppBar(title: String, onBackClicked: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
//            .background(Color.Black)
    ) {
        IconButton(onClick = onBackClicked) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MonthHeader(month: String) {
    Text(
        text = month,
        color = Color(0xFF999999),
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 16.dp)
    )
}

fun generateMonthlyBarData(monthData: List<MonthDataValue>, title: String): MonthlyBarData {
    // Skip if data is empty
    if (monthData.isEmpty()) {
        return MonthlyBarData(
            title = title,
            subtitle = "No data available",
            averageValue = "0 h 0 m",
            averageValueActual = 0f,
            dailyData = emptyList(),
            yAxisConfig = YAxisConfig(
                intervals = listOf(60f, 120f, 180f),
                labels = listOf("6h", "7h", "8h")
            )
        )
    }

    // Calculate average
    val average = monthData.map { it.value }.average().toFloat()

    // Format the average display value
    val averageHours = (average / 60).toInt()
    val averageMinutes = (average % 60).toInt()
    val averageValueFormatted = "$averageHours h $averageMinutes m"

    // Generate daily bar data
    val dailyData = monthData.map { dataValue ->
        val hours = dataValue.value / 60
        val minutes = dataValue.value % 60
        val displayValue = "${hours}h ${minutes}m"

        // Parse date from the day string (assuming format like "2023-05-11")
        val dateComponents = dataValue.day.split("-")
        val month = when (dateComponents[1]) {
            "01" -> "Jan"
            "02" -> "Feb"
            "03" -> "Mar"
            "04" -> "Apr"
            "05" -> "May"
            "06" -> "Jun"
            "07" -> "Jul"
            "08" -> "Aug"
            "09" -> "Sep"
            "10" -> "Oct"
            "11" -> "Nov"
            "12" -> "Dec"
            else -> "???"
        }
        val formattedDate = "$month ${dateComponents[2].toInt()}"

        DailyBarData(
            day = "", // Not used anymore but kept for compatibility
            date = formattedDate,
            value = dataValue.value.toFloat(),
            displayValue = displayValue,
            color = Color(0xFF4A90E2) // Default color, you can customize based on values if needed
        )
    }

    // Generate Y-axis config based on the maximum value
    val maxValue = monthData.maxOfOrNull { it.value }?.toFloat() ?: 180f
//    val roundedMax = (maxValue / 60).toInt() * 60f + 60f // Round up to the next hour

    val intervals = mutableListOf<Float>()
    val labels = mutableListOf<String>()

    // Create intervals every hour up to the rounded max
    var interval = (maxValue / 4).toInt()
    var cur = 0
    while (cur <= maxValue) {
        intervals.add(cur.toFloat())
        val suffix = when {
            (3600 <= cur) -> 'h'
            (61 <= cur) -> 'm'
            else -> 's'
        }
        val prefix = when {
            (3600 <= cur) -> (cur / 3600).toInt()
            (61 <= cur) -> (cur / 60).toInt()
            else -> cur
        }
        labels.add("$prefix$suffix")
        cur += interval
    }

    if (intervals.last() < maxValue) {
        intervals.removeLast()
        intervals.add(maxValue)
        val suffix = when {
            (3600 <= cur) -> 'h'
            (61 <= cur) -> 'm'
            else -> 's'
        }
        val prefix = when {
            (3600 <= cur) -> (cur / 3600).toInt()
            (61 <= cur) -> (cur / 60).toInt()
            else -> cur
        }
        labels.add("$prefix$suffix")
    }

    // Generate subtitle with date range
//    val firstDate = monthData.first().day
//    val lastDate = monthData.last().day
//    val firstComponents = firstDate.split("-")
//    val lastComponents = lastDate.split("-")
//
//    // Assuming format is YYYY-MM-DD
//    val firstMonth = when (firstComponents[1]) {
//        "01" -> "Jan"
//        "02" -> "Feb"
//        "03" -> "Mar"
//        "04" -> "Apr"
//        "05" -> "May"
//        "06" -> "Jun"
//        "07" -> "Jul"
//        "08" -> "Aug"
//        "09" -> "Sep"
//        "10" -> "Oct"
//        "11" -> "Nov"
//        "12" -> "Dec"
//        else -> "???"
//    }
//
//    val lastMonth = when (lastComponents[1]) {
//        "01" -> "Jan"
//        "02" -> "Feb"
//        "03" -> "Mar"
//        "04" -> "Apr"
//        "05" -> "May"
//        "06" -> "Jun"
//        "07" -> "Jul"
//        "08" -> "Aug"
//        "09" -> "Sep"
//        "10" -> "Oct"
//        "11" -> "Nov"
//        "12" -> "Dec"
//        else -> "???"
//    }
//
//    val firstDay = firstComponents[2].toInt()
//    val lastDay = lastComponents[2].toInt()
//
//    // Calculate week number (simplified)
//    val calendar = Calendar.getInstance()
//    calendar.set(firstComponents[0].toInt(), firstComponents[1].toInt() - 1, firstDay)
//    val weekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
//
//    val subtitle = "$firstMonth $firstDay - $lastMonth $lastDay (Week $weekNumber)"

    return MonthlyBarData(
        title = title,
        subtitle = "Testing",
        averageValue = averageValueFormatted,
        averageValueActual = average,
        dailyData = dailyData,
        yAxisConfig = YAxisConfig(
            intervals = intervals,
            labels = labels
        )
    )
}