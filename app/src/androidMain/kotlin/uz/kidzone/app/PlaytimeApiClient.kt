package uz.kidzone.app

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HeartbeatResult(
    val remainingSeconds: Long,
    val limitMinutes: Int,
    val limitReached: Boolean,
    val noLimit: Boolean,
)

/**
 * Reports play-time heartbeats to the KidZone backend's /playtime/heartbeat endpoint, which
 * is the sole authority on elapsed time (its own clock, its own day boundary) — see this
 * repo's Kidzone_Backend PlaytimeService. Returns null on any network/backend failure so
 * callers can fall back to local tracking (ParentalStatsManager) without breaking the UI.
 */
object PlaytimeApiClient {

    private const val TAG = "PlaytimeApiClient"
    private val BACKEND_URL = "${BackendConfig.BASE_URL}/playtime/heartbeat"

    // Short timeouts so a slow/sleeping backend (e.g. a cold-started free-tier dyno) falls
    // back to local tracking quickly instead of stalling the countdown badge.
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    suspend fun heartbeat(profileId: String): HeartbeatResult? = withContext(Dispatchers.IO) {
        try {
            val idToken = currentIdToken() ?: return@withContext null

            val requestJson = JSONObject().apply {
                put("profileId", profileId)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(BACKEND_URL)
                .addHeader("Authorization", "Bearer $idToken")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Heartbeat HTTP failed: ${response.code}")
                    return@withContext null
                }
                val respBody = response.body?.string() ?: return@withContext null
                val root = JSONObject(respBody)
                HeartbeatResult(
                    remainingSeconds = root.optLong("remainingSeconds"),
                    limitMinutes = root.optInt("limitMinutes"),
                    limitReached = root.optBoolean("limitReached"),
                    noLimit = root.optBoolean("noLimit"),
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Heartbeat call failed: ${e.message}")
            null
        }
    }

    private fun currentIdToken(): String? = try {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        Tasks.await(user.getIdToken(false)).token
    } catch (e: Exception) {
        Log.w(TAG, "getIdToken failed: ${e.message}")
        null
    }
}
