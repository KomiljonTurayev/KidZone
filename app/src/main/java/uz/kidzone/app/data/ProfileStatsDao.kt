package uz.kidzone.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProfileStatsDao {
    @Query("SELECT * FROM profile_stats WHERE profileId = :profileId AND date = :date LIMIT 1")
    suspend fun getByProfileAndDate(profileId: String, date: String): ProfileStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: ProfileStatsEntity)
}
