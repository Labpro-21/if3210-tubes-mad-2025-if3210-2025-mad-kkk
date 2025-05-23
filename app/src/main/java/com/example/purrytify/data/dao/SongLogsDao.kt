package com.example.purrytify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.purrytify.data.entity.SongEntity
import kotlinx.coroutines.flow.Flow
import com.example.purrytify.data.entity.SongLogsEntity
import com.example.purrytify.ui.model.MonthDataValue


@Dao
interface SongLogsDao {
    @Insert
    suspend fun insertSongLog(songLog: SongLogsEntity)

    @Query("SELECT SUM(duration) FROM song_logs WHERE userId = :userId AND at >= :startOfMonth AND at <= :endOfMonth")
    fun getTotalListeningTimeForMonth(userId: Int, startOfMonth: Long, endOfMonth: Long): Flow<Int?>

    @Query("""
    SELECT s.id, s.artist, s.title, s.imagePath, COUNT(*) as playCount 
    FROM song_logs l 
    JOIN songs s ON l.id = s.id AND l.userId = s.userId 
    WHERE l.userId = :userId AND l.at >= :startOfMonth AND l.at <= :endOfMonth 
    GROUP BY s.id
    ORDER BY playCount DESC 
    """)
    fun getTopSongForMonth(userId: Int, startOfMonth: Long, endOfMonth: Long): Flow<List<TopSongResult?>>

    @Query("""
    SELECT s.artist, s.imagePath, SUM(duration) as artistPlayCount 
    FROM song_logs l 
    JOIN songs s ON l.id = s.id AND l.userId = s.userId 
    WHERE l.userId = :userId AND l.at >= :startOfMonth AND l.at <= :endOfMonth 
    GROUP BY s.artist, s.imagePath 
    ORDER BY artistPlayCount DESC
    """)
    fun getTopArtistForMonth(userId: Int, startOfMonth: Long, endOfMonth: Long): Flow<List<TopArtistResult?>>

    @Query("SELECT COUNT(DISTINCT date(at/1000, 'unixepoch')) FROM song_logs WHERE userId = :userId AND at >= :startDate AND at <= :endDate")
    fun getConsecutiveDaysCount(userId: Int, startDate: Long, endDate: Long): Flow<Int>

    @Query("""
    SELECT s.id, s.title, s.artist, s.imagePath, l.at 
    FROM song_logs l 
    JOIN songs s ON l.id = s.id AND l.userId = s.userId 
    WHERE l.userId = :userId 
    AND l.at >= :startDate AND l.at <= :endDate 
    ORDER BY l.at DESC
""")
    fun getSongLogsForDateRange(userId: Int, startDate: Long, endDate: Long): Flow<List<SongLogWithDetails>>

    @Query("""
    SELECT date(at/1000, 'unixepoch') as day, COUNT(*) as count 
    FROM song_logs 
    WHERE userId = :userId 
    AND at >= :startDate AND at <= :endDate 
    GROUP BY day
    ORDER BY at DESC
""")
    fun getDailyPlayCounts(userId: Int, startDate: Long, endDate: Long): Flow<List<DailyPlayCount>>

    @Query("""
        SELECT DISTINCT log1.id
        FROM song_logs log1
        GROUP BY 
            log1.id, DATE(log1.at / 1000, 'unixepoch')
    """)
    suspend fun getSongsWithStreak(): List<Long>

    /**
     * Returns full song information for songs with a streak of at least 3 days
     */
    @Query("""
        SELECT DISTINCT s.*
        FROM songs s
        JOIN song_logs log1 ON s.id = log1.id
        GROUP BY 
            s.id, DATE(log1.at / 1000, 'unixepoch')
    """)
    suspend fun getSongsWithStreakFullInfo(): List<SongEntity>

//    @Query("""
//    SELECT
//        s.id,
//        s.title,
//        s.artist,
//        s.imagePath,
//        s.userId,
//        strftime('%s', MIN(listen_date)) * 1000 as start_date,
//        strftime('%s', MAX(listen_date)) * 1000 as end_date,
//        COUNT(*) as streak_length,
//        SUM(daily_duration) as total_duration
//    FROM (
//        SELECT
//            id,
//            userId,
//            DATE(at / 1000, 'unixepoch') as listen_date,
//            SUM(duration) as daily_duration,
//            -- Calculate streak groups: dates with same difference between row number and actual date are in the same streak
//            (julianday(DATE(at / 1000, 'unixepoch')) -
//             (ROW_NUMBER() OVER (PARTITION BY id, userId ORDER BY DATE(at / 1000, 'unixepoch')))) as streak_group
//        FROM song_logs
//        WHERE userId = :userId
//          AND at >= :startOfMonth
//          AND at <= :endOfMonth
//        GROUP BY id, userId, listen_date
//    ) streak_detection
//    JOIN songs s ON streak_detection.id = s.id AND streak_detection.userId = s.userId
//    GROUP BY s.id, s.userId, streak_group
//    HAVING streak_length >= 1
//    ORDER BY streak_length DESC, total_duration DESC
//    LIMIT 1
//""")
//    suspend fun getTopSongStreakForMonth(userId: Int, startOfMonth: Long, endOfMonth: Long): SongWithStreakInfo?

    @Query("""
    SELECT 
        s.id,
        s.title,
        s.artist, 
        s.imagePath,
        s.userId,
        DATE(l.at / 1000, 'unixepoch') as listen_date,
        SUM(l.duration) as daily_duration
    FROM song_logs l
    JOIN songs s ON l.id = s.id AND l.userId = s.userId
    WHERE l.userId = :userId
      AND l.at >= :startOfMonth 
      AND l.at <= :endOfMonth
    GROUP BY s.id, s.userId, listen_date
    ORDER BY s.id, listen_date
""")
    suspend fun getSongLogsByDay(userId: Int, startOfMonth: Long, endOfMonth: Long): List<SongDailyLog>

    @Query("""
    SELECT SUM(duration) as total
    FROM song_logs
    WHERE id = :songId
    AND userId = :userId
    AND at >= :startDate
    AND at <= :endDate
""")
    suspend fun getTotalDurationForSongBetweenDates(songId: Long, userId: Int, startDate: Long, endDate: Long): Int

    @Query("""
    SELECT DISTINCT 
        s.id,
        s.title,
        s.artist, 
        s.imagePath,
        s.userId,
        DATE(l.at / 1000, 'unixepoch') as listen_date
    FROM song_logs l
    JOIN songs s ON l.id = s.id AND l.userId = s.userId
    WHERE l.userId = :userId
      AND l.at >= :startOfMonth 
      AND l.at <= :endOfMonth
    ORDER BY s.id, listen_date
""")
    suspend fun getSongPlayDates(userId: Int, startOfMonth: Long, endOfMonth: Long): List<SongPlayDate>

    @Query("""
    SELECT MIN(at) as earliest_date FROM song_logs
    """)
    suspend fun getEarliestLog(): SongEarliestDate

    @Query("""
    SELECT SUM(duration) as value, DATE(at/1000, 'unixepoch') as day
    FROM song_logs 
    WHERE strftime('%m', DATE(at/1000, 'unixepoch')) = printf('%02d', :month)
    AND strftime('%Y', DATE(at/1000, 'unixepoch')) = printf('%d', :year)
    GROUP BY DATE(at/1000, 'unixepoch')
    ORDER BY DATE(at/1000, 'unixepoch')
""")
    fun getMonthData(month: Int, year: Int) : Flow<List<MonthDataValue>>
}

data class TopSongResult(
    val id: Long,
    val artist: String,
    val title: String,
    val imagePath: String,
    val playCount: Int
)
data class TopArtistResult(
    val artist: String,
    val imagePath: String,
    val artistPlayCount: Int
)
data class SongLogWithDetails(
    val id: Long,
    val title: String,
    val artist: String,
    val imagePath: String,
    val at: Long
)
data class DailyPlayCount(
    val day: String,
    val count: Int
)
data class SongStreakInfo(
    val start_date: String,
    val end_date: String,
    val streak_length: Int
)
data class SongWithStreakInfo(
    val id: Long,
    val title: String,
    val artist: String,
    val imagePath: String,
    val userId: Int,
    val start_date: Long,
    val end_date: Long,
    val streak_length: Int,
    val total_duration: Int
)
data class SongDailyLog(
    val id: Long,
    val title: String,
    val artist: String,
    val imagePath: String,
    val userId: Int,
    val listen_date: String,
    val daily_duration: Int
)
data class SongPlayDate(
    val id: Long,
    val title: String,
    val artist: String,
    val imagePath: String,
    val userId: Int,
    val listen_date: String
)
data class SongEarliestDate(
    val earliest_date: Long
)

//SELECT DISTINCT log1.id
//FROM song_logs log1
//JOIN song_logs log2 ON log1.id = log2.id AND
//DATE(log2.at / 1000, 'unixepoch') = DATE(
//DATE(log1.at / 1000, 'unixepoch'), '+1 day'
//)
//JOIN song_logs log3 ON log2.id = log3.id AND
//DATE(log3.at / 1000, 'unixepoch') = DATE(
//DATE(log2.at / 1000, 'unixepoch'), '+1 day'
//)
//GROUP BY
//log1.id, DATE(log1.at / 1000, 'unixepoch')


//SELECT DISTINCT s.*
//FROM songs s
//JOIN song_logs log1 ON s.id = log1.id
//JOIN song_logs log2 ON log1.id = log2.id AND
//DATE(log2.at / 1000, 'unixepoch') = DATE(
//DATE(log1.at / 1000, 'unixepoch'), '+1 day'
//)
//JOIN song_logs log3 ON log2.id = log3.id AND
//DATE(log3.at / 1000, 'unixepoch') = DATE(
//DATE(log2.at / 1000, 'unixepoch'), '+1 day'
//)
//GROUP BY
//s.id, DATE(log1.at / 1000, 'unixepoch')