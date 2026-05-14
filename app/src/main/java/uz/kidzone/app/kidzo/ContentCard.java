package uz.kidzone.app.kidzo;

public class ContentCard {
    public final String contentId;
    public final String displayText;

    public ContentCard(String contentId, String displayText) {
        this.contentId = contentId;
        this.displayText = displayText;
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
