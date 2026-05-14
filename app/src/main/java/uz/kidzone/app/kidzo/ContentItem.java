package uz.kidzone.app.kidzo;

public class ContentItem {
    public final String id;
    public final String emoji;
    public final String titleUz;
    public final String titleRu;
    public final String titleEn;
    public final String category;

    public ContentItem(String id, String emoji,
                       String titleUz, String titleRu, String titleEn,
                       String category) {
        this.id = id;
        this.emoji = emoji;
        this.titleUz = titleUz;
        this.titleRu = titleRu;
        this.titleEn = titleEn;
        this.category = category;
    }

    public String getTitle(String lang) {
        if ("uz".equals(lang)) return titleUz;
        if ("ru".equals(lang)) return titleRu;
        return titleEn;
    }

    /** Gemini prompt format: "id|emoji|nomUz|kategoriya" */
    public String toPromptLine() {
        return id + "|" + emoji + "|" + titleUz + "|" + category;
    }
}
