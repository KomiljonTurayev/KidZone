package uz.kidzone.app.ai

import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.delay
import org.json.JSONObject
import uz.kidzone.app.BuildConfig

/**
 * Genuine Uzbek narration via Aisha AI's "Navoiy TTS" REST API (Gulnoza voice).
 * No on-device Android TTS engine ships an Uzbek voice at all (confirmed via
 * TextToSpeech.isLanguageAvailable — Google's engine's supported-language set
 * doesn't include "uz"), and Firebase AI Logic's Gemini TTS rejects requests from
 * this app's pinned older SDK (its response's server-side validation requires a
 * speechConfig that only the Live API can set) — this is the only source of real
 * Uzbek pronunciation. Caller falls back to on-device TTS on any failure: no
 * network, no/invalid API key, empty balance, or a slow/failed synth job.
 */
object AishaSpeechGenerator {
    private const val TAG = "AishaSpeechGenerator"
    private const val BASE_URL = "https://back.aisha.group"

    suspend fun synthesize(text: String, cacheDir: File?): File? {
        if (cacheDir == null || BuildConfig.AISHA_TTS_API_KEY.isBlank()) return null
        return try {
            val (code, body) = post(text)
            if (code !in 200..299) {
                Log.w(TAG, "TTS request failed: HTTP $code $body")
                return null
            }
            val json = JSONObject(body)
            val audioPath = when {
                json.has("audio_path") -> json.getString("audio_path")
                json.optString("status") == "PENDING" -> pollForResult(json.getInt("id"))
                else -> null
            } ?: return null
            downloadAudio(audioPath, cacheDir)
        } catch (e: Exception) {
            Log.w(TAG, "Speech generation failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    private fun post(text: String): Pair<Int, String> {
        val boundary = "----KidZoneBoundary${System.nanoTime()}"
        val body = buildString {
            fun field(name: String, value: String) {
                append("--").append(boundary).append("\r\n")
                append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n")
                append(value).append("\r\n")
            }
            // 1000-char cap matches the API's authenticated-request limit; stories
            // run ~150-250 words so this only ever trims pathological input.
            field("transcript", text.take(1000))
            field("language", "uz")
            field("model", "Gulnoza")
            field("mood", "Cheerful")
            field("speed", "0.95")
            append("--").append(boundary).append("--\r\n")
        }.toByteArray(Charsets.UTF_8)

        val conn = (URL("$BASE_URL/api/v1/tts/post/").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("X-Api-Key", BuildConfig.AISHA_TTS_API_KEY)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connectTimeout = 15_000
            readTimeout = 20_000
        }
        conn.outputStream.use { it.write(body) }
        val code = conn.responseCode
        val text2 = (if (code in 200..299) conn.inputStream else conn.errorStream)
            .bufferedReader().use { it.readText() }
        return code to text2
    }

    private suspend fun pollForResult(id: Int): String? {
        repeat(15) {
            delay(1_000)
            val conn = (URL("$BASE_URL/api/v1/tts/status/$id/").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("X-Api-Key", BuildConfig.AISHA_TTS_API_KEY)
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            if (conn.responseCode !in 200..299) return@repeat
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            when (json.optString("status")) {
                "SUCCESS" -> return json.getString("audio_path")
                "FAILED" -> return null
            }
        }
        return null
    }

    private fun downloadAudio(audioPath: String, cacheDir: File): File? {
        val url = if (audioPath.startsWith("http")) audioPath else "$BASE_URL$audioPath"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("X-Api-Key", BuildConfig.AISHA_TTS_API_KEY)
            connectTimeout = 15_000
            readTimeout = 20_000
        }
        if (conn.responseCode !in 200..299) return null
        val file = File(cacheDir, "kidzo_tts_${System.currentTimeMillis()}.wav")
        conn.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        return file
    }
}
