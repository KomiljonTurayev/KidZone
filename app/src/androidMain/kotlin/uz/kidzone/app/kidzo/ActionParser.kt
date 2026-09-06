package uz.kidzone.app.kidzo

import java.util.regex.Pattern

object ActionParser {

    private val SINGLE: Pattern = Pattern.compile("\\[OPEN:([a-z0-9\\-]+)\\]")
    private val LINE: Pattern   = Pattern.compile("\\[OPEN:([a-z0-9\\-]+)\\]\\s*(.*)")

    @JvmStatic
    fun parse(text: String?): String? {
        if (text.isNullOrEmpty()) return null
        val m = SINGLE.matcher(text)
        return if (m.find()) m.group(1) else null
    }

    @JvmStatic
    fun parseRecommendations(text: String?): List<ContentCard> {
        val result = mutableListOf<ContentCard>()
        if (text.isNullOrEmpty()) return result
        for (line in text.split(Regex("\\r?\\n"))) {
            val m = LINE.matcher(line.trim())
            if (m.find()) {
                result.add(ContentCard(m.group(1)!!, m.group(2)!!.trim()))
            }
        }
        return result
    }
}
