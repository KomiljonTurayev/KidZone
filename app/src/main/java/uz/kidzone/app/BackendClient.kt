package uz.kidzone.app

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

object BackendClient {

    private const val TAG = "BackendClient"
    private const val BASE_URL = "https://kidzone-backend-s7to.onrender.com"

    /** Registers the FCM token with the backend so UID-targeted pushes work. Failure is logged only. */
    @JvmStatic
    fun registerToken(uid: String, fcmToken: String) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "registerToken: no Firebase user")
            return
        }
        currentUser.getIdToken(false)
            .addOnSuccessListener { result ->
                doRegisterToken(result.token, fcmToken)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "registerToken: getIdToken failed: $e")
            }
    }

    private fun doRegisterToken(idToken: String?, fcmToken: String) {
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
        val request = Request.Builder()
            .url("$BASE_URL/banners")
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
