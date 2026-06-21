package uz.kidzone.app.kidzo

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class ContentFilter @Throws(JSONException::class) constructor(contentJson: String) {

    private val stories = mutableListOf<ContentItem>()
    private val songs   = mutableListOf<ContentItem>()

    init {
        val root = JSONObject(contentJson)
        parseItems(root.optJSONArray("stories"), stories)
        parseItems(root.optJSONArray("songs"),   songs)
    }

    @Throws(JSONException::class)
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

    /** Top 5 stories (songs removed from app). */
    fun getTop5(): List<ContentItem> {
        val limit = minOf(5, stories.size)
        return stories.subList(0, limit).toList()
    }

    /** Filter stories by keyword — title (uz/ru/en) or category. */
    fun getFiltered(query: String?): List<ContentItem> {
        if (query.isNullOrBlank()) return getTop5()
        val q = query.lowercase().trim()
        val result = stories.filter { matches(it, q) }
        val limit = minOf(5, result.size)
        return result.subList(0, limit)
    }

    fun findById(id: String?): ContentItem? {
        if (id == null) return null
        return stories.find { it.id == id } ?: songs.find { it.id == id }
    }

    /** Format a list of items as a Gemini prompt block. */
    fun toPromptBlock(items: List<ContentItem>): String {
        return items.joinToString("\n") { it.toPromptLine() }
    }

    private fun matches(item: ContentItem, q: String): Boolean {
        return item.titleUz.lowercase().contains(q)
            || item.titleRu.lowercase().contains(q)
            || item.titleEn.lowercase().contains(q)
            || item.category.lowercase().contains(q)
    }

    companion object {
        /** Production: reads from assets/www/content.json */
        @JvmStatic
        @Throws(IOException::class, JSONException::class)
        fun fromAssets(ctx: Context): ContentFilter {
            ctx.assets.open("www/content.json").use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                val sb = StringBuilder()
                var line = reader.readLine()
                while (line != null) {
                    sb.append(line)
                    line = reader.readLine()
                }
                return ContentFilter(sb.toString())
            }
        }
    }
}
