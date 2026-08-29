package uz.kidzone.app.kidzo

fun interface GeminiCaller {
    fun call(prompt: String, onResult: (String) -> Unit, onError: (String) -> Unit)
}
