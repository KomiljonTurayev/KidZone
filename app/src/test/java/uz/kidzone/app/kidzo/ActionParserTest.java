package uz.kidzone.app.kidzo;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ActionParserTest {

    // --- parse() ---

    @Test
    public void parse_validTag_returnsId() {
        assertEquals("story-003", ActionParser.parse("[OPEN:story-003] Bu qiziqarli ertak!"));
    }

    @Test
    public void parse_tagAtStart_returnsId() {
        assertEquals("song-001", ActionParser.parse("[OPEN:song-001]"));
    }

    @Test
    public void parse_noTag_returnsNull() {
        assertNull(ActionParser.parse("Salom! Bugun nima qilamiz?"));
    }

    @Test
    public void parse_nullInput_returnsNull() {
        assertNull(ActionParser.parse(null));
    }

    @Test
    public void parse_emptyInput_returnsNull() {
        assertNull(ActionParser.parse(""));
    }

    // --- parseRecommendations() ---

    @Test
    public void parseRecommendations_threeLines_returnsThreeCards() {
        String response = "[OPEN:story-003] Toshbaqa va Quyon — juda qiziqarli!\n"
                + "[OPEN:song-001] Alla — uxlash vaqti!\n"
                + "[OPEN:story-005] Sehrli Daraxt — tabiat haqida!";

        List<ContentCard> cards = ActionParser.parseRecommendations(response);

        assertEquals(3, cards.size());
        assertEquals("story-003", cards.get(0).contentId);
        assertEquals("Toshbaqa va Quyon — juda qiziqarli!", cards.get(0).displayText);
        assertEquals("song-001", cards.get(1).contentId);
        assertEquals("story-005", cards.get(2).contentId);
    }

    @Test
    public void parseRecommendations_noTags_returnsEmptyList() {
        List<ContentCard> cards = ActionParser.parseRecommendations("Salom! Keling o'ynaymiz.");
        assertTrue(cards.isEmpty());
    }

    @Test
    public void parseRecommendations_nullInput_returnsEmptyList() {
        assertTrue(ActionParser.parseRecommendations(null).isEmpty());
    }

    @Test
    public void parseRecommendations_mixedLinesWithProse_returnsOnlyTaggedLines() {
        String response = "Salom, bu qiziqarli kontentlar:\n"
                + "[OPEN:story-001] Sher va Sichqon\n"
                + "Yana ko'proq tanlash uchun:\n"
                + "[OPEN:song-001] Alla";

        List<ContentCard> cards = ActionParser.parseRecommendations(response);

        assertEquals(2, cards.size());
        assertEquals("story-001", cards.get(0).contentId);
        assertEquals("song-001", cards.get(1).contentId);
    }
}
