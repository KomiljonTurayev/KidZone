package uz.kidzone.shared.kidzo

data class ContentItem(
    val id: String,
    val emoji: String,
    val titleUz: String,
    val titleRu: String,
    val titleEn: String,
    val category: String,
) {
    fun getTitle(lang: String): String = when (lang) {
        "uz" -> titleUz
        "ru" -> titleRu
        else -> titleEn
    }

    fun toPromptLine(): String = "$id|$emoji|$titleUz|$category"
}
