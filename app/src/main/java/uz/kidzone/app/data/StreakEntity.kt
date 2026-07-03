package uz.kidzone.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val profileId: String,
    val count: Int = 0,
    val lastCompletedDate: String = "",  // "YYYY-MM-DD"
)
