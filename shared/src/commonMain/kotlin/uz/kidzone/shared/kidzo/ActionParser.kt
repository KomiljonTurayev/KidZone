package uz.kidzone.shared.kidzo

object ActionParser {

    private val SINGLE = Regex("\\[OPEN:([a-z0-9\\-]+)\\]")
    private val LINE   = Regex("\\[OPEN:([a-z0-9\\-]+)\\]\\s*(.*)")

    fun parse(text: String?): String? {
        if (text.isNullOrEmpty()) return null
        val match = SINGLE.find(text)
        return match?.groupValues?.get(1)
    }

    fun parseRecommendations(text: String?): List<ContentCard> {
        val result = mutableListOf<ContentCard>()
        if (text.isNullOrEmpty()) return result
        for (line in text.split(Regex("\\r?\\n"))) {
            val trimmedLine = line.trim()
            val match = LINE.find(trimmedLine)
            if (match != null) {
                result.add(ContentCard(match.groupValues[1], match.groupValues[2].trim()))
            }
        }
        return result
    }
}
