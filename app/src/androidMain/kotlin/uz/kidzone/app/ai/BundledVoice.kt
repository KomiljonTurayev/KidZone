package uz.kidzone.app.ai

import android.content.Context
import java.io.File
import java.security.MessageDigest

/**
 * Uzbek clips recorded ahead of time with Gapiring (tools/gapiring/build_voice.py), looked up by the
 * exact text. They need no network and no account. Kidzo's lines are recorded with the default word
 * in the name slot, so a line with the child's name is spoken with that word instead.
 */
internal object BundledVoice {
    private val DEFAULT_NAMES = listOf("Bolajonim", "do'stim")

    /** A copy of the clip in the cache (the player deletes it when done), or null when the text has none. */
    fun find(context: Context, text: String, childName: String): File? =
        spokenVariants(text, childName).firstNotNullOfOrNull { variant ->
            val key = key(variant)
            runCatching {
                context.assets.open("www/audio/voice/$key.mp3").use { input ->
                    File(context.cacheDir, "kidzo_voice_${System.nanoTime()}.mp3")
                        .also { file -> file.outputStream().use { input.copyTo(it) } }
                }
            }.getOrNull()
        }

    fun spokenVariants(text: String, childName: String): List<String> =
        listOf(text) + if (childName.isBlank()) emptyList() else DEFAULT_NAMES.map { text.replace(childName, it) }

    /** Same rule as key() in build_voice.py. */
    fun key(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.trim().toByteArray())
            .joinToString("") { "%02x".format(it) }.take(16)
}
