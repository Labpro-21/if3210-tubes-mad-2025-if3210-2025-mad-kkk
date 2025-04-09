package com.example.purrytify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.data.model.Song
import java.io.File
import kotlin.math.roundToInt


class MediaPlaybackService : Service() {
    private val binder = LocalBinder()
    private var exoPlayer: ExoPlayer? = null
    private var currentSong: Song? = null
    private var playerListener: Player.Listener? = null
    private lateinit var mediaSession: MediaSession

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeExoPlayer()
        setupMediaSession()
    }

    private fun initializeExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()

        playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // Handle song finished
                }
                updateNotification()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification()
            }
        }

        exoPlayer?.addListener(playerListener!!)
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> exoPlayer?.play()
            ACTION_PAUSE -> exoPlayer?.pause()
        }

        updateNotification()
        return START_NOT_STICKY
    }

    fun playSong(song: Song) {
        currentSong = song
        val uri = getUriFromPath(this, song.audioPath!!)
        val mediaItem = MediaItem.fromUri(uri)

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }

        updateNotification()
    }

    fun getCurrentSong(): Song? = currentSong
    fun pausePlayback() = exoPlayer?.pause()
    fun resumePlayback() = exoPlayer?.play()
    fun isPlaying(): Boolean = exoPlayer?.isPlaying ?: false
    fun getCurrentPosition(): Int = (exoPlayer?.currentPosition?.toDouble()?.div(1000))?.roundToInt() ?: 0
    fun getDuration(): Int = (exoPlayer?.duration?.toDouble()?.div(1000))?.roundToInt() ?: 0
    fun seekTo(position: Int) = exoPlayer?.seekTo(position * 1000L)

    private fun updateNotification() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val song = currentSong ?: return createEmptyNotification()

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val isPlaying = exoPlayer?.isPlaying == true
        val actionIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val actionTitle = if (isPlaying) "Pause" else "Play"

        val playPauseIntent = Intent(this, MediaPlaybackService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }

        val playPausePendingIntent = PendingIntent.getService(
            this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .addAction(actionIcon, actionTitle, playPausePendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createEmptyNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Purrytify")
            .setContentText("Ready to play music")
            .setSmallIcon(R.drawable.ic_music_note)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        playerListener?.let { exoPlayer?.removeListener(it) }
        exoPlayer?.release()
        mediaSession.release()
        super.onDestroy()
    }

    fun getUriFromPath(context: Context, path: String): Uri {
        return if (path.startsWith("content://")) path.toUri()
        else File(path).toUri()
    }

    companion object {
        private const val CHANNEL_ID = "MusicPlayerChannel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_PLAY = "com.purrytify.ACTION_PLAY"
        const val ACTION_PAUSE = "com.purrytify.ACTION_PAUSE"
    }
}