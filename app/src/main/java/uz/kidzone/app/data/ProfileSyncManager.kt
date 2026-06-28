// app/src/main/java/uz/kidzone/app/data/ProfileSyncManager.kt
package uz.kidzone.app.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ProfileSyncManager(private val db: FirebaseFirestore?) {

    companion object {
        private const val TAG = "ProfileSyncManager"
    }

    fun pushProfile(profile: ProfileEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = db ?: return
        val data = mapOf(
            "name" to profile.name,
            "language" to profile.language,
            "timeLimitMinutes" to profile.timeLimitMinutes,
            "pinHash" to (profile.pinHash ?: ""),
            "isDefault" to profile.isDefault,
            "createdAt" to profile.createdAt,
        )
        db.collection("users").document(uid)
            .collection("profiles").document(profile.id)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "pushProfile failed: $e") }
    }

    fun pushStats(stats: ProfileStatsEntity) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = db ?: return
        val data = mapOf(
            "minutesPlayed" to stats.minutesPlayed,
            "gamesPlayed" to stats.gamesPlayed,
        )
        db.collection("users").document(uid)
            .collection("profiles").document(stats.profileId)
            .collection("stats").document(stats.date)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "pushStats failed: $e") }
    }

    suspend fun pullProfiles(uid: String): List<ProfileEntity> {
        val db = db ?: return emptyList()
        return try {
            val snap = db.collection("users").document(uid)
                .collection("profiles").get().await()
            snap.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                ProfileEntity(
                    id = doc.id,
                    name = name,
                    avatarPath = null,
                    language = doc.getString("language") ?: "uz",
                    timeLimitMinutes = (doc.getLong("timeLimitMinutes") ?: 0L).toInt(),
                    pinHash = doc.getString("pinHash")?.takeIf { it.isNotEmpty() },
                    isDefault = doc.getBoolean("isDefault") ?: false,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "pullProfiles failed: $e")
            emptyList()
        }
    }
}
