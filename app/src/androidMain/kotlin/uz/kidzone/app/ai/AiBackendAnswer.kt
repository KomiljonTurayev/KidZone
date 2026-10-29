package uz.kidzone.app.ai

import org.json.JSONObject

/** What the backend's `/ai/story` and `/ai/ask` said (Faza 17). */
internal sealed interface AiBackendAnswer<out T> {
    data class Ready<T>(val value: T) : AiBackendAnswer<T>

    /** 402: the family's free daily allowance is used up; Premium would allow more. */
    data object DailyLimitReached : AiBackendAnswer<Nothing>

    /** Offline, no model configured yet (503), an empty reply...: the offline engine answers. */
    data object Unavailable : AiBackendAnswer<Nothing>
}

internal const val HTTP_PAYMENT_REQUIRED = 402

internal fun <T> aiBackendAnswer(httpCode: Int, body: String?, parse: (JSONObject) -> T?): AiBackendAnswer<T> {
    if (httpCode == HTTP_PAYMENT_REQUIRED) return AiBackendAnswer.DailyLimitReached
    if (httpCode !in 200..299 || body.isNullOrBlank()) return AiBackendAnswer.Unavailable
    val value = runCatching { parse(JSONObject(body)) }.getOrNull() ?: return AiBackendAnswer.Unavailable
    return AiBackendAnswer.Ready(value)
}

internal fun parseStory(json: JSONObject): GeneratedStory? {
    val title = json.optString("title").trim()
    val text = json.optString("text").trim()
    return if (title.isNotEmpty() && text.isNotEmpty()) GeneratedStory(title, text) else null
}

internal fun parseAnswer(json: JSONObject): String? = json.optString("answer").trim().ifEmpty { null }
