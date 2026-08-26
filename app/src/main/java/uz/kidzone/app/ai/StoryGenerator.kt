package uz.kidzone.app.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import org.json.JSONObject

data class GeneratedStory(val title: String, val text: String)

/**
 * Generates a short, original children's story via Firebase AI Logic (Gemini).
 * No API key ever lives client-side — Firebase AI Logic authenticates through
 * the app's own Firebase project instead.
 */
object StoryGenerator {

    private const val TAG = "StoryGenerator"

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.6-flash",
            generationConfig = generationConfig {
                temperature = 0.9f
                // gemini-3.6-flash spends part of this budget on internal "thinking"
                // tokens before the visible JSON output (firebase-ai 16.0.0 has no
                // ThinkingConfig knob to disable that), so this needs real headroom
                // beyond the ~250-word story itself.
                maxOutputTokens = 4096
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    mapOf(
                        "title" to Schema.string(),
                        "text" to Schema.string(),
                    ),
                )
            },
            // Strictest available blocking on every category relevant to text output —
            // this app's audience is children aged 2-8.
            safetySettings = listOf(
                // HarmBlockMethod is a Vertex AI-only knob — omitted here since this app
                // uses the Gemini Developer API backend, which rejects it outright.
                SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.LOW_AND_ABOVE),
                SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.LOW_AND_ABOVE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.LOW_AND_ABOVE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.LOW_AND_ABOVE),
            ),
        )
    }

    suspend fun generate(lang: String, ageRange: String): GeneratedStory? {
        return try {
            val response = model.generateContent(buildPrompt(lang, ageRange))
            val raw = response.text?.trim()
            if (raw.isNullOrEmpty()) return null
            val json = JSONObject(raw)
            val title = json.optString("title").trim()
            val text = json.optString("text").trim()
            if (title.isEmpty() || text.isEmpty()) null else GeneratedStory(title, text)
        } catch (e: Exception) {
            var cause: Throwable? = e
            val chain = StringBuilder()
            while (cause != null) {
                chain.append(cause.javaClass.name).append(": ").append(cause.message).append(" <- ")
                cause = cause.cause
            }
            Log.w(TAG, "Story generation failed ($chain), caller will fall back to the static pool")
            null
        }
    }

    private fun buildPrompt(lang: String, ageRange: String): String {
        val languageName = when (lang) {
            "ru" -> "Russian"
            "en" -> "English"
            else -> "Uzbek"
        }
        return """
            You are a children's story writer for the KidZone app.
            Write ONE original, warm, gentle short story in $languageName for a child aged $ageRange.
            Requirements:
            - 150 to 250 words.
            - Simple vocabulary appropriate for the age group.
            - A clear, kind moral or lesson at the end.
            - No violence, fear, romance, or scary content of any kind.
            - Use paragraph breaks (\n\n) between paragraphs, 4 to 6 paragraphs total.
            - Give the story a short, appealing title in $languageName.
            Respond with ONLY a JSON object matching the schema — no extra commentary.
        """.trimIndent()
    }
}
