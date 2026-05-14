package uz.kidzone.app.kidzo;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ContentFilterTest {

    private static final String TEST_JSON =
        "{\"stories\":["
        + "{\"id\":\"story-001\",\"category\":\"animals\",\"emoji\":\"🦁\","
        +  "\"title\":{\"uz\":\"Sher va Sichqon\",\"ru\":\"Лев и Мышь\",\"en\":\"Lion and Mouse\"}},"
        + "{\"id\":\"story-002\",\"category\":\"nature\",\"emoji\":\"🌳\","
        +  "\"title\":{\"uz\":\"Sehrli Daraxt\",\"ru\":\"Волшебное Дерево\",\"en\":\"Magic Tree\"}}"
        + "],\"songs\":["
        + "{\"id\":\"song-001\",\"category\":\"lullaby\",\"emoji\":\"🌙\","
        +  "\"title\":{\"uz\":\"Alla\",\"ru\":\"Колыбельная\",\"en\":\"Lullaby\"}}"
        + "]}";

    private ContentFilter filter;

    @Before
    public void setUp() throws JSONException {
        filter = new ContentFilter(TEST_JSON);
    }

    @Test
    public void getTop5_returnsAllThreeItems() {
        assertEquals(3, filter.getTop5().size());
    }

    @Test
    public void getTop5_neverExceedsFive() throws JSONException {
        StringBuilder json = new StringBuilder("{\"stories\":[");
        for (int i = 1; i <= 10; i++) {
            if (i > 1) json.append(",");
            json.append("{\"id\":\"story-00").append(i)
                .append("\",\"category\":\"animals\",\"emoji\":\"🦁\",")
                .append("\"title\":{\"uz\":\"Nom\",\"ru\":\"Nom\",\"en\":\"Name\"}}");
        }
        json.append("],\"songs\":[]}");
        ContentFilter big = new ContentFilter(json.toString());
        assertEquals(5, big.getTop5().size());
    }

    @Test
    public void getFiltered_matchesUzbekTitle() {
        List<ContentItem> result = filter.getFiltered("sher");
        assertEquals(1, result.size());
        assertEquals("story-001", result.get(0).id);
    }

    @Test
    public void getFiltered_matchesCategory() {
        List<ContentItem> result = filter.getFiltered("lullaby");
        assertEquals(1, result.size());
        assertEquals("song-001", result.get(0).id);
    }

    @Test
    public void getFiltered_emptyQuery_returnsTop5() {
        List<ContentItem> result = filter.getFiltered("");
        assertEquals(3, result.size());
    }

    @Test
    public void getFiltered_noMatch_returnsEmptyList() {
        List<ContentItem> result = filter.getFiltered("xxxxxxxxx");
        assertTrue(result.isEmpty());
    }

    @Test
    public void findById_existingId_returnsItem() {
        ContentItem item = filter.findById("song-001");
        assertNotNull(item);
        assertEquals("🌙", item.emoji);
    }

    @Test
    public void findById_missingId_returnsNull() {
        assertNull(filter.findById("story-999"));
    }

    @Test
    public void findById_nullId_returnsNull() {
        assertNull(filter.findById(null));
    }

    @Test
    public void toPromptBlock_formatsCorrectly() {
        List<ContentItem> items = filter.getTop5();
        String block = filter.toPromptBlock(items);
        assertTrue(block.contains("story-001|🦁|Sher va Sichqon|animals"));
        assertTrue(block.contains("song-001|🌙|Alla|lullaby"));
    }

    @Test
    public void constructor_emptyJson_doesNotCrash() throws JSONException {
        ContentFilter empty = new ContentFilter("{\"stories\":[],\"songs\":[]}");
        assertEquals(0, empty.getTop5().size());
    }
}
