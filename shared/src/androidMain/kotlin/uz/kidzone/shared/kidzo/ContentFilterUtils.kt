package uz.kidzone.shared.kidzo

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

fun ContentFilter.Companion.fromAssets(ctx: Context): ContentFilter {
    ctx.assets.open("www/content.json").use { inputStream ->
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val content = reader.readText()
        return ContentFilter(content)
    }
}
