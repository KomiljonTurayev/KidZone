// app/src/main/java/uz/kidzone/app/data/ProfileRepository.kt
package uz.kidzone.app.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val profileStatsDao: ProfileStatsDao,
    private val prefs: SharedPreferences,
    private val syncManager: ProfileSyncManager,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    val profiles: Flow<List<ProfileEntity>> = profileDao.getAll()

    fun getActiveProfileId(): String? = prefs.getString("active_profile_id", null)

    fun setActiveProfileId(id: String) {
        prefs.edit().putString("active_profile_id", id).apply()
    }

    suspend fun getActiveProfile(): ProfileEntity? {
        val id = getActiveProfileId() ?: return null
        return profileDao.getById(id)
    }

    suspend fun count(): Int = profileDao.count()

    suspend fun insert(profile: ProfileEntity) {
        val stamped = profile.copy(updatedAt = nowMs())
        profileDao.insert(stamped)
        syncManager.pushProfile(stamped)
    }

    suspend fun update(profile: ProfileEntity) {
        val stamped = profile.copy(updatedAt = nowMs())
        profileDao.update(stamped)
        syncManager.pushProfile(stamped)
    }

    /** Keeps a synced tombstone instead of removing the row, so other devices drop it too. */
    suspend fun delete(profile: ProfileEntity) {
        update(profile.copy(deleted = true))
    }

    suspend fun upsertStats(stats: ProfileStatsEntity) {
        profileStatsDao.upsert(stats)
        syncManager.pushStats(stats)
    }

    suspend fun getStats(profileId: String, date: String): ProfileStatsEntity? =
        profileStatsDao.getByProfileAndDate(profileId, date)

    // Yaxshilangan: N+1 DB operatsiyalari o'rniga bitta so'rovda ishni bitiramiz.
    suspend fun pullAndMergeFromFirestore(uid: String) {
        val remote = syncManager.pullProfiles(uid)
        if (remote.isNotEmpty()) {
            profileDao.mergeProfiles(remote)
        }
    }
}
