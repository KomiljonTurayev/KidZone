package uz.kidzone.app

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Constructor is internal so tests in the same module can do:
 *   new FirestoreSync(null)  (Java) / FirestoreSync(null) (Kotlin)
 */
open class FirestoreSync internal constructor(private val db: FirebaseFirestore?) {

    companion object {
        private const val TAG = "FirestoreSync"

        @Volatile private var instance: FirestoreSync? = null

        @JvmStatic
        @Synchronized
        fun init(@Suppress("UNUSED_PARAMETER") ctx: Context): FirestoreSync {
            return instance ?: run {
                val db: FirebaseFirestore? = try {
                    FirebaseFirestore.getInstance()
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore init FAILED: ${e.message}")
                    null
                }
                FirestoreSync(db).also {
                    instance = it
                    Log.d(TAG, "init complete, available=${db != null}")
                }
            }
        }

        @JvmStatic
        @Synchronized
        fun getInstance(): FirestoreSync = instance ?: FirestoreSync(null)

        /**
         * Package-private for unit testing — callable as FirestoreSync.normalizeAgeGroup() from Java.
         * @JvmStatic makes it a true static method in the compiled bytecode.
         */
        @JvmStatic
        fun normalizeAgeGroup(ageGroup: String?): String {
            return when (ageGroup) {
                "2-4" -> "3-5"
                "3-5", "5-7", "7+" -> ageGroup
                else -> "3-5"
            }
        }
    }

    interface UserListCallback {
        fun onResult(users: List<UserInfo>)
    }

    data class UserInfo(
        @JvmField val uid: String,
        @JvmField val email: String,
        @JvmField val status: String
    )

    fun isAvailable(): Boolean = db != null

    /** Package-private for BanChecker and ParentalDashboardActivity. */
    fun getDb(): FirebaseFirestore? = db

    fun syncUserProfile(uid: String?, displayName: String?, email: String?, ageGroup: String?) {
        Log.d(TAG, "syncUserProfile: uid=$uid available=${isAvailable()}")
        if (!isAvailable() || uid == null) return
        val data = mutableMapOf<String, Any>(
            "displayName" to (displayName ?: ""),
            "email" to (email ?: ""),
            "ageGroup" to normalizeAgeGroup(ageGroup),
            "lastActiveAt" to FieldValue.serverTimestamp()
        )
        val ref: DocumentReference = db!!.collection("users").document(uid)
        ref.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    data["status"] = "active"
                    data["createdAt"] = FieldValue.serverTimestamp()
                } else {
                    if (!snap.contains("createdAt")) {
                        data["createdAt"] = FieldValue.serverTimestamp()
                    }
                    if (!snap.contains("status")) {
                        data["status"] = "active"
                    }
                }
                ref.set(data, SetOptions.merge())
                    .addOnFailureListener { e -> Log.w(TAG, "syncUserProfile failed: $e") }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "syncUserProfile get() failed: $e")
                // Do not set 'status' here to ensure existing ban status is never overwritten on network failure
                ref.set(data, SetOptions.merge())
                    .addOnFailureListener { e2 -> Log.w(TAG, "syncUserProfile set failed: $e2") }
            }
    }

    fun updateFcmToken(uid: String?, token: String?) {
        if (!isAvailable() || uid == null || token == null) return
        val data = mapOf<String, Any>("fcmToken" to token)
        db!!.collection("users").document(uid).set(data, SetOptions.merge())
    }

    fun getAllUsers(callback: UserListCallback) {
        if (!isAvailable()) {
            callback.onResult(emptyList())
            return
        }
        db!!.collection("users").get()
            .addOnSuccessListener { snap ->
                val users = snap.map { doc ->
                    UserInfo(
                        uid = doc.id,
                        email = doc.getString("email") ?: "",
                        status = doc.getString("status") ?: "active"
                    )
                }
                callback.onResult(users)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "getAllUsers failed: $e")
                callback.onResult(emptyList())
            }
    }

    fun setUserStatus(uid: String?, status: String, onDone: Runnable?) {
        if (!isAvailable() || uid == null) return
        val data = mapOf<String, Any>("status" to status)
        db!!.collection("users").document(uid).set(data, SetOptions.merge())
            .addOnSuccessListener { onDone?.run() }
            .addOnFailureListener { e -> Log.w(TAG, "setUserStatus failed: $e") }
    }

    fun setBanner(title: String?, body: String?, url: String?, adminUid: String?) {
        if (!isAvailable()) return
        val data = mapOf<String, Any>(
            "active" to true,
            "title" to (title ?: ""),
            "body" to (body ?: ""),
            "url" to (url ?: ""),
            "createdAt" to FieldValue.serverTimestamp(),
            "createdBy" to (adminUid ?: "")
        )
        db!!.collection("config").document("banner").set(data)
            .addOnFailureListener { e -> Log.w(TAG, "setBanner failed: $e") }
    }

    fun clearBanner() {
        if (!isAvailable()) return
        val data = mapOf<String, Any>("active" to false)
        db!!.collection("config").document("banner").update(data)
            .addOnFailureListener { e -> Log.w(TAG, "clearBanner failed: $e") }
    }

    fun recordSession(uid: String?, sessionMinutes: Long, gamePlays: Map<String, String>?, isFirstSession: Boolean) {
        if (!isAvailable() || uid == null || sessionMinutes <= 0) {
            Log.w(TAG, "recordSession skipped: available=${isAvailable()} uid=$uid minutes=$sessionMinutes")
            return
        }
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        Log.d(TAG, "recordSession: date=$dateKey uid=$uid minutes=$sessionMinutes firstSession=$isFirstSession")
        val stats = mutableMapOf<String, Any>(
            "totalMinutes" to FieldValue.increment(sessionMinutes),
            "totalSessions" to FieldValue.increment(1)
        )
        if (isFirstSession) {
            stats["dau"] = FieldValue.increment(1)
        }
        gamePlays?.forEach { (gameId, gameName) ->
            stats["gameBreakdown.$gameId.gameName"] = gameName
            stats["gameBreakdown.$gameId.playCount"] = FieldValue.increment(1)
        }
        db!!.collection("stats").document(dateKey).set(stats, SetOptions.merge())
            .addOnSuccessListener { Log.d(TAG, "recordSession write OK: $dateKey") }
            .addOnFailureListener { e -> Log.e(TAG, "recordSession write FAILED: ${e.message}") }
    }

    open fun syncStreak(uid: String, profileId: String, count: Int, lastDate: String) {
        if (!isAvailable()) return
        val data = mapOf<String, Any>(
            "streak" to mapOf(
                "count" to count,
                "lastCompletedDate" to lastDate,
            ),
        )
        db!!.collection("users").document(uid)
            .collection("profiles").document(profileId)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "syncStreak failed: $e") }
    }

    open fun syncChallengeCompleted(
        uid: String,
        profileId: String,
        date: String,
        gameId: String,
        gameTitle: String,
    ) {
        if (!isAvailable()) return
        val data = mapOf<String, Any>(
            "gameId" to gameId,
            "gameTitle" to gameTitle,
            "completed" to true,
            "completedAt" to FieldValue.serverTimestamp(),
        )
        db!!.collection("users").document(uid)
            .collection("profiles").document(profileId)
            .collection("daily_challenges").document(date)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.w(TAG, "syncChallengeCompleted failed: $e") }
    }
}
