package uz.kidzone.app.kidzo

data class ContentCard @JvmOverloads constructor(
    @JvmField val contentId: String,
    @JvmField val displayText: String,
    @JvmField val emoji: String = "🐥",
    @JvmField val type: String = "",
)
