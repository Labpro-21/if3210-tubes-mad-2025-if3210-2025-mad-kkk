package com.example.purrytify.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.purrytify.data.dao.DailyPlayCount
import com.example.purrytify.data.dao.SongLogWithDetails
import com.example.purrytify.data.dao.SongLogsDao
import com.example.purrytify.data.dao.SongStreakInfo
import com.example.purrytify.data.dao.SongWithStreakInfo
import com.example.purrytify.data.dao.TopArtistResult
import com.example.purrytify.data.dao.TopSongResult
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.data.entity.SongLogsEntity
import com.example.purrytify.ui.model.TopArtistViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

class SongLogsRepository(
    private val songLogsDao: SongLogsDao,
    private val context: Context
) {
    fun getTotalListeningTimeForMonth(userId: Int, startOfMonth: Long, endOfMonth: Long) : Flow<Int?> {
        return songLogsDao.getTotalListeningTimeForMonth(userId, startOfMonth, endOfMonth)
    }

    suspend fun getTopSongForMonth(userId: Int, startOfMonth: Long, endOfMonth: Long): List<TopSongResult?> {
        return songLogsDao.getTopSongForMonth(userId, startOfMonth, endOfMonth).first()
    }

    suspend fun getTopArtistForMonth(userId: Int, startOfMonth: Long, endOfMonth: Long): List<TopArtistResult?> {
        val topArtist = songLogsDao.getTopArtistForMonth(userId, startOfMonth, endOfMonth).first()
        val nonNullResults = topArtist.filterNotNull()

        // Group entries by artist
        val artistGroups = nonNullResults.groupBy { it.artist }

        // Process each artist group to create consolidated entries
        return artistGroups.map { (artist, entries) ->
            // Calculate total play count for the artist
            val totalPlayCount = entries.sumOf { it.artistPlayCount }

            // Find the entry with the highest individual play count to get its image path
            val entryWithHighestCount = entries.maxByOrNull { it.artistPlayCount }
                ?: throw IllegalStateException("No entries found for artist $artist")

            // Create a new consolidated entry
            TopArtistResult(
                artist = artist,
                imagePath = entryWithHighestCount.imagePath,
                artistPlayCount = totalPlayCount
            )
        }.sortedByDescending { it.artistPlayCount } // Sort by play count in descending order

    }

    fun getDailyPlayCounts(userId: Int, startDate: Long, endDate: Long): Flow<List<DailyPlayCount>> {
        return songLogsDao.getDailyPlayCounts(userId, startDate, endDate)
    }

    fun getSongLogsForDateRange(userId: Int, startDate: Long, endDate: Long): Flow<List<SongLogWithDetails>> {
        return songLogsDao.getSongLogsForDateRange(userId, startDate, endDate)
    }

    suspend fun insertLog(
        id: Long,
        userId: Int,
        duration: Int,
        at: Long
    ) {
        val songLog = SongLogsEntity(
            id = id,
            userId = userId,
            duration = duration,
            at = at
        )
        songLogsDao.insertSongLog(songLog)
    }

    suspend fun getSongsWithMinimumStreak(): List<Long> {
        return songLogsDao.getSongsWithStreak()
    }

    /**
     * Get full song information for songs with streaks of at least 3 days
     */
    suspend fun getSongsWithStreakFullInfo(): List<SongEntity> {
        return songLogsDao.getSongsWithStreakFullInfo()
    }

//    suspend fun getTopSongStreakForMonth(userId: Int, month: Int, year: Int): SongWithStreakInfo? {
//        // Calculate start and end timestamps for the month
//        val calendar = Calendar.getInstance(TimeZone.getDefault())
//
//        // Set to first day of month at 00:00:00
//        calendar.set(year, month - 1, 1, 0, 0, 0)
//        calendar.set(Calendar.MILLISECOND, 0)
//        val startOfMonth = calendar.timeInMillis
//
//        // Move to first day of next month and subtract 1 millisecond to get end of month
//        calendar.add(Calendar.MONTH, 1)
//        val endOfMonth = calendar.timeInMillis - 1
//
//        return songLogsDao.getTopSongStreakForMonth(userId, startOfMonth, endOfMonth)
//    }

    suspend fun getTopSongStreakForMonth(userId: Int, startDate: Long, endDate: Long): SongWithStreakInfo? {
        // Calculate start and end timestamps for the month
//        val calendar = Calendar.getInstance(TimeZone.getDefault())
//
//        // Set to first day of month at 00:00:00
//        calendar.set(year, month - 1, 1, 0, 0, 0)
//        calendar.set(Calendar.MILLISECOND, 0)
//        val startOfMonth = calendar.timeInMillis
//
//        // Move to first day of next month and subtract 1 millisecond to get end of month
//        calendar.add(Calendar.MONTH, 1)
//        val endOfMonth = calendar.timeInMillis - 1

        // Get all distinct days each song was played in the month
        val songPlayDates = songLogsDao.getSongPlayDates(userId, startDate, endDate)

        Log.d("STARTMONTH", startDate.toString())
        Log.d("ENDMONTH", endDate.toString())

        // Group by song ID
        val songGroups = songPlayDates.groupBy { it.id }

        Log.d("SONG GROUPS", songGroups.size.toString())

        var longestStreak: SongWithStreakInfo? = null

        // Process each song to find its longest streak
        for ((_, dates) in songGroups) {
            if (dates.isEmpty()) continue

            // Sort dates chronologically
            val sortedDates = dates.sortedBy { it.listen_date }

            // Find the longest streak for this song
            var currentStreak = 1
            var longestStreakForSong = 1
            var streakStartIndex = 0
            var streakEndIndex = 0
            var currentStreakStart = 0

            for (i in 1 until sortedDates.size) {
                val prevDate = LocalDate.parse(sortedDates[i-1].listen_date)
                val currDate = LocalDate.parse(sortedDates[i].listen_date)

                if (currDate.isEqual(prevDate.plusDays(1))) {
                    // Consecutive day
                    currentStreak++

                    if (currentStreak >= longestStreakForSong) {
                        longestStreakForSong = currentStreak
                        streakStartIndex = currentStreakStart
                        streakEndIndex = i
                    }
                } else if (!currDate.isEqual(prevDate)) {
                    // Streak broken
                    currentStreak = 1
                    currentStreakStart = i
                    if (currentStreak >= longestStreakForSong) {
                        longestStreakForSong = currentStreak
                        streakStartIndex = currentStreakStart
                        streakEndIndex = i
                    }
                }
            }

            // Get the first song details for creating result
            val song = sortedDates.first()

            // Calculate total duration for the streak
            val totalDuration = songLogsDao.getTotalDurationForSongBetweenDates(
                song.id,
                userId,
                LocalDate.parse(sortedDates[streakStartIndex].listen_date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                LocalDate.parse(sortedDates[streakEndIndex].listen_date).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            )

            // Create streak info
            val streakInfo = SongWithStreakInfo(
                id = song.id,
                title = song.title,
                artist = song.artist,
                imagePath = song.imagePath,
                userId = song.userId,
                start_date = LocalDate.parse(sortedDates[streakStartIndex].listen_date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                end_date = LocalDate.parse(sortedDates[streakEndIndex].listen_date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                streak_length = longestStreakForSong,
                total_duration = totalDuration
            )

            // Update longest streak if this one is better
            if (longestStreak == null ||
                streakInfo.streak_length > longestStreak.streak_length ||
                (streakInfo.streak_length == longestStreak.streak_length && streakInfo.total_duration > longestStreak.total_duration)) {
                longestStreak = streakInfo
            }
        }

        return longestStreak
    }

    suspend fun getEarliestLog(): Long {
        return songLogsDao.getEarliestLog().earliest_date
    }


    /**
     * Get detailed streak information for a specific song
     */
//    suspend fun getSongStreakDetails(songId: Long): List<SongStreakInfo> {
//        return songLogsDao.getSongStreakInfo(songId)
//    }
}
