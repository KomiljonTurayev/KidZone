package uz.kidzone.app.kidzo;

import android.content.Context;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ContentFilter {

    private final List<ContentItem> stories = new ArrayList<>();
    private final List<ContentItem> songs   = new ArrayList<>();

    /** Test and production use — accepts JSON string. */
    public ContentFilter(String contentJson) throws JSONException {
        JSONObject root = new JSONObject(contentJson);
        parseItems(root.optJSONArray("stories"), stories);
        parseItems(root.optJSONArray("songs"),   songs);
    }

    /** Production: reads from assets/www/content.json */
    public static ContentFilter fromAssets(Context ctx) throws IOException, JSONException {
        try (InputStream is = ctx.getAssets().open("www/content.json");
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return new ContentFilter(sb.toString());
        }
    }

    private void parseItems(JSONArray arr, List<ContentItem> out) throws JSONException {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            JSONObject title = o.optJSONObject("title");
            out.add(new ContentItem(
                o.getString("id"),
                o.optString("emoji", ""),
                title != null ? title.optString("uz", "") : "",
                title != null ? title.optString("ru", "") : "",
                title != null ? title.optString("en", "") : "",
                o.optString("category", "")
            ));
        }
    }

    /** Top 5 stories (songs removed from app). */
    public List<ContentItem> getTop5() {
        int limit = Math.min(5, stories.size());
        return new ArrayList<>(stories.subList(0, limit));
    }

    /** Filter stories by keyword — title (uz/ru/en) or category. */
    public List<ContentItem> getFiltered(String query) {
        if (query == null || query.trim().isEmpty()) return getTop5();
        String q = query.toLowerCase().trim();
        List<ContentItem> result = new ArrayList<>();
        for (ContentItem item : stories) { if (matches(item, q)) result.add(item); }
        int limit = Math.min(5, result.size());
        return new ArrayList<>(result.subList(0, limit));
    }

    @Nullable
    public ContentItem findById(@Nullable String id) {
        if (id == null) return null;
        for (ContentItem item : stories) { if (item.id.equals(id)) return item; }
        for (ContentItem item : songs)   { if (item.id.equals(id)) return item; }
        return null;
    }

    /** Format a list of items as a Gemini prompt block. */
    public String toPromptBlock(List<ContentItem> items) {
        StringBuilder sb = new StringBuilder();
        for (ContentItem item : items) sb.append(item.toPromptLine()).append("\n");
        return sb.toString().trim();
    }

    private boolean matches(ContentItem item, String q) {
        return item.titleUz.toLowerCase().contains(q)
            || item.titleRu.toLowerCase().contains(q)
            || item.titleEn.toLowerCase().contains(q)
            || item.category.toLowerCase().contains(q);
    }
}
