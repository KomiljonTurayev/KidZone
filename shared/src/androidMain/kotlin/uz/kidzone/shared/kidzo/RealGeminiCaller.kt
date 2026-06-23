package uz.kidzone.shared.kidzo

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import uz.kidzone.app.BuildConfig

class RealGeminiCaller : GeminiCaller {

    private val model: GenerativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    override suspend fun call(prompt: String): String {
        return try {
            val response = model.generateContent(content { text(prompt) })
            response.text ?: ""
        } catch (e: Exception) {
            "" 
        }
    }
}
