package uz.kidzone.shared.kidzo

data class ContentCard(
    val contentId: String,
    val displayText: String,
    val emoji: String = "🐥",
    val type: String = "",
)
