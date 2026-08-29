package uz.kidzone.app.kidzo

data class ContentItem(
    @JvmField val id: String,
    @JvmField val emoji: String,
    @JvmField val titleUz: String,
    @JvmField val titleRu: String,
    @JvmField val titleEn: String,
    @JvmField val category: String,
) {
    fun getTitle(lang: String): String = when (lang) {
        "uz" -> titleUz
        "ru" -> titleRu
        else -> titleEn
    }

    fun toPromptLine(): String = "$id|$emoji|$titleUz|$category"
}
