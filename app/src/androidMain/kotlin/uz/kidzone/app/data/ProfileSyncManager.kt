// app/src/main/java/uz/kidzone/app/data/ProfileSyncManager.kt
package uz.kidzone.app.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

open class ProfileSyncManager(private val db: FirebaseFirestore?) {

    companion object {
        private const val TAG = "ProfileSyncManager"
    }

    open fun pushProfile(profile: ProfileEntity) {
        val uid = ProfileOwner.uidFor(FirebaseAuth.getInstance().currentUser?.uid) ?: return
        val db = db ?: return
        val data = mapOf(
            "name" to profile.name,
            "language" to profile.language,
            "timeLimitMinutes" to profile.timeLimitMinutes,
            "pinHash" to (profile.pinHash ?: ""),
            "isDefault" to profile.isDefault,
            "createdAt" to profile.createdAt,
            "updatedAt" to profile.updatedAt,
            "deleted" to profile.deleted,
            // Not read yet: lets the merge move to server time later without a migration.
            "serverUpdatedAt" to FieldValue.serverTimestamp(),
        )
        db.collection("users").document(uid)
            .collection("profiles").document(profile.id)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "pushProfile failed: $e") }
    }

    // Historical/analytics data only (parent dashboard charts) — not authoritative for the
    // daily play-time limit. That decision comes from the backend's /playtime/heartbeat
    // (PlaytimeApiClient), which tracks elapsedSeconds under profiles/{id}/playtime/{date}.
    open fun pushStats(stats: ProfileStatsEntity) {
        val uid = ProfileOwner.uidFor(FirebaseAuth.getInstance().currentUser?.uid) ?: return
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

    open suspend fun pullProfiles(uid: String): List<ProfileEntity> {
        val db = db ?: return emptyList()
        return try {
            val snap = db.collection("users").document(uid)
                .collection("profiles").get().await()
            snap.documents.mapNotNull(::toProfile)
        } catch (e: Exception) {
            Log.w(TAG, "pullProfiles failed: $e")
            emptyList()
        }
    }

    /**
     * The profiles as the server has them, or null if it can't be reached. Unlike [pullProfiles],
     * an empty list here really means "no profiles", not "offline".
     */
    open suspend fun pullProfilesFromServer(uid: String): List<ProfileEntity>? {
        val db = db ?: return null
        return try {
            val snap = db.collection("users").document(uid)
                .collection("profiles").get(Source.SERVER).await()
            snap.documents.mapNotNull(::toProfile)
        } catch (e: Exception) {
            Log.w(TAG, "pullProfilesFromServer failed: $e")
            null
        }
    }

    /**
     * Calls [onChange] with the account's profiles whenever another device changes them. This
     * device's own writes are skipped until the server has them (they're already in Room).
     * Close the result to stop listening; null when Firestore isn't available.
     */
    open fun listenProfiles(uid: String, onChange: (List<ProfileEntity>) -> Unit): AutoCloseable? {
        val db = db ?: return null
        val registration = db.collection("users").document(uid).collection("profiles")
            .addSnapshotListener { snap, e ->
                if (e != null) { Log.w(TAG, "listenProfiles failed: $e"); return@addSnapshotListener }
                if (snap == null || snap.metadata.hasPendingWrites()) return@addSnapshotListener
                onChange(snap.documents.mapNotNull(::toProfile))
            }
        return AutoCloseable { registration.remove() }
    }

    /**
     * Calls [onChange] with `users/{uid}.familyOwner` (null when not in a family) now and
     * whenever the backend links or unlinks this account.
     */
    open fun listenFamilyOwner(uid: String, onChange: (String?) -> Unit): AutoCloseable? {
        val db = db ?: return null
        val registration = db.collection("users").document(uid)
            .addSnapshotListener { snap, e ->
                if (e != null) { Log.w(TAG, "listenFamilyOwner failed: $e"); return@addSnapshotListener }
                onChange(snap?.getString("familyOwner"))
            }
        return AutoCloseable { registration.remove() }
    }

    private fun toProfile(doc: DocumentSnapshot): ProfileEntity? {
        val name = doc.getString("name") ?: return null
        return ProfileEntity(
            id = doc.id,
            name = name,
            avatarPath = null,
            language = doc.getString("language") ?: "uz",
            timeLimitMinutes = (doc.getLong("timeLimitMinutes") ?: 0L).toInt(),
            pinHash = doc.getString("pinHash")?.takeIf { it.isNotEmpty() },
            isDefault = doc.getBoolean("isDefault") ?: false,
            createdAt = doc.getLong("createdAt") ?: 0L,
            // Written before Faza 16: treat as last changed when created, and alive.
            updatedAt = doc.getLong("updatedAt") ?: doc.getLong("createdAt") ?: 0L,
            deleted = doc.getBoolean("deleted") ?: false,
        )
    }
}
