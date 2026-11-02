package uz.kidzone.app

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import uz.kidzone.app.auth.AuthGraph
import java.io.IOException
import java.util.concurrent.TimeUnit

object BackendClient {

    private const val TAG = "BackendClient"
    private val BASE_URL = BackendConfig.BASE_URL
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // The backend's host suspends an idle instance and takes up to ~60s to wake it. The AI
    // clients use short timeouts, so the first Kidzo question after a quiet period would fail;
    // a warm-up ping with a long timeout gets the instance started before it is needed.
    private const val WARM_UP_TIMEOUT_SECONDS = 70L
    private val warmUpClient by lazy {
        KidZoneApplication.httpClient.newBuilder()
            .callTimeout(WARM_UP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(WARM_UP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /** Fire-and-forget GET /health so a sleeping backend wakes before the first real request. */
    @JvmStatic
    fun warmUp() {
        val request = Request.Builder().url("$BASE_URL/health").get().build()
        warmUpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "warmUp failed: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    /**
     * Registers the FCM token with the backend so push notifications reach this device.
     * The backend derives the user identity from the Firebase ID token itself (see
     * PushController#registerToken), so no client-supplied UID is needed here.
     * Failure is logged only.
     */
    @JvmStatic
    fun registerToken(fcmToken: String) {
        scope.launch {
            val idToken = AuthGraph.idTokens.idToken()
            if (idToken == null) {
                Log.w(TAG, "registerToken: no Firebase session")
                return@launch
            }
            doRegisterToken(idToken, fcmToken)
        }
    }

    private fun doRegisterToken(idToken: String, fcmToken: String) {
        try {
            val payload = JSONObject().apply {
                put("fcmToken", fcmToken)
            }
            val mediaType = "application/json".toMediaType()
            val reqBody = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("$BASE_URL/push/register-token")
                .addHeader("Authorization", "Bearer $idToken")
                .post(reqBody)
                .build()

            KidZoneApplication.httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.w(TAG, "registerToken: HTTP failed: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d(TAG, "registerToken HTTP ${response.code}")
                    response.close()
                }
            })
        } catch (e: JSONException) {
            Log.w(TAG, "registerToken: JSON error: $e")
        }
    }

    /** Fetches the first active banner from the backend. Calls callback(null) on any error. */
    @JvmStatic
    fun fetchFirstActiveBanner(callback: (BannerChecker.BannerData?) -> Unit) {
        fetchFirstActiveBanner(null, callback)
    }

    /** Fetches the first active banner matching [ageGroup] (or any age group if null/blank). */
    @JvmStatic
    fun fetchFirstActiveBanner(ageGroup: String?, callback: (BannerChecker.BannerData?) -> Unit) {
        val url = "$BASE_URL/banners".toHttpUrl().newBuilder().apply {
            if (!ageGroup.isNullOrBlank()) addQueryParameter("ageGroup", ageGroup)
        }.build()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        KidZoneApplication.httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "fetchFirstActiveBanner: HTTP failed: $e")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        Log.w(TAG, "fetchFirstActiveBanner: HTTP ${response.code}")
                        response.close()
                        callback(null)
                        return
                    }
                    val bodyStr = response.body?.string() ?: ""
                    response.close()
                    val arr = org.json.JSONArray(bodyStr)
                    if (arr.length() == 0) { callback(null); return }
                    val first = arr.getJSONObject(0)
                    val title = first.optString("title", "")
                    val desc  = first.optString("description", "")
                    val link  = first.optString("link", "")
                    if (link.isEmpty()) { callback(null); return }
                    callback(BannerChecker.BannerData(title = title, body = desc, url = link))
                } catch (e: Exception) {
                    Log.w(TAG, "fetchFirstActiveBanner: parse error: $e")
                    callback(null)
                }
            }
        })
    }
}
