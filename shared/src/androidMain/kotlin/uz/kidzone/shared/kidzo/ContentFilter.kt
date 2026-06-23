package uz.kidzone.shared.kidzo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class ContentFilter(contentJson: String) : ContentFilterProvider {

    private val stories = mutableListOf<ContentItem>()

    init {
        val root = JSONObject(contentJson)
        parseItems(root.optJSONArray("stories"), stories)
    }

    private fun parseItems(arr: JSONArray?, out: MutableList<ContentItem>) {
        if (arr == null) return
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val title = o.optJSONObject("title")
            out.add(
                ContentItem(
                    id       = o.getString("id"),
                    emoji    = o.optString("emoji", ""),
                    titleUz  = title?.optString("uz", "") ?: "",
                    titleRu  = title?.optString("ru", "") ?: "",
                    titleEn  = title?.optString("en", "") ?: "",
                    category = o.optString("category", ""),
                )
            )
        }
    }

    override fun getTop5(): List<ContentItem> {
        return stories.take(5)
    }

    override fun getFiltered(query: String): List<ContentItem> {
        val q = query.lowercase().trim()
        return stories.filter { 
            it.titleUz.lowercase().contains(q) || 
            it.titleRu.lowercase().contains(q) || 
            it.category.lowercase().contains(q) 
        }.take(5)
    }

    companion object {
        fun fromAssets(ctx: Context): ContentFilter {
            ctx.assets.open("www/content.json").use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                val content = reader.readText()
                return ContentFilter(content)
            }
        }
    }
}
