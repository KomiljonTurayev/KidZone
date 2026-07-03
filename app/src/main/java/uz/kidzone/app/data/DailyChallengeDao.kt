package uz.kidzone.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyChallengeDao {
    @Query("SELECT * FROM daily_challenge WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DailyChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DailyChallengeEntity)

    @Query("UPDATE daily_challenge SET completed = 1 WHERE id = :id")
    suspend fun markCompleted(id: String)

    @Query("SELECT * FROM daily_challenge WHERE profileId = :profileId ORDER BY date DESC")
    fun getByProfile(profileId: String): Flow<List<DailyChallengeEntity>>
}
