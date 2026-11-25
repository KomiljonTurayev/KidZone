package uz.kidzone.app.ai

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.kidzone.app.ui.viewmodel.greeting

class BundledVoiceTest {
    private val www = listOf("src/androidMain/assets/www", "app/src/androidMain/assets/www").map(::File).first { it.exists() }

    private fun hasClip(text: String, childName: String = "") =
        BundledVoice.spokenVariants(text, childName).any { File(www, "audio/voice/${BundledVoice.key(it)}.mp3").length() > 0 }

    @Test
    fun `the key is the one build_voice writes`() {
        assertEquals("aaf4c61ddcc5e8a2", BundledVoice.key(" hello\n"))
    }

    @Test
    fun `every Uzbek story has a clip`() {
        val stories = JSONObject(File(www, "content.json").readText()).getJSONArray("stories")
        for (i in 0 until stories.length()) {
            val s = stories.getJSONObject(i)
            assertTrue(s.getString("id"), hasClip(s.getJSONObject("text").getString("uz")))
        }
    }

    @Test
    fun `Kidzo's offline lines have a clip, with and without a name`() {
        val queries = listOf("topishmoq", "osmon", "dino", "ertak", "hazil", "maqtov", "qush", "salom", "???")
        for (name in listOf("", "Aziz")) {
            for (q in queries) repeat(30) { // riddles and generic lines are random
                val line = OfflineKidzoBrain.respond(q, "uz", name)
                assertTrue(line, hasClip(line, name))
            }
            assertTrue(hasClip(greeting("uz"), name))
        }
    }
}
