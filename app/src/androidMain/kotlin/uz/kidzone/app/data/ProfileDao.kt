package uz.kidzone.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE deleted = 0 ORDER BY createdAt ASC")
    fun getAll(): Flow<List<ProfileEntity>>

    /** Every row, tombstones included: sync must push deletes too. */
    @Query("SELECT * FROM profiles")
    suspend fun getAllSync(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id AND deleted = 0 LIMIT 1")
    suspend fun getById(id: String): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profiles WHERE deleted = 0")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    // Qo'shimcha: Bir nechta profillarni bitta qilib saqlash (Batch Insert)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<ProfileEntity>)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()

    /** Applies the remote copies that are newer than the local ones (see [profilesToApply]). */
    @Transaction
    suspend fun mergeProfiles(remoteProfiles: List<ProfileEntity>) {
        val toApply = profilesToApply(getAllSync().associateBy { it.id }, remoteProfiles)
        if (toApply.isNotEmpty()) insertAll(toApply)
    }

    /** Makes [remoteProfiles] the only profiles on this device (another account's took their place). */
    @Transaction
    suspend fun replaceProfiles(remoteProfiles: List<ProfileEntity>) {
        deleteAll()
        insertAll(remoteProfiles)
    }
}

/**
 * Last write wins: a remote copy replaces the local one only if it was changed later. Unknown
 * remote profiles (tombstones too) are taken as they are. The avatar is local-only, so the
 * local path is kept.
 */
fun profilesToApply(local: Map<String, ProfileEntity>, remote: List<ProfileEntity>): List<ProfileEntity> =
    remote.mapNotNull { r ->
        val l = local[r.id]
        when {
            l == null -> r
            r.updatedAt > l.updatedAt -> r.copy(avatarPath = l.avatarPath)
            else -> null
        }
    }
