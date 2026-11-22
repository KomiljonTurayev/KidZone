package uz.kidzone.app.ai

import android.util.Log
import uz.kidzone.app.auth.AuthGraph
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

    /**
     * Until this time (elapsedRealtime) the backend is skipped and callers go
     * straight to on-device TTS. Without it every Uzbek phrase waited on the
     * network first: the backend sleeps on Render's free tier (40–60 s cold
     * start) and had no /ai/tts at all, so speech came seconds late.
     */
    @Volatile private var skipUntil = 0L
    private const val RETRY_AFTER_ERROR_MS = 5 * 60_000L

    suspend fun synthesize(text: String, cacheDir: File?): File? {
        if (cacheDir == null) return null
        if (android.os.SystemClock.elapsedRealtime() < skipUntil) return null
        return try {
            val idToken = AuthGraph.idTokens.idToken() ?: return null
            downloadFromBackend(text.take(1000), idToken, cacheDir)
        } catch (e: Exception) {
            Log.w(TAG, "Speech generation failed: ${e.javaClass.simpleName}: ${e.message}")
            backOff(RETRY_AFTER_ERROR_MS)
            null
        }
    }

    private fun backOff(ms: Long) {
        val now = android.os.SystemClock.elapsedRealtime()
        skipUntil = if (ms == Long.MAX_VALUE) Long.MAX_VALUE else now + ms
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
                // Short: a child is waiting for the voice; on-device TTS is the fallback.
                connectTimeout = 3_000
                // Backend synchronously waits on Aisha's submit/poll/download cycle;
                // longer than this and the voice no longer matches the screen.
                readTimeout = 6_000
            }
            conn.outputStream.use { it.write(payload) }

            val code = conn.responseCode
            if (code !in 200..299) {
                Log.w(TAG, "Backend TTS failed: HTTP $code")
                // No endpoint / not allowed: won't fix itself during this session.
                backOff(if (code in setOf(401, 403, 404, 405, 501)) Long.MAX_VALUE else RETRY_AFTER_ERROR_MS)
                return@withContext null
            }
            val file = File(cacheDir, "kidzo_tts_${System.currentTimeMillis()}.wav")
            conn.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            file
        }
}
