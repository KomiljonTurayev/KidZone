package uz.kidzone.app.ai

import android.content.Context
import android.util.Log
import uz.kidzone.app.auth.AuthGraph
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Genuine Uzbek narration: a [BundledVoice] clip when the text has one, otherwise
 * the KidZone backend's POST /ai/tts, which calls
 * Azure AI Speech (uz-UZ-MadinaNeural) server-side and returns MP3. The Azure
 * key must never live in the client: it's trivially extractable from the APK.
 * No on-device Android TTS engine ships an Uzbek voice at all (confirmed via
 * TextToSpeech.isLanguageAvailable — Google's engine's supported-language set
 * doesn't include "uz") — these are the only sources of real Uzbek pronunciation.
 * Caller falls back to on-device TTS on any failure: no network, no signed-in
 * user, backend without a speech key (503), or an Azure error (502).
 * (The object keeps its old name: Aisha's Navoiy TTS was the first plan.)
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

    suspend fun synthesize(text: String, context: Context?, childName: String = ""): File? {
        if (context == null) return null
        withContext(Dispatchers.IO) { BundledVoice.find(context, text, childName) }?.let { return it }
        if (android.os.SystemClock.elapsedRealtime() < skipUntil) return null
        return try {
            val idToken = AuthGraph.idTokens.idToken() ?: return null
            downloadFromBackend(text.take(1000), idToken, context.cacheDir)
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
                // Longer than this and the voice no longer matches the screen.
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
            val file = File(cacheDir, "kidzo_tts_${System.currentTimeMillis()}.mp3")
            conn.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            file
        }
}
