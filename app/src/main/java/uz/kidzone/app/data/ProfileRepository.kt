// app/src/main/java/uz/kidzone/app/data/ProfileRepository.kt
package uz.kidzone.app.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val profileStatsDao: ProfileStatsDao,
    private val prefs: SharedPreferences,
    private val syncManager: ProfileSyncManager,
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
        profileDao.insert(profile)
        syncManager.pushProfile(profile)
    }

    suspend fun update(profile: ProfileEntity) {
        profileDao.update(profile)
        syncManager.pushProfile(profile)
    }

    suspend fun delete(profile: ProfileEntity) {
        profileDao.delete(profile)
    }

    suspend fun upsertStats(stats: ProfileStatsEntity) {
        profileStatsDao.upsert(stats)
        syncManager.pushStats(stats)
    }

    suspend fun getStats(profileId: String, date: String): ProfileStatsEntity? =
        profileStatsDao.getByProfileAndDate(profileId, date)

    suspend fun pullAndMergeFromFirestore(uid: String) {
        val remote = syncManager.pullProfiles(uid)
        remote.forEach { remoteProfile ->
            val local = profileDao.getById(remoteProfile.id)
            if (local == null || remoteProfile.createdAt > local.createdAt) {
                profileDao.insert(remoteProfile.copy(avatarPath = local?.avatarPath))
            }
        }
    }
}
