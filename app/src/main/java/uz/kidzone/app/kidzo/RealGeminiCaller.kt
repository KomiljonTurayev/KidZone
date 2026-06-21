package uz.kidzone.app.kidzo

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.java.GenerativeModelFutures
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import uz.kidzone.app.BuildConfig
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class RealGeminiCaller : GeminiCaller {

    private val model: GenerativeModelFutures
    private val executor: Executor = Executors.newSingleThreadExecutor()

    init {
        val gm = GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY)
        model = GenerativeModelFutures.from(gm)
    }

    override fun call(prompt: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        val promptContent = content { text(prompt) }
        val future = model.generateContent(promptContent)
        Futures.addCallback(future, object : FutureCallback<GenerateContentResponse> {
            override fun onSuccess(result: GenerateContentResponse) {
                onResult(result.text ?: "")
            }

            override fun onFailure(t: Throwable) {
                onError(t.message ?: "Xato yuz berdi")
            }
        }, executor)
    }
}
