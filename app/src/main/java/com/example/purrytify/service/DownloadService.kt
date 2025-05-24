package com.example.purrytify.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.webkit.MimeTypeMap.getFileExtensionFromUrl
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.purrytify.R
import com.example.purrytify.data.database.SongDatabase
import com.example.purrytify.data.repository.SongRepository
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class DownloadRequest(
    val serverId: Int,
    val userId: Int,
    val remoteAudioUrl: String,
    val remoteImageUrl: String,
    val title: String
) : Serializable

class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val CHANNEL_ID = "purrytify_download_channel"
        private const val NOTIF_ID = 1005
        const val EXTRA_DOWNLOAD_LIST = "downloadList"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val downloadList = intent?.getSerializableExtra(EXTRA_DOWNLOAD_LIST) as? ArrayList<DownloadRequest>
        val downloadType = intent?.getStringExtra("downloadType")

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading Songs")
            .setSmallIcon(R.drawable.ic_download)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
        startForeground(NOTIF_ID, builder.build())

        if (downloadList.isNullOrEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }
        Log.d("DOWNLOAD_SERVICE", "Starting downloads...")

        scope.launch {
            val songDao = SongDatabase.getDatabase(applicationContext).songDao()
            val repository = SongRepository(songDao, applicationContext)

            for ((index, request) in downloadList.withIndex()) {
                try {
                    val song = repository.getSongByServerId(request.serverId, request.userId)
                    if (song != null && !song.isDownloaded) {
                        builder.setContentText(song.title)
                        builder.setProgress(downloadList.size, index, false)
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(NOTIF_ID, builder.build())
                        val audioFile = downloadFileToInternalStorage(request.remoteAudioUrl, "audio/${request.userId}/", applicationContext)
                        val imageFile = downloadFileToInternalStorage(request.remoteImageUrl, "images/${request.userId}/", applicationContext)
                        val dl = repository.updateAndGetSong(
                            song.copy(
                                audioPath = audioFile,
                                imagePath = imageFile,
                                isDownloaded = true
                            ),
                            serverId = request.serverId,
                            userId = request.userId
                        )
                        Log.d("DOWNLOAD_SERVICE", "Success: ${dl.toString()}")
                    }
                } catch (e: Exception) {
                    Log.e("DOWNLOAD_SERVICE", "Failed: ${request.title}", e)
                }
            }

            if (downloadType == "TOPGLOBAL") {
                notifyTopGlobalComplete()
            } else if (downloadType == "TOPCOUNTRY") {
                notifyTopCountryComplete()
            }

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_REDELIVER_INTENT
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Download Service"
            val descriptionText = "Downloading songs to purritify"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("purrytify_download_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun notifyTopGlobalComplete() {
        val intent = Intent("com.example.purrytify.DOWNLOAD_TOP_GLOBAL_COMPLETE")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d("DOWNLOAD_SERVICE", "Sending Top Global Broadcast")
    }

    private fun notifyTopCountryComplete() {
        val intent = Intent("com.example.purrytify.DOWNLOAD_TOP_COUNTRY_COMPLETE")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d("DOWNLOAD_SERVICE", "Sending Top Country Broadcast")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun downloadFileToInternalStorage(
        urlString: String,
        folderName: String,
        context: Context
    ): String  {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        connection.connectTimeout = 5000
        connection.readTimeout = 10000
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = true

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("Server returned HTTP ${connection.responseCode}")
        }

        val directory = File(context.filesDir, folderName)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val fileExtension = getFileExtensionFromUrl(urlString)
        val fileName = "${UUID.randomUUID()}.$fileExtension"
        val file = File(directory, fileName)

        connection.inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        return file.absolutePath
    }
}
