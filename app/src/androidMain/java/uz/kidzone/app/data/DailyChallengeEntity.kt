package uz.kidzone.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_challenge")
data class DailyChallengeEntity(
    @PrimaryKey val id: String,         // "$profileId-$date", e.g. "p1-2026-07-01"
    val profileId: String,
    val date: String,                   // "YYYY-MM-DD"
    val gameId: String,
    val gameTitle: String,
    @ColumnInfo(defaultValue = "0") val completed: Boolean = false,
)
