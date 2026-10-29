package uz.kidzone.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelUpDao {
    /** Returns -1 when the level was already recorded. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: LevelUpEntity): Long

    @Query("SELECT MAX(level) FROM level_ups WHERE profileId = :profileId AND gameId = :gameId")
    suspend fun bestLevel(profileId: String, gameId: String): Int?

    @Query(
        "SELECT gameId, MAX(level) AS level, " +
        "SUM(CASE WHEN date >= :since THEN 1 ELSE 0 END) AS gainedThisWeek " +
        "FROM level_ups WHERE profileId = :profileId GROUP BY gameId ORDER BY level DESC"
    )
    fun progress(profileId: String, since: String): Flow<List<GameProgress>>
}
