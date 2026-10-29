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
    /** 0 = play [gameId] (the original challenge), 1 = reach a new level, 2 = earn [target] stars. */
    @ColumnInfo(defaultValue = "0") val slot: Int = 0,
    @ColumnInfo(defaultValue = "0") val target: Int = 0,
    @ColumnInfo(defaultValue = "0") val progress: Int = 0,
) {
    companion object {
        const val SLOT_PLAY = 0
        const val SLOT_LEVEL = 1
        const val SLOT_STARS = 2

        /** Slot 0 keeps the old id so rows written before Faza 18 still count. */
        fun idFor(profileId: String, date: String, slot: Int): String =
            if (slot == SLOT_PLAY) "$profileId-$date" else "$profileId-$date-$slot"
    }
}
