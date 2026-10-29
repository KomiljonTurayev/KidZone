package uz.kidzone.app.data

import androidx.room.Entity
import androidx.room.Index

/**
 * One row per level a child reached in a game (Faza 18). The best level is the highest row;
 * rows dated within the last week show the parent how fast the child is moving.
 */
@Entity(
    tableName = "level_ups",
    primaryKeys = ["profileId", "gameId", "level"],
    indices = [Index(value = ["profileId", "date"])],
)
data class LevelUpEntity(
    val profileId: String,
    val gameId: String,
    val level: Int,
    val date: String,                   // "YYYY-MM-DD", AppClock zone
)

/** A game's row on the parent dashboard. */
data class GameProgress(
    val gameId: String,
    val level: Int,
    val gainedThisWeek: Int,
)
