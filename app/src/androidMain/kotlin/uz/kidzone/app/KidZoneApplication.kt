package uz.kidzone.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import uz.kidzone.app.data.KidZoneDatabase
import uz.kidzone.app.data.ProfileEntity
import java.util.UUID

class KidZoneApplication : Application() {

    companion object {
        const val CHANNEL_ID = "kidzone_push"

        @JvmStatic
        val httpClient: OkHttpClient by lazy { OkHttpClient() }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            if (BuildConfig.DEBUG) {
                FirebaseFirestore.setLoggingEnabled(true)
            }
            DebugAppCheckInit.install()
        } catch (e: Exception) {
            Log.w("KZ_DEBUG", "Firebase debug setup skipped: ${e.message}")
        }

        try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user != null) {
                Log.d("KZ_DEBUG", "Auth OK: uid=${user.uid}")
                syncToFirestore(user.uid)
            } else {
                auth.signInAnonymously()
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener
                        Log.d("KZ_DEBUG", "signInAnonymously OK: uid=$uid")
                        syncToFirestore(uid)
                    }
                    .addOnFailureListener { e ->
                        Log.e("KZ_DEBUG", "signInAnonymously FAILED: ${e.message}")
                    }
            }
        } catch (e: IllegalStateException) {
            Log.w("KZ_DEBUG", "Firebase not available (test environment?): ${e.message}")
        }
        createNotificationChannel()
        CoroutineScope(Dispatchers.IO).launch { migrateToProfilesIfNeeded() }
    }

    private fun syncToFirestore(uid: String) {
        val prefs: SharedPreferences = getSharedPreferences("kz_prefs", MODE_PRIVATE)
        prefs.edit().putString("kz_uid", uid).apply()
        val ageGroup = prefs.getString("kz_age_filter", "3-5") ?: "3-5"
        FirestoreSync.init(this).syncUserProfile(uid, null, null, ageGroup)
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                prefs.edit().putString("kz_fcm_token", token).apply()
                FirestoreSync.getInstance().updateFcmToken(uid, token)
                BackendClient.registerToken(token)
            }
            .addOnFailureListener { e ->
                Log.w("KZ_DEBUG", "FCM token fetch failed: ${e.message}")
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "KidZone Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private suspend fun migrateToProfilesIfNeeded() {
        val db = KidZoneDatabase.getInstance(this)
        if (db.profileDao().count() > 0) return

        val prefs = getSharedPreferences("kz_prefs", MODE_PRIVATE)
        val lang = prefs.getString("kz_lang", "uz") ?: "uz"
        val timeLimit = prefs.getInt("kz_time_limit", 0)
        val pinHash = PinUtil.getOrMigrateHash(prefs, "kz_pin")
        val profileId = UUID.randomUUID().toString()

        val profile = ProfileEntity(
            id = profileId,
            name = "Asosiy",
            avatarPath = null,
            language = lang,
            timeLimitMinutes = timeLimit,
            pinHash = pinHash,
            isDefault = true,
            createdAt = System.currentTimeMillis(),
        )
        db.profileDao().insert(profile)
        prefs.edit()
            .putString("active_profile_id", profileId)
            .putInt("kz_profile_count", 1)
            .apply()
        android.util.Log.d("KZ_DEBUG", "Migration complete: default profile=$profileId")
    }
}
