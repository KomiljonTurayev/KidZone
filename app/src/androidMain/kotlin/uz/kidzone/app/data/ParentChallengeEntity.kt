package uz.kidzone.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A goal a parent set from the dashboard (Faza 18): "reach level [targetLevel] in [gameId]".
 * One at a time per child; setting a new one replaces it.
 */
@Entity(tableName = "parent_challenge")
data class ParentChallengeEntity(
    @PrimaryKey val profileId: String,
    val gameId: String,
    val targetLevel: Int,
    val createdAt: Long,
    val completedAt: Long? = null,
)

@Dao
interface ParentChallengeDao {
    @Query("SELECT * FROM parent_challenge WHERE profileId = :profileId LIMIT 1")
    fun observe(profileId: String): Flow<ParentChallengeEntity?>

    @Query("SELECT * FROM parent_challenge WHERE profileId = :profileId LIMIT 1")
    suspend fun get(profileId: String): ParentChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ParentChallengeEntity)

    @Query("DELETE FROM parent_challenge WHERE profileId = :profileId")
    suspend fun clear(profileId: String)
}
