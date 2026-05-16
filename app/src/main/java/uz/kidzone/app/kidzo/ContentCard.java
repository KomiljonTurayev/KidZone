package uz.kidzone.app.kidzo;

public class ContentCard {
    public final String contentId;
    public final String displayText;
    public final String emoji;
    public final String type;

    public ContentCard(String contentId, String displayText, String emoji, String type) {
        this.contentId   = contentId;
        this.displayText = displayText;
        this.emoji       = emoji;
        this.type        = type;
    }

    /** Backward-compatible 2-arg constructor used by ActionParser. */
    public ContentCard(String contentId, String displayText) {
        this(contentId, displayText, "🐥", "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContentCard)) return false;
        ContentCard that = (ContentCard) o;
        return contentId.equals(that.contentId) && displayText.equals(that.displayText);
    }

    @Override
    public int hashCode() {
        return 31 * contentId.hashCode() + displayText.hashCode();
    }
}
