package com.example.purrytify.data.repository

import android.content.Context
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.dao.SongLogsDao
import com.example.purrytify.data.entity.SongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class SongRecommendationRepository(
    private val songDao: SongDao,
    private val songLogsDao: SongLogsDao,
    private val context: Context
) {
    private val RECENT_PLAY_THRESHOLD_MILLIS = TimeUnit.DAYS.toMillis(30) // for recent song
    private val RECOMMENDATION_LIMIT = 10 // Total song
    private val NUMBER_OF_TOP_ARTISTS_TO_CONSIDER = 30 // total artist

    fun getLikedArtistsBasedRecommendations(userId: Int): Flow<List<SongEntity>> = flow {
        val topLikedArtists = songLogsDao.getArtistsByLikedSongsCount(userId)
            .take(NUMBER_OF_TOP_ARTISTS_TO_CONSIDER)
            .map { it.artist }

        if (topLikedArtists.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val candidateSongs = songDao.getCandidateRecommendationSongsByArtists(
            userId = userId,
            artists = topLikedArtists,
            recentThreshold = System.currentTimeMillis() - RECENT_PLAY_THRESHOLD_MILLIS
        )

        val recommendedSongs = candidateSongs.shuffled(Random(System.nanoTime()))
            .take(RECOMMENDATION_LIMIT)

        emit(recommendedSongs)
    }

    fun getDurationArtistsBasedRecommendations(userId: Int): Flow<List<SongEntity>> = flow {
        val topDurationArtists = songLogsDao.getArtistsByTotalListeningDuration(userId)
            .take(NUMBER_OF_TOP_ARTISTS_TO_CONSIDER)
            .map { it.artist }

        if (topDurationArtists.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val candidateSongs = songDao.getCandidateRecommendationSongsByArtists(
            userId = userId,
            artists = topDurationArtists,
            recentThreshold = System.currentTimeMillis() - RECENT_PLAY_THRESHOLD_MILLIS
        )

        val recommendedSongs = candidateSongs.shuffled(Random(System.nanoTime()))
            .take(RECOMMENDATION_LIMIT)

        emit(recommendedSongs)
    }
}