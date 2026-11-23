package uz.kidzone.app.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uz.kidzone.app.BackendConfig
import uz.kidzone.app.KidZoneApplication
import uz.kidzone.app.auth.AuthGraph
import java.util.concurrent.TimeUnit

/** Sends an AI answer a parent flagged as bad (Google Play AI-generated content policy). */
object AiReportClient {

    private val client by lazy {
        KidZoneApplication.httpClient.newBuilder().readTimeout(90, TimeUnit.SECONDS).build()
    }

    suspend fun report(text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val idToken = AuthGraph.idTokens.idToken() ?: return@withContext false
            val body = JSONObject().put("text", text.take(8000)).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("${BackendConfig.BASE_URL}/ai/report")
                .addHeader("Authorization", "Bearer $idToken")
                .post(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w("AiReportClient", "report failed: ${e.message}")
            false
        }
    }
}
