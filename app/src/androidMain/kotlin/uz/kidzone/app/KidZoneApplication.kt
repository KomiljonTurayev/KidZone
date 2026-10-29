package uz.kidzone.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import uz.kidzone.app.auth.AuthException
import uz.kidzone.app.auth.AuthGraph
import uz.kidzone.app.auth.CloudUserListener
import uz.kidzone.app.auth.ProfileCloudSyncListener
import uz.kidzone.app.auth.PurchasesIdentityListener
import uz.kidzone.app.data.KidZoneDatabase
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.data.PrefsLocalProfilesOwner
import uz.kidzone.app.data.ProfileSyncManager
import java.util.UUID

class KidZoneApplication : Application() {

    companion object {
        const val CHANNEL_ID = "kidzone_push"

        @JvmStatic
        val httpClient: OkHttpClient by lazy { OkHttpClient() }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        val prefs: SharedPreferences = getSharedPreferences("kz_prefs", MODE_PRIVATE)
        val db = KidZoneDatabase.getInstance(this)
        val firestore = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
        // Per-account setup (users/{uid} doc, push token, profile sync, purchase identity)
        // runs once per account change from here — on first launch, after sign-in and after
        // sign-out alike — instead of being repeated by each screen that can change the user.
        AuthGraph.init(
            context = this,
            appScope = appScope,
            listeners = listOf(
                CloudUserListener({ FirestoreSync.init(this) }, prefs),
                ProfileCloudSyncListener(db.profileDao(), ProfileSyncManager(firestore), appScope, PrefsLocalProfilesOwner(prefs)),
                PurchasesIdentityListener(),
            ),
        )
        // Everyone starts as a guest: the app is fully usable without signing in.
        appScope.launch {
            try {
                AuthGraph.auth.ensureGuestSession()
            } catch (e: AuthException) {
                Log.e("KZ_DEBUG", "Guest session failed: ${e.reason}")
            }
        }
        createNotificationChannel()
        appScope.launch { migrateToProfilesIfNeeded() }
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
