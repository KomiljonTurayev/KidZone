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
    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun getAll(): Flow<List<ProfileEntity>>

    // Qo'shimcha: Barcha profillarni sinxron olish uchun
    @Query("SELECT * FROM profiles")
    suspend fun getAllSync(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profiles")
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

    // Qo'shimcha: N+1 muammosini hal qilish uchun Transaction orqali ishlash
    @Transaction
    suspend fun mergeProfiles(remoteProfiles: List<ProfileEntity>) {
        // Avval hamma lokal profillarni xotiraga olamiz (faqat 1 ta DB so'rovi)
        val localProfiles = getAllSync().associateBy { it.id }
        
        // Ularni solishtiramiz
        val toInsert = remoteProfiles.mapNotNull { remote ->
            val local = localProfiles[remote.id]
            if (local == null || remote.createdAt > local.createdAt) {
                // Lokal profil bo'lmasa yoki eskirgan bo'lsa, ro'yxatga qo'shamiz
                remote.copy(avatarPath = local?.avatarPath)
            } else {
                null
            }
        }
        
        // Yangi/O'zgargan profillarni bir paytning o'zida DB ga yozamiz (Yana 1 ta DB so'rovi)
        if (toInsert.isNotEmpty()) {
            insertAll(toInsert)
        }
    }
}
