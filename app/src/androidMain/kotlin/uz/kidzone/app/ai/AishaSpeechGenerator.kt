package uz.kidzone.app.ai

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Genuine Uzbek narration via the KidZone backend, which proxies Aisha AI's
 * "Navoiy TTS" REST API (Gulnoza voice) server-side. The Aisha API key must
 * never live in the client: it's trivially extractable from the APK, and the
 * backend also handles Aisha's submit/poll/download dance so the client makes
 * one call. No on-device Android TTS engine ships an Uzbek voice at all
 * (confirmed via TextToSpeech.isLanguageAvailable — Google's engine's
 * supported-language set doesn't include "uz") — this is the only source of
 * real Uzbek pronunciation. Caller falls back to on-device TTS on any failure:
 * no network, no signed-in user, backend/Aisha error, or a slow/failed synth job.
 */
object AishaSpeechGenerator {
    private const val TAG = "AishaSpeechGenerator"
    private val BACKEND_URL = "${uz.kidzone.app.BackendConfig.BASE_URL}/ai/tts"

    suspend fun synthesize(text: String, cacheDir: File?): File? {
        if (cacheDir == null) return null
        return try {
            val idToken = currentIdToken() ?: return null
            downloadFromBackend(text.take(1000), idToken, cacheDir)
        } catch (e: Exception) {
            Log.w(TAG, "Speech generation failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    private suspend fun downloadFromBackend(text: String, idToken: String, cacheDir: File): File? =
        withContext(Dispatchers.IO) {
            val payload = org.json.JSONObject().apply {
                put("text", text)
                put("lang", "uz")
            }.toString().toByteArray(Charsets.UTF_8)

            val conn = (URL(BACKEND_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $idToken")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connectTimeout = 15_000
                // Backend synchronously waits on Aisha's submit/poll/download cycle.
                readTimeout = 30_000
            }
            conn.outputStream.use { it.write(payload) }

            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "Backend TTS failed: HTTP ${conn.responseCode}")
                return@withContext null
            }
            val file = File(cacheDir, "kidzo_tts_${System.currentTimeMillis()}.wav")
            conn.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            file
        }

    private fun currentIdToken(): String? = try {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        Tasks.await(user.getIdToken(false)).token
    } catch (e: Exception) {
        Log.w(TAG, "getIdToken failed: ${e.message}")
        null
    }
}
