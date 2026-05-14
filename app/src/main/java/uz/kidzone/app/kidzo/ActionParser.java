package uz.kidzone.app.kidzo;

import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionParser {

    private static final Pattern SINGLE =
            Pattern.compile("\\[OPEN:([a-z0-9\\-]+)\\]");

    private static final Pattern LINE =
            Pattern.compile("\\[OPEN:([a-z0-9\\-]+)\\]\\s*(.*)");

    @Nullable
    public static String parse(@Nullable String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher m = SINGLE.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    public static List<ContentCard> parseRecommendations(@Nullable String text) {
        List<ContentCard> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;
        for (String line : text.split("\\r?\\n")) {
            Matcher m = LINE.matcher(line.trim());
            if (m.find()) {
                result.add(new ContentCard(m.group(1), m.group(2).trim()));
            }
        }
        return result;
    }
}
