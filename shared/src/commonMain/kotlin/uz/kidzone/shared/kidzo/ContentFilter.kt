package uz.kidzone.shared.kidzo

import kotlinx.serialization.json.Json

class ContentFilter(contentJson: String) : ContentFilterProvider {

    private val stories: List<ContentItem>

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    init {
        val root = try {
            json.decodeFromString<ContentRoot>(contentJson)
        } catch (e: Exception) {
            ContentRoot()
        }
        
        stories = root.stories?.map { item ->
            ContentItem(
                id = item.id,
                emoji = item.emoji ?: "",
                titleUz = item.title?.uz ?: "",
                titleRu = item.title?.ru ?: "",
                titleEn = item.title?.en ?: "",
                category = item.category ?: ""
            )
        } ?: emptyList()
    }

    override fun getTop5(): List<ContentItem> {
        return stories.take(5)
    }

    override fun getFiltered(query: String): List<ContentItem> {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return getTop5()
        
        return stories.filter {
            it.titleUz.lowercase().contains(q) ||
            it.titleRu.lowercase().contains(q) ||
            it.category.lowercase().contains(q)
        }.take(5)
    }
}
