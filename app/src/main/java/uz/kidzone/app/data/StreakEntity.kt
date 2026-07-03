package uz.kidzone.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val profileId: String,
    @ColumnInfo(defaultValue = "0") val count: Int = 0,
    @ColumnInfo(defaultValue = "") val lastCompletedDate: String = "",  // "YYYY-MM-DD"
)
