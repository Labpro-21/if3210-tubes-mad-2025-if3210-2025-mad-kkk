package com.example.purrytify.data.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "song_logs",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("id"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )],
    indices = [Index(value = ["id", "userId"])]
)
data class SongLogsEntity(
    @PrimaryKey(autoGenerate = true)
    val realId: Long = 0,
    val id: Long = 0,
    val userId: Int,
    val duration: Int,
    val at: Long = System.currentTimeMillis(),
)