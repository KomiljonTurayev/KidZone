package uz.kidzone.shared.kidzo

fun interface GeminiCaller {
    suspend fun call(prompt: String): String
}
