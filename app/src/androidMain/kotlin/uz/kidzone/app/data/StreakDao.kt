package uz.kidzone.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak WHERE profileId = :profileId LIMIT 1")
    suspend fun getByProfile(profileId: String): StreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StreakEntity)

    @Query("SELECT * FROM streak")
    fun getAll(): Flow<List<StreakEntity>>
}
