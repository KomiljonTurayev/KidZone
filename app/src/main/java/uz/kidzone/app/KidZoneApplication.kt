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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class KidZoneApplication : Application() {

    companion object {
        const val CHANNEL_ID = "kidzone_push"

        @JvmStatic
        lateinit var httpClient: OkHttpClient
            private set
    }

    override fun onCreate() {
        super.onCreate()
        httpClient = OkHttpClient()
        if (BuildConfig.DEBUG) {
            FirebaseFirestore.setLoggingEnabled(true)
        }

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
        createNotificationChannel()
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
                registerTokenWithBackend(token)
            }
            .addOnFailureListener { e ->
                Log.w("KZ_DEBUG", "FCM token fetch failed: ${e.message}")
            }
    }

    private fun registerTokenWithBackend(fcmToken: String) {
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(false)
            ?.addOnSuccessListener { result ->
                val idToken = result.token ?: return@addOnSuccessListener
                val json = """{"fcmToken":"$fcmToken"}"""
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://kidzone-backend-s7to.onrender.com/push/register-token")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(body)
                    .build()
                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.w("KZ_DEBUG", "registerToken failed: ${e.message}")
                    }
                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        Log.d("KZ_DEBUG", "registerToken HTTP ${response.code}")
                        response.close()
                    }
                })
            }
            ?.addOnFailureListener { e ->
                Log.w("KZ_DEBUG", "getIdToken failed: ${e.message}")
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
}
