package com.example.purrytify.data.database

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.purrytify.R
import com.example.purrytify.data.repository.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import android.util.Log

object DatabasePrePopulator {
    fun prepopulateIfEmpty(context: Context, repository: SongRepository, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            val songs = repository.allSongs.first()

//            if (songs.isEmpty()) {
//                withContext(Dispatchers.IO) {
//                    insertSampleSongs(context, repository)
//                }
//            }
            if (!songs.isEmpty()) {
                deleteAllSongs(repository)
            }
        }
    }

    private suspend fun insertSampleSongs(context: Context, repository: SongRepository) {
        val sampleSongs = listOf(
            Triple("Starboy", "The Weeknd", R.drawable.starboy),
            Triple("Here Comes The Sun", "The Beatles", R.drawable.here_comes),
            Triple("Midnight Pretenders", "Tomoko Aran", R.drawable.midnight),
            Triple("Violent Crimes", "Kanye West", R.drawable.violent),
            Triple("Jazz is for ordinary people", "berlioz", R.drawable.jazz),
            Triple("Loose", "Daniel Caesar", R.drawable.starboy),
            Triple("Nights", "Frank Ocean", R.drawable.here_comes),
            Triple("Kiss of Life", "Sade", R.drawable.midnight),
            Triple("BEST INTEREST", "Tyler, The Creator", R.drawable.violent),
            Triple("Blinding Lights", "The Weeknd", R.drawable.jazz)
        )

        for ((title, artist, imagePathResId) in sampleSongs) {
//            val song = SongEntity(
//                title = title,
//                artist = artist,
//                imagePath = imagePathResId,
//                audioPath = null, // No audio path for samples
//                isLiked = (0..1).random() == 1 // Randomly set some as liked
//            )
            var resID = imagePathResId
            var path = (ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                    context.resources.getResourcePackageName(resID) + '/' +
                    context.resources.getResourceTypeName(resID) + '/' +
                    context.resources.getResourceEntryName(resID)).toUri();
            // val path = ("android.resource://" + getPackageResourcePath() + "/" + R.drawable.starboy).toUri()
            repository.insertSong(title, artist, path, null)
        }
        Log.d("SEEDER", "SEEDER SUCCESSS")
    }

    private suspend fun deleteAllSongs(repository: SongRepository) {
        repository.deleteAllSongs()
    }

    fun prepopulateDatabase(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        val songDao = SongDatabase.getDatabase(context).songDao()
        val repository = SongRepository(songDao, context)

        prepopulateIfEmpty(context, repository, lifecycleScope)
    }
}