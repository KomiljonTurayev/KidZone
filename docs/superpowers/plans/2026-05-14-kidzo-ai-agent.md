# Kidzo AI Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** KidZone ilovasiga Kidzo AI agentini qo'shish — FAB → BottomSheet orqali personalizatsiyalangan kontent tavsiyalari va chat (Gemini 1.5 Flash + Android TTS).

**Architecture:** State machine pattern (IDLE → THINKING → RECOMMENDATIONS → CHATTING → ERROR). KidzoAgent bosh orkestrator, ContentFilter lokal filtrlash, ActionParser `[OPEN:id]` teglari uchun. Java prototip → Kotlin/Compose migratsiyaga 1:1 tayyor.

**Tech Stack:** Java 17, Android API 26+, Gemini 0.9.0 (allaqachon bog'liq), RecyclerView, BottomSheetDialogFragment, SharedPreferences, JUnit 4, Mockito 5.

---

## Fayl Xaritasi

| Amal | Fayl |
|------|------|
| Modify | `app/build.gradle` — test deps + RecyclerView + BuildConfig |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/KidzoState.java` |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/KidzoStateListener.java` |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/ContentCard.java` |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/GeminiCaller.java` |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/MainThreadRunner.java` |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/ActionParser.java` |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/ContentItem.java` |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/ContentFilter.java` |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/KidzoProfile.java` |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/KidzoAgent.java` |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/RealGeminiCaller.java` |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/KidzoCardAdapter.java` |
| Create | `app/src/main/java/uz/kidzone/app/kidzo/KidzoBottomSheet.java` |
| Create | `app/src/main/res/layout/bottom_sheet_kidzo.xml` |
| Create | `app/src/main/res/layout/item_kidzo_card.xml` |
| Modify | `app/src/main/java/uz/kidzone/app/MainActivity.java` — setupKidzo() |
| Modify | `app/src/main/res/values/strings.xml` — Kidzo string resurslari |
| Create | `app/src/test/java/uz/kidzone/app/kidzo/ActionParserTest.java` |
| Create | `app/src/test/java/uz/kidzone/app/kidzo/ContentFilterTest.java` |
| Create | `app/src/test/java/uz/kidzone/app/kidzo/KidzoProfileTest.java` |
| Create | `app/src/test/java/uz/kidzone/app/kidzo/KidzoAgentStateTest.java` |

---

## Task 1: Build Setup

**Files:**
- Modify: `app/build.gradle`
- Modify: `local.properties`

- [ ] **Step 1: Test va RecyclerView dependencylarini qo'shish**

`app/build.gradle`ning `dependencies {}` bloкiga quyidagilarni qo'shing (mavjud qatorlarni o'zgartirmang):

```gradle
    // RecyclerView (KidzoCardAdapter uchun)
    implementation 'androidx.recyclerview:recyclerview:1.3.2'

    // Test dependencies
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:5.4.0'
    testImplementation 'org.json:json:20231013'
```

- [ ] **Step 2: BuildConfig uchun Gemini API kalitini sozlash**

`app/build.gradle`ning `android { defaultConfig { ... } }` ichiga qo'shing:

```gradle
        buildConfigField "String", "GEMINI_API_KEY", "\"${project.findProperty('GEMINI_API_KEY') ?: ''}\""
```

`android {}` blokining yuqorisiga (boshqa `plugins` va `android` bloklaridan keyin) qo'shing:

```gradle
    buildFeatures {
        buildConfig true
    }
```

- [ ] **Step 3: API kalitini local.properties'ga yozish**

`local.properties` faylini oching (`.gitignore`da allaqachon bor), quyidagi qatorni qo'shing:

```
GEMINI_API_KEY=REMOVED_SECRET
```

- [ ] **Step 4: Sync va tekshirish**

```bash
./gradlew assembleDebug
```

Kutilgan natija: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle local.properties
git commit -m "build: add RecyclerView, test deps, BuildConfig for Gemini key"
```

---

## Task 2: ActionParser (TDD)

**Files:**
- Create: `app/src/test/java/uz/kidzone/app/kidzo/ActionParserTest.java`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/ActionParser.java`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/ContentCard.java`

- [ ] **Step 1: ContentCard data klassini yozish**

`app/src/main/java/uz/kidzone/app/kidzo/ContentCard.java`:

```java
package uz.kidzone.app.kidzo;

public class ContentCard {
    public final String contentId;
    public final String displayText;

    public ContentCard(String contentId, String displayText) {
        this.contentId = contentId;
        this.displayText = displayText;
    }
}
```

- [ ] **Step 2: Muvaffaqiyatsiz testlarni yozish**

`app/src/test/java/uz/kidzone/app/kidzo/ActionParserTest.java`:

```java
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
}
```

- [ ] **Step 3: Testlarni ishga tushirib, muvaffaqiyatsiz ekanini tasdiqlash**

```bash
./gradlew test --tests "uz.kidzone.app.kidzo.ActionParserTest"
```

Kutilgan natija: `FAILED` — `ActionParser` hali mavjud emas.

- [ ] **Step 4: ActionParser implementatsiyasini yozish**

`app/src/main/java/uz/kidzone/app/kidzo/ActionParser.java`:

```java
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
        for (String line : text.split("\n")) {
            Matcher m = LINE.matcher(line.trim());
            if (m.find()) {
                result.add(new ContentCard(m.group(1), m.group(2).trim()));
            }
        }
        return result;
    }
}
```

- [ ] **Step 5: Testlarni ishga tushirib, o'tishini tasdiqlash**

```bash
./gradlew test --tests "uz.kidzone.app.kidzo.ActionParserTest"
```

Kutilgan natija: `BUILD SUCCESSFUL` — barcha 8 test `PASSED`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/kidzo/ContentCard.java \
        app/src/main/java/uz/kidzone/app/kidzo/ActionParser.java \
        app/src/test/java/uz/kidzone/app/kidzo/ActionParserTest.java
git commit -m "feat(kidzo): ActionParser — [OPEN:id] tag parsing, TDD"
```

---

## Task 3: ContentItem + ContentFilter (TDD)

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/kidzo/ContentItem.java`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/ContentFilter.java`
- Create: `app/src/test/java/uz/kidzone/app/kidzo/ContentFilterTest.java`

- [ ] **Step 1: ContentItem yozish**

`app/src/main/java/uz/kidzone/app/kidzo/ContentItem.java`:

```java
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

    /** Gemini prompt ichiga qo'yiladigan format: "id|emoji|nomUz|kategoriya" */
    public String toPromptLine() {
        return id + "|" + emoji + "|" + titleUz + "|" + category;
    }
}
```

- [ ] **Step 2: Muvaffaqiyatsiz testlarni yozish**

`app/src/test/java/uz/kidzone/app/kidzo/ContentFilterTest.java`:

```java
package uz.kidzone.app.kidzo;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ContentFilterTest {

    // Minimal test JSON: 2 ta ertak (animals, nature) + 1 ta qo'shiq (lullaby)
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
        // 10 ta element bo'lsa ham max 5
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
```

- [ ] **Step 3: Testlarni ishga tushirib, muvaffaqiyatsiz ekanini tasdiqlash**

```bash
./gradlew test --tests "uz.kidzone.app.kidzo.ContentFilterTest"
```

Kutilgan natija: `FAILED` — `ContentFilter` hali mavjud emas.

- [ ] **Step 4: ContentFilter implementatsiyasini yozish**

`app/src/main/java/uz/kidzone/app/kidzo/ContentFilter.java`:

```java
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

    /** Test va production'da ishlatish uchun — JSON string qabul qiladi. */
    public ContentFilter(String contentJson) throws JSONException {
        JSONObject root = new JSONObject(contentJson);
        parseItems(root.optJSONArray("stories"), stories);
        parseItems(root.optJSONArray("songs"),   songs);
    }

    /** Production: assets/www/content.json'dan o'qish. */
    public static ContentFilter fromAssets(Context ctx) throws IOException, JSONException {
        InputStream is = ctx.getAssets().open("www/content.json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return new ContentFilter(sb.toString());
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

    /** Oxirgi 5 ta populyar kontent (stories birinchi). */
    public List<ContentItem> getTop5() {
        List<ContentItem> all = new ArrayList<>(stories);
        all.addAll(songs);
        return all.subList(0, Math.min(5, all.size()));
    }

    /** Kalit so'z bo'yicha filter — title (uz/ru/en) yoki kategoriya bo'yicha. */
    public List<ContentItem> getFiltered(String query) {
        if (query == null || query.trim().isEmpty()) return getTop5();
        String q = query.toLowerCase().trim();
        List<ContentItem> result = new ArrayList<>();
        for (ContentItem item : stories) { if (matches(item, q)) result.add(item); }
        for (ContentItem item : songs)   { if (matches(item, q)) result.add(item); }
        return result.subList(0, Math.min(5, result.size()));
    }

    @Nullable
    public ContentItem findById(String id) {
        for (ContentItem item : stories) { if (item.id.equals(id)) return item; }
        for (ContentItem item : songs)   { if (item.id.equals(id)) return item; }
        return null;
    }

    /** Gemini prompti uchun formatlanган blok. */
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
```

- [ ] **Step 5: Testlarni ishga tushirib, o'tishini tasdiqlash**

```bash
./gradlew test --tests "uz.kidzone.app.kidzo.ContentFilterTest"
```

Kutilgan natija: `BUILD SUCCESSFUL` — barcha 10 test `PASSED`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/kidzo/ContentItem.java \
        app/src/main/java/uz/kidzone/app/kidzo/ContentFilter.java \
        app/src/test/java/uz/kidzone/app/kidzo/ContentFilterTest.java
git commit -m "feat(kidzo): ContentItem + ContentFilter — JSON parsing, filter, TDD"
```

---

## Task 4: Data Turlari (State, Listener, Interfaces)

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/kidzo/KidzoState.java`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/KidzoStateListener.java`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/GeminiCaller.java`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/MainThreadRunner.java`

Bu fayllar sof ma'lumot turlari va interfeyslari — alohida unit test talab qilmaydi.

- [ ] **Step 1: KidzoState enum yozish**

`app/src/main/java/uz/kidzone/app/kidzo/KidzoState.java`:

```java
package uz.kidzone.app.kidzo;

public enum KidzoState {
    IDLE,
    THINKING,
    RECOMMENDATIONS,
    CHATTING,
    ERROR
}
```

- [ ] **Step 2: KidzoStateListener interfeysi yozish**

`app/src/main/java/uz/kidzone/app/kidzo/KidzoStateListener.java`:

```java
package uz.kidzone.app.kidzo;

public interface KidzoStateListener {
    /**
     * State o'zgarganda chaqiriladi.
     * @param newState yangi holat
     * @param payload RECOMMENDATIONS → List<ContentCard>, CHATTING → String (javob matni),
     *                ERROR → String (xabar), boshqalarda null
     */
    void onStateChanged(KidzoState newState, Object payload);

    /**
     * Bola kontent ochishni so'raganda chaqiriladi.
     * @param contentId masalan "story-003" yoki "song-001"
     */
    void onActionRequested(String contentId);
}
```

- [ ] **Step 3: GeminiCaller interfeysi yozish (test inject uchun)**

`app/src/main/java/uz/kidzone/app/kidzo/GeminiCaller.java`:

```java
package uz.kidzone.app.kidzo;

public interface GeminiCaller {
    void call(String prompt, OnSuccess onSuccess, OnError onError);

    interface OnSuccess { void accept(String text); }
    interface OnError   { void accept(String message); }
}
```

- [ ] **Step 4: MainThreadRunner interfeysi yozish (test inject uchun)**

`app/src/main/java/uz/kidzone/app/kidzo/MainThreadRunner.java`:

```java
package uz.kidzone.app.kidzo;

public interface MainThreadRunner {
    void run(Runnable r);
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/kidzo/KidzoState.java \
        app/src/main/java/uz/kidzone/app/kidzo/KidzoStateListener.java \
        app/src/main/java/uz/kidzone/app/kidzo/GeminiCaller.java \
        app/src/main/java/uz/kidzone/app/kidzo/MainThreadRunner.java
git commit -m "feat(kidzo): KidzoState, KidzoStateListener, GeminiCaller, MainThreadRunner"
```

---

## Task 5: KidzoAgent Yadro (TDD — State Transitions)

**Files:**
- Create: `app/src/test/java/uz/kidzone/app/kidzo/KidzoAgentStateTest.java`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/KidzoAgent.java`

- [ ] **Step 1: Muvaffaqiyatsiz testlarni yozish**

`app/src/test/java/uz/kidzone/app/kidzo/KidzoAgentStateTest.java`:

```java
package uz.kidzone.app.kidzo;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KidzoAgentStateTest {

    private static final String TEST_JSON =
        "{\"stories\":["
        + "{\"id\":\"story-001\",\"category\":\"animals\",\"emoji\":\"🦁\","
        +  "\"title\":{\"uz\":\"Sher va Sichqon\",\"ru\":\"Лев и Мышь\",\"en\":\"Lion\"}}],"
        + "\"songs\":["
        + "{\"id\":\"song-001\",\"category\":\"lullaby\",\"emoji\":\"🌙\","
        +  "\"title\":{\"uz\":\"Alla\",\"ru\":\"Колыбельная\",\"en\":\"Lullaby\"}}]}";

    @Mock KidzoStateListener listener;

    private ContentFilter contentFilter;
    // Synchronous dispatcher — tests run without Android Looper
    private final MainThreadRunner syncRunner = Runnable::run;

    @Before
    public void setUp() throws JSONException {
        MockitoAnnotations.openMocks(this);
        contentFilter = new ContentFilter(TEST_JSON);
    }

    @Test
    public void initialState_isIdle() {
        KidzoAgent agent = new KidzoAgent(contentFilter, (p, ok, err) -> {}, syncRunner);
        assertEquals(KidzoState.IDLE, agent.getCurrentState());
    }

    @Test
    public void requestRecommendations_setsThinkingThenRecommendations() {
        String fakeResponse =
            "[OPEN:story-001] Sher va Sichqon — hayvonlar haqida!\n"
          + "[OPEN:song-001] Alla — uxlash vaqti!";

        KidzoAgent agent = new KidzoAgent(
            contentFilter,
            (prompt, onSuccess, onError) -> onSuccess.accept(fakeResponse),
            syncRunner
        );
        agent.setListener(listener);

        agent.requestRecommendations();

        // THINKING, so'ng RECOMMENDATIONS chaqirilgan bo'lishi kerak
        ArgumentCaptor<KidzoState> stateCaptor = ArgumentCaptor.forClass(KidzoState.class);
        verify(listener, atLeast(2)).onStateChanged(stateCaptor.capture(), any());
        List<KidzoState> states = stateCaptor.getAllValues();
        assertTrue(states.contains(KidzoState.THINKING));
        assertTrue(states.contains(KidzoState.RECOMMENDATIONS));
        assertEquals(KidzoState.RECOMMENDATIONS, agent.getCurrentState());
    }

    @Test
    public void requestRecommendations_onGeminiError_setsErrorState() {
        KidzoAgent agent = new KidzoAgent(
            contentFilter,
            (prompt, onSuccess, onError) -> onError.accept("Tarmoq xatosi"),
            syncRunner
        );
        agent.setListener(listener);

        agent.requestRecommendations();

        verify(listener).onStateChanged(eq(KidzoState.ERROR), eq("Tarmoq xatosi"));
        assertEquals(KidzoState.ERROR, agent.getCurrentState());
    }

    @Test
    public void requestRecommendations_emptyGeminiResponse_usesFallback() {
        KidzoAgent agent = new KidzoAgent(
            contentFilter,
            (prompt, onSuccess, onError) -> onSuccess.accept(""),
            syncRunner
        );
        agent.setListener(listener);

        agent.requestRecommendations();

        // Bo'sh javobda RECOMMENDATIONS + fallback kartalar
        verify(listener).onStateChanged(eq(KidzoState.RECOMMENDATIONS), any());
    }

    @Test
    public void dismiss_fromThinkingState_setsIdleState() {
        KidzoAgent agent = new KidzoAgent(
            contentFilter,
            (prompt, onSuccess, onError) -> { /* qo'ng'iroq qilmaydi */ },
            syncRunner
        );
        agent.setListener(listener);
        agent.requestRecommendations(); // → THINKING

        agent.dismiss();

        verify(listener).onStateChanged(eq(KidzoState.IDLE), isNull());
        assertEquals(KidzoState.IDLE, agent.getCurrentState());
    }

    @Test
    public void dismiss_fromIdleState_remainsIdle() {
        KidzoAgent agent = new KidzoAgent(contentFilter, (p, ok, err) -> {}, syncRunner);
        agent.setListener(listener);

        agent.dismiss();

        assertEquals(KidzoState.IDLE, agent.getCurrentState());
    }

    @Test
    public void actionRequested_parsedFromGeminiResponse() {
        String fakeResponse = "[OPEN:story-001] Sher ertagi!";
        KidzoAgent agent = new KidzoAgent(
            contentFilter,
            (prompt, onSuccess, onError) -> onSuccess.accept(fakeResponse),
            syncRunner
        );
        agent.setListener(listener);

        agent.requestRecommendations();
        // Karta bosilganda actionRequested chaqiriladi
        agent.openContent("story-001");

        verify(listener).onActionRequested("story-001");
    }
}
```

- [ ] **Step 2: Testlarni ishga tushirib, muvaffaqiyatsiz ekanini tasdiqlash**

```bash
./gradlew test --tests "uz.kidzone.app.kidzo.KidzoAgentStateTest"
```

Kutilgan natija: `FAILED` — `KidzoAgent` hali mavjud emas.

- [ ] **Step 3: KidzoAgent implementatsiyasini yozish**

`app/src/main/java/uz/kidzone/app/kidzo/KidzoAgent.java`:

```java
package uz.kidzone.app.kidzo;

import androidx.annotation.Nullable;
import java.util.List;

public class KidzoAgent {

    private KidzoState currentState = KidzoState.IDLE;
    private final ContentFilter contentFilter;
    private final GeminiCaller geminiCaller;
    private final MainThreadRunner mainThreadRunner;
    private @Nullable KidzoStateListener listener;

    /** Test konstruktori — GeminiCaller va MainThreadRunner inject qilinadi. */
    KidzoAgent(ContentFilter contentFilter,
               GeminiCaller geminiCaller,
               MainThreadRunner mainThreadRunner) {
        this.contentFilter   = contentFilter;
        this.geminiCaller    = geminiCaller;
        this.mainThreadRunner = mainThreadRunner;
    }

    public void setListener(@Nullable KidzoStateListener listener) {
        this.listener = listener;
    }

    public KidzoState getCurrentState() { return currentState; }

    /** FAB bosilganda: IDLE → THINKING → RECOMMENDATIONS */
    public void requestRecommendations() {
        setState(KidzoState.THINKING, null);
        List<ContentItem> top5 = contentFilter.getTop5();
        String promptBlock = contentFilter.toPromptBlock(top5);
        String prompt = buildRecommendationPrompt("Bolam", null, promptBlock);

        geminiCaller.call(prompt,
            text -> {
                List<ContentCard> cards = ActionParser.parseRecommendations(text);
                if (cards.isEmpty()) {
                    // Fallback: ContentFilter'dan to'g'ridan-to'g'ri kartalar
                    for (ContentItem item : top5) {
                        cards.add(new ContentCard(item.id, item.titleUz));
                    }
                }
                setState(KidzoState.RECOMMENDATIONS, cards);
            },
            errorMsg -> setState(KidzoState.ERROR, errorMsg)
        );
    }

    /** Karta yoki chat orqali kontent ochish. */
    public void openContent(String contentId) {
        if (listener != null) {
            mainThreadRunner.run(() -> listener.onActionRequested(contentId));
        }
    }

    /**
     * P1/P2 kompilatsiyasi uchun stub. Task 12 da to'liq implement qilinadi.
     * RECOMMENDATIONS → CHATTING
     */
    public void startChat() {
        setState(KidzoState.CHATTING, "Salom! Men Kidzo. Nima haqida gaplashamiz? 🐥");
    }

    /**
     * P1/P2 kompilatsiyasi uchun stub. Task 12 da to'liq implement qilinadi.
     */
    public void sendChatMessage(String userMessage) {
        setState(KidzoState.THINKING, null);
        geminiCaller.call(
            "Sen Kidzo. Qisqa javob ber: " + userMessage,
            text -> setState(KidzoState.CHATTING, text),
            err  -> setState(KidzoState.ERROR, err)
        );
    }

    /** Har qanday holatdan IDLE'ga qaytish. */
    public void dismiss() {
        setState(KidzoState.IDLE, null);
    }

    private void setState(KidzoState newState, @Nullable Object payload) {
        currentState = newState;
        if (listener != null) {
            mainThreadRunner.run(() -> listener.onStateChanged(newState, payload));
        }
    }

    private String buildRecommendationPrompt(String childName,
                                              @Nullable String lastContentId,
                                              String contentBlock) {
        return "Sen KidZone ilovasidagi \"Kidzo\" nomli mehribon qushchasan.\n"
             + "Faqat O'zbek tilida, qisqa va bolalarga mos tarzda gaplash.\n"
             + "Bolaning ismi: " + childName + ".\n"
             + (lastContentId != null ? "Oxirgi eshitgan kontenti: " + lastContentId + ".\n" : "")
             + "\nQuyidagi kontentlardan " + childName + " uchun 3 ta mos tavsiya tanlaydi:\n"
             + contentBlock + "\n"
             + "// Har qator formati: \"id|emoji|nomUz|kategoriya\"\n"
             + "\nHar bir tavsiyani quyidagi formatda yoz:\n"
             + "[OPEN:content-id] Kontent nomi — qisqa tavsif\n"
             + "\nBoshqa format ishlatma. Faqat ro'yxatdagi ID'larni ishlat.";
    }
}
```

- [ ] **Step 4: Testlarni ishga tushirib, o'tishini tasdiqlash**

```bash
./gradlew test --tests "uz.kidzone.app.kidzo.KidzoAgentStateTest"
```

Kutilgan natija: `BUILD SUCCESSFUL` — barcha 7 test `PASSED`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/kidzo/KidzoAgent.java \
        app/src/test/java/uz/kidzone/app/kidzo/KidzoAgentStateTest.java
git commit -m "feat(kidzo): KidzoAgent core — state machine, TDD"
```

---

## Task 6: XML Layoutlar

**Files:**
- Create: `app/src/main/res/layout/bottom_sheet_kidzo.xml`
- Create: `app/src/main/res/layout/item_kidzo_card.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: String resurslarini qo'shish**

`app/src/main/res/values/strings.xml`ga quyidagi qatorlarni qo'shing (mavjud `</resources>` oldiga):

```xml
    <string name="kidzo_thinking">Kidzo o\'ylamoqda… 🐥</string>
    <string name="kidzo_recommendations_header">Bugun siz uchun:</string>
    <string name="kidzo_start_chat">💬 Kidzo bilan gaplash</string>
    <string name="kidzo_chat_hint">Kidzo bilan gaplash…</string>
    <string name="kidzo_send">Yuborish</string>
    <string name="kidzo_retry">Qayta urining</string>
    <string name="kidzo_close">Yopish</string>
    <string name="kidzo_error_network">Internet yo\'q. Qaytadan urining 🐥</string>
    <string name="kidzo_error_api">Kidzo biroz charchadi 😴 Keyinroq urinib ko\'ring</string>
```

- [ ] **Step 2: BottomSheet layoutini yozish**

`app/src/main/res/layout/bottom_sheet_kidzo.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@android:color/white"
    android:paddingBottom="32dp">

    <!-- Drag handle -->
    <View
        android:layout_width="40dp"
        android:layout_height="4dp"
        android:layout_gravity="center_horizontal"
        android:layout_marginTop="12dp"
        android:layout_marginBottom="8dp"
        android:background="#CCCCCC"
        android:backgroundTint="#CCCCCC"/>

    <!-- THINKING state -->
    <LinearLayout
        android:id="@+id/layout_thinking"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="32dp"
        android:visibility="visible">

        <ProgressBar
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:indeterminateTint="#FFEB3B"/>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="@string/kidzo_thinking"
            android:textSize="18sp"
            android:textColor="#5D4037"/>
    </LinearLayout>

    <!-- RECOMMENDATIONS state -->
    <LinearLayout
        android:id="@+id/layout_recommendations"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp"
        android:visibility="gone">

        <TextView
            android:id="@+id/tv_recommendations_header"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/kidzo_recommendations_header"
            android:textSize="18sp"
            android:textStyle="bold"
            android:textColor="#3E2723"
            android:layout_marginBottom="12dp"/>

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rv_kidzo_cards"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:nestedScrollingEnabled="false"/>

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_start_chat"
            android:layout_width="match_parent"
            android:layout_height="56dp"
            android:layout_marginTop="16dp"
            android:text="@string/kidzo_start_chat"
            android:textSize="16sp"
            app:cornerRadius="20dp"
            app:backgroundTint="#FFF9C4"/>
    </LinearLayout>

    <!-- CHATTING state -->
    <LinearLayout
        android:id="@+id/layout_chat"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp"
        android:visibility="gone">

        <ScrollView
            android:id="@+id/scroll_chat"
            android:layout_width="match_parent"
            android:layout_height="280dp">

            <LinearLayout
                android:id="@+id/chat_messages"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="8dp"/>
        </ScrollView>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="8dp">

            <EditText
                android:id="@+id/et_chat_input"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:hint="@string/kidzo_chat_hint"
                android:inputType="text"
                android:maxLines="3"
                android:background="@android:color/transparent"
                android:textSize="15sp"/>

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_send"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/kidzo_send"
                android:textSize="14sp"
                app:cornerRadius="20dp"
                app:backgroundTint="#FFEB3B"/>
        </LinearLayout>
    </LinearLayout>

    <!-- ERROR state -->
    <LinearLayout
        android:id="@+id/layout_error"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="32dp"
        android:visibility="gone">

        <TextView
            android:id="@+id/tv_error_message"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="16sp"
            android:textColor="#5D4037"
            android:gravity="center"
            android:layout_marginBottom="16dp"/>

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal">

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_retry"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginEnd="8dp"
                android:text="@string/kidzo_retry"
                app:cornerRadius="20dp"
                app:backgroundTint="#C8E6C9"/>

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btn_error_close"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/kidzo_close"
                app:cornerRadius="20dp"
                app:backgroundTint="#FFCCBC"/>
        </LinearLayout>
    </LinearLayout>

</LinearLayout>
```

- [ ] **Step 3: Karta item layoutini yozish**

`app/src/main/res/layout/item_kidzo_card.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="10dp"
    app:cardCornerRadius="16dp"
    app:cardElevation="4dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="14dp"
        android:gravity="center_vertical">

        <TextView
            android:id="@+id/tv_card_emoji"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:gravity="center"
            android:textSize="26sp"/>

        <TextView
            android:id="@+id/tv_card_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="10dp"
            android:textSize="15sp"
            android:textColor="#3E2723"
            android:textStyle="bold"/>

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_card_play"
            style="@style/Widget.Material3.Button.IconButton"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:text="▶"
            android:textSize="16sp"
            app:backgroundTint="#FFEB3B"/>

    </LinearLayout>
</androidx.cardview.widget.CardView>
```

- [ ] **Step 4: Tekshirish — build muvaffaqiyatli o'tishini**

```bash
./gradlew assembleDebug
```

Kutilgan natija: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/bottom_sheet_kidzo.xml \
        app/src/main/res/layout/item_kidzo_card.xml \
        app/src/main/res/values/strings.xml
git commit -m "feat(kidzo): bottom sheet + card layouts, string resources"
```

---

## Task 7: KidzoBottomSheet (THINKING holati)

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/kidzo/KidzoBottomSheet.java`

- [ ] **Step 1: KidzoBottomSheet yozish (faqat THINKING holati aktiv)**

`app/src/main/java/uz/kidzone/app/kidzo/KidzoBottomSheet.java`:

```java
package uz.kidzone.app.kidzo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import uz.kidzone.app.R;
import java.util.List;

public class KidzoBottomSheet extends BottomSheetDialogFragment
        implements KidzoStateListener {

    private final KidzoAgent agent;

    // Views
    private LinearLayout layoutThinking;
    private LinearLayout layoutRecommendations;
    private LinearLayout layoutChat;
    private LinearLayout layoutError;
    private RecyclerView rvCards;
    private LinearLayout chatMessages;
    private EditText etChatInput;
    private TextView tvErrorMessage;

    public KidzoBottomSheet(KidzoAgent agent) {
        this.agent = agent;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_kidzo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutThinking        = view.findViewById(R.id.layout_thinking);
        layoutRecommendations = view.findViewById(R.id.layout_recommendations);
        layoutChat            = view.findViewById(R.id.layout_chat);
        layoutError           = view.findViewById(R.id.layout_error);
        rvCards               = view.findViewById(R.id.rv_kidzo_cards);
        chatMessages          = view.findViewById(R.id.chat_messages);
        etChatInput           = view.findViewById(R.id.et_chat_input);
        tvErrorMessage        = view.findViewById(R.id.tv_error_message);

        rvCards.setLayoutManager(new LinearLayoutManager(requireContext()));

        view.findViewById(R.id.btn_start_chat).setOnClickListener(v -> {
            agent.startChat();
        });

        view.findViewById(R.id.btn_send).setOnClickListener(v -> {
            String msg = etChatInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                addChatBubble(msg, true);
                etChatInput.setText("");
                agent.sendChatMessage(msg);
            }
        });

        view.findViewById(R.id.btn_retry).setOnClickListener(v ->
            agent.requestRecommendations()
        );

        view.findViewById(R.id.btn_error_close).setOnClickListener(v -> dismiss());

        agent.setListener(this);
        // Agar agent allaqachon THINKING yoki RECOMMENDATIONS holatida bo'lsa, mos UI ko'rsatamiz
        onStateChanged(agent.getCurrentState(), null);
    }

    @Override
    public void onDestroyView() {
        agent.setListener(null);
        super.onDestroyView();
    }

    @Override
    public void onCancel(@NonNull android.content.DialogInterface dialog) {
        agent.dismiss();
        super.onCancel(dialog);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onStateChanged(KidzoState newState, Object payload) {
        if (getView() == null) return;
        requireActivity().runOnUiThread(() -> {
            layoutThinking.setVisibility(View.GONE);
            layoutRecommendations.setVisibility(View.GONE);
            layoutChat.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);

            switch (newState) {
                case THINKING:
                    layoutThinking.setVisibility(View.VISIBLE);
                    break;

                case RECOMMENDATIONS:
                    layoutRecommendations.setVisibility(View.VISIBLE);
                    if (payload instanceof List) {
                        List<ContentCard> cards = (List<ContentCard>) payload;
                        rvCards.setAdapter(new KidzoCardAdapter(cards, contentId -> {
                            agent.openContent(contentId);
                            dismiss();
                        }));
                    }
                    break;

                case CHATTING:
                    layoutChat.setVisibility(View.VISIBLE);
                    if (payload instanceof String) {
                        addChatBubble((String) payload, false);
                    }
                    break;

                case ERROR:
                    layoutError.setVisibility(View.VISIBLE);
                    if (payload instanceof String) {
                        tvErrorMessage.setText((String) payload);
                    }
                    break;

                case IDLE:
                    dismiss();
                    break;
            }
        });
    }

    @Override
    public void onActionRequested(String contentId) {
        // MainActivity tomonidan boshqariladi — bu yerda hech narsa qilmaymiz
    }

    private void addChatBubble(String text, boolean isUser) {
        TextView bubble = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 8;
        if (isUser) {
            params.gravity = android.view.Gravity.END;
            bubble.setBackgroundResource(android.R.color.holo_blue_light);
        } else {
            params.gravity = android.view.Gravity.START;
            bubble.setBackgroundResource(android.R.color.holo_green_light);
        }
        bubble.setLayoutParams(params);
        bubble.setPadding(16, 10, 16, 10);
        bubble.setText(text);
        bubble.setTextSize(15f);
        bubble.setTextColor(android.graphics.Color.BLACK);
        chatMessages.addView(bubble);
    }
}
```

- [ ] **Step 2: KidzoCardAdapter (stub) yozish — Task 11'da to'ldiriladi**

`app/src/main/java/uz/kidzone/app/kidzo/KidzoCardAdapter.java`:

```java
package uz.kidzone.app.kidzo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import uz.kidzone.app.R;
import java.util.List;

public class KidzoCardAdapter extends RecyclerView.Adapter<KidzoCardAdapter.VH> {

    public interface OnCardClick { void onClick(String contentId); }

    private final List<ContentCard> cards;
    private final OnCardClick onCardClick;

    public KidzoCardAdapter(List<ContentCard> cards, OnCardClick onCardClick) {
        this.cards = cards;
        this.onCardClick = onCardClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_kidzo_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ContentCard card = cards.get(position);
        holder.tvTitle.setText(card.displayText);
        holder.tvEmoji.setText("🐥"); // Task 11'da ContentFilter lookup bilan almashtiriladi
        holder.btnPlay.setOnClickListener(v -> onCardClick.onClick(card.contentId));
        holder.itemView.setOnClickListener(v -> onCardClick.onClick(card.contentId));
    }

    @Override
    public int getItemCount() { return cards.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvTitle;
        View btnPlay;
        VH(@NonNull View v) {
            super(v);
            tvEmoji = v.findViewById(R.id.tv_card_emoji);
            tvTitle = v.findViewById(R.id.tv_card_title);
            btnPlay = v.findViewById(R.id.btn_card_play);
        }
    }
}
```

- [ ] **Step 3: Build tekshirish**

```bash
./gradlew assembleDebug
```

Kutilgan natija: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/kidzo/KidzoBottomSheet.java \
        app/src/main/java/uz/kidzone/app/kidzo/KidzoCardAdapter.java
git commit -m "feat(kidzo): KidzoBottomSheet + KidzoCardAdapter stub"
```

---

## Task 8: MainActivity Integration — P1 Yakunlash

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/kidzo/RealGeminiCaller.java`
- Modify: `app/src/main/java/uz/kidzone/app/MainActivity.java`

- [ ] **Step 1: RealGeminiCaller yozish**

`app/src/main/java/uz/kidzone/app/kidzo/RealGeminiCaller.java`:

```java
package uz.kidzone.app.kidzo;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import androidx.annotation.NonNull;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import uz.kidzone.app.BuildConfig;

public class RealGeminiCaller implements GeminiCaller {

    private final GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public RealGeminiCaller() {
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY);
        this.model = GenerativeModelFutures.from(gm);
    }

    @Override
    public void call(String prompt, OnSuccess onSuccess, OnError onError) {
        Content content = new Content.Builder().addText(prompt).build();
        Futures.addCallback(
            model.generateContent(content),
            new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String text = result.getText();
                    onSuccess.accept(text != null ? text : "");
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    onError.accept(t.getMessage() != null ? t.getMessage() : "Noma'lum xato");
                }
            },
            executor
        );
    }
}
```

- [ ] **Step 2: MainActivity'ni yangilash**

`app/src/main/java/uz/kidzone/app/MainActivity.java`da quyidagi o'zgartirishlarni kiriting:

**2a.** Import qatorlarini qo'shing (mavjud importlar ostiga):

```java
import android.os.Handler;
import android.os.Looper;
import uz.kidzone.app.kidzo.ContentFilter;
import uz.kidzone.app.kidzo.KidzoAgent;
import uz.kidzone.app.kidzo.KidzoBottomSheet;
import uz.kidzone.app.kidzo.KidzoState;
import uz.kidzone.app.kidzo.KidzoStateListener;
import uz.kidzone.app.kidzo.RealGeminiCaller;
```

**2b.** Mavjud `private BirdAiManager birdAiManager;` qatoridan keyin yangi maydon qo'shing:

```java
    private KidzoAgent kidzoAgent;
```

**2c.** `onCreate()`dagi `setupBirdAi();` qatorini quyidagicha almashtiring:

```java
        setupKidzo();
```

**2d.** `setupBirdAi()` metodini o'chirib, o'rniga `setupKidzo()` yozing:

```java
    private void setupKidzo() {
        try {
            ContentFilter contentFilter = ContentFilter.fromAssets(this);
            kidzoAgent = new KidzoAgent(
                contentFilter,
                new RealGeminiCaller(),
                r -> new Handler(Looper.getMainLooper()).post(r)
            );
            kidzoAgent.setListener(new KidzoStateListener() {
                @Override
                public void onStateChanged(KidzoState newState, Object payload) {
                    // BottomSheet o'zi boshqaradi — bu yerda hech narsa qilmaymiz
                }

                @Override
                public void onActionRequested(String contentId) {
                    // WebView'da kontentni ochish
                    String js = "if(window.openContent) openContent('" + contentId + "');";
                    if (webViewManager != null) webViewManager.evaluateJavascript(js);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Kidzo setup failed", e);
            return;
        }

        View btnBird = findViewById(R.id.btnBirdAi);
        if (btnBird != null) {
            btnBird.bringToFront();
            btnBird.setOnClickListener(v -> {
                KidzoBottomSheet sheet = new KidzoBottomSheet(kidzoAgent);
                sheet.show(getSupportFragmentManager(), "kidzo");
                kidzoAgent.requestRecommendations();
            });
        }
    }
```

**2e.** `onDestroy()`da `birdAiManager.onDestroy()` qatorini quyidagicha almashtiring:

```java
        if (kidzoAgent != null) kidzoAgent.dismiss();
```

- [ ] **Step 3: Build va qurilmaga o'rnatish**

```bash
./gradlew assembleDebug
```

Kutilgan natija: `BUILD SUCCESSFUL`

```bash
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 4: Qo'lda test (P1 tekshiruvi)**

Qurilmada:
1. Ilovani oching — sariq 🐥 FAB ko'rinishi kerak
2. FABga bosing — BottomSheet ochiladi + "Kidzo o'ylamoqda… 🐥" spinner ko'rinishi kerak
3. (Gemini javobi kelgunicha) THINKING holati spinner ko'rsatishi kerak

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/kidzo/RealGeminiCaller.java \
        app/src/main/java/uz/kidzone/app/MainActivity.java
git commit -m "feat(kidzo): P1 complete — FAB → BottomSheet THINKING state, RealGeminiCaller"
```

---
## ── Phase 2: Recommendations ──
---

## Task 9: KidzoProfile (TDD)

**Files:**
- Create: `app/src/test/java/uz/kidzone/app/kidzo/KidzoProfileTest.java`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/KidzoProfile.java`

- [ ] **Step 1: Muvaffaqiyatsiz testlarni yozish**

`app/src/test/java/uz/kidzone/app/kidzo/KidzoProfileTest.java`:

```java
package uz.kidzone.app.kidzo;

import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class KidzoProfileTest {

    // SharedPreferences'ni simulate qiladigan in-memory map
    private Map<String, String> fakePrefs;
    private KidzoProfile profile;

    @Before
    public void setUp() {
        fakePrefs = new HashMap<>();
        profile = new KidzoProfile(fakePrefs::get, fakePrefs::put);
    }

    @Test
    public void getChildName_default_returnsDefault() {
        assertEquals("Bolam", profile.getChildName());
    }

    @Test
    public void setAndGetChildName_returnsSetValue() {
        profile.setChildName("Amir");
        assertEquals("Amir", profile.getChildName());
    }

    @Test
    public void getLastContentId_default_returnsNull() {
        assertNull(profile.getLastContentId());
    }

    @Test
    public void recordContentPlayed_savesId() {
        profile.recordContentPlayed("story-003");
        assertEquals("story-003", profile.getLastContentId());
    }

    @Test
    public void recordContentPlayed_overwritesPrevious() {
        profile.recordContentPlayed("story-001");
        profile.recordContentPlayed("song-002");
        assertEquals("song-002", profile.getLastContentId());
    }
}
```

- [ ] **Step 2: Testlarni ishga tushirib, muvaffaqiyatsiz ekanini tasdiqlash**

```bash
./gradlew test --tests "uz.kidzone.app.kidzo.KidzoProfileTest"
```

Kutilgan natija: `FAILED` — `KidzoProfile` hali mavjud emas.

- [ ] **Step 3: KidzoProfile implementatsiyasini yozish**

`app/src/main/java/uz/kidzone/app/kidzo/KidzoProfile.java`:

```java
package uz.kidzone.app.kidzo;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

public class KidzoProfile {

    private static final String PREFS_NAME    = "kidzo_profile";
    private static final String KEY_NAME      = "child_name";
    private static final String KEY_LAST_ID   = "last_content_id";
    private static final String DEFAULT_NAME  = "Bolam";

    // Testda inject qilinadi, productionda SharedPreferences ishlatiladi
    private final StringGetter getter;
    private final StringSetter setter;

    public interface StringGetter { @Nullable String get(String key, @Nullable String defVal); }
    public interface StringSetter { void put(String key, String value); }

    /** Production konstruktori. */
    public static KidzoProfile fromContext(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new KidzoProfile(
            (key, def) -> prefs.getString(key, def),
            (key, val) -> prefs.edit().putString(key, val).apply()
        );
    }

    /** Test konstruktori — in-memory xotira bilan. */
    KidzoProfile(StringGetter getter, StringSetter setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public String getChildName() {
        return getter.get(KEY_NAME, DEFAULT_NAME);
    }

    public void setChildName(String name) {
        setter.put(KEY_NAME, name);
    }

    @Nullable
    public String getLastContentId() {
        return getter.get(KEY_LAST_ID, null);
    }

    public void recordContentPlayed(String contentId) {
        setter.put(KEY_LAST_ID, contentId);
    }
}
```

- [ ] **Step 4: Testlarni ishga tushirib, o'tishini tasdiqlash**

```bash
./gradlew test --tests "uz.kidzone.app.kidzo.KidzoProfileTest"
```

Kutilgan natija: `BUILD SUCCESSFUL` — barcha 5 test `PASSED`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/kidzo/KidzoProfile.java \
        app/src/test/java/uz/kidzone/app/kidzo/KidzoProfileTest.java
git commit -m "feat(kidzo): KidzoProfile — SharedPreferences wrapper, TDD"
```

---

## Task 10: KidzoAgent — Profil + Gemini Prompt Wiring

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/kidzo/KidzoAgent.java`
- Modify: `app/src/main/java/uz/kidzone/app/MainActivity.java`

- [ ] **Step 1: KidzoAgent'ga KidzoProfile qo'shish**

`KidzoAgent.java` ning maydonlari qatoriga qo'shing:

```java
    private @Nullable KidzoProfile profile;
```

Test konstruktoriga `profile` qo'shish shart emas — u optional. Ikkita yangi metod qo'shing:

```java
    public void setProfile(KidzoProfile profile) {
        this.profile = profile;
    }

    public KidzoProfile getProfile() { return profile; }
```

- [ ] **Step 2: requestRecommendations() ichida profil ma'lumotlarini ishlatish**

`KidzoAgent.java` dagi `requestRecommendations()` metodini quyidagicha yangilang:

```java
    public void requestRecommendations() {
        setState(KidzoState.THINKING, null);
        List<ContentItem> top5 = contentFilter.getTop5();
        String promptBlock = contentFilter.toPromptBlock(top5);

        String childName     = profile != null ? profile.getChildName()     : "Bolam";
        String lastContentId = profile != null ? profile.getLastContentId() : null;

        String prompt = buildRecommendationPrompt(childName, lastContentId, promptBlock);

        geminiCaller.call(prompt,
            text -> {
                List<ContentCard> cards = ActionParser.parseRecommendations(text);
                if (cards.isEmpty()) {
                    for (ContentItem item : top5) {
                        cards.add(new ContentCard(item.id, item.emoji + " " + item.titleUz));
                    }
                }
                setState(KidzoState.RECOMMENDATIONS, cards);
            },
            errorMsg -> setState(KidzoState.ERROR, errorMsg)
        );
    }
```

- [ ] **Step 3: MainActivity'da KidzoProfile'ni KidzoAgent'ga berish**

`MainActivity.java` dagi `setupKidzo()` metodida, `kidzoAgent = new KidzoAgent(...)` qatoridan keyin qo'shing:

```java
            kidzoAgent.setProfile(KidzoProfile.fromContext(this));
```

Import qo'shing:
```java
import uz.kidzone.app.kidzo.KidzoProfile;
```

- [ ] **Step 4: onActionRequested'da profil yangilash**

`setupKidzo()` dagi `onActionRequested()` callback'ini yangilang:

```java
                @Override
                public void onActionRequested(String contentId) {
                    if (kidzoAgent.getProfile() != null) {
                        kidzoAgent.getProfile().recordContentPlayed(contentId);
                    }
                    String js = "if(window.openContent) openContent('" + contentId + "');";
                    if (webViewManager != null) webViewManager.evaluateJavascript(js);
                }
```

- [ ] **Step 5: Build va qurilmaga o'rnatish**

```bash
./gradlew assembleDebug && adb install -r -t app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 6: Qo'lda test**

FABga bosing → BottomSheet ochiladi → Gemini javobi kelgandan so'ng 3 ta karta ko'rinishi kerak. Kartaga bosing → ertak/qo'shiq ochilishi kerak. Ikkinchi marta FAB bossangiz — Gemini oxirgi kontent ID'ni bilishi kerak (prompt'da ko'rinadi).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/kidzo/KidzoAgent.java \
        app/src/main/java/uz/kidzone/app/MainActivity.java
git commit -m "feat(kidzo): P2 — KidzoProfile wiring, personalized recommendations"
```

---

## Task 11: KidzoCardAdapter — Emoji Lookup bilan

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/kidzo/KidzoCardAdapter.java`

- [ ] **Step 1: KidzoCardAdapter'ga ContentFilter lookup qo'shish**

`KidzoCardAdapter.java`ni to'liq quyidagicha almashtiring:

```java
package uz.kidzone.app.kidzo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import uz.kidzone.app.R;
import java.util.List;

public class KidzoCardAdapter extends RecyclerView.Adapter<KidzoCardAdapter.VH> {

    public interface OnCardClick { void onClick(String contentId); }

    private final List<ContentCard> cards;
    private final OnCardClick onCardClick;
    private final @Nullable ContentFilter contentFilter;

    public KidzoCardAdapter(List<ContentCard> cards,
                            @Nullable ContentFilter contentFilter,
                            OnCardClick onCardClick) {
        this.cards = cards;
        this.contentFilter = contentFilter;
        this.onCardClick = onCardClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_kidzo_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ContentCard card = cards.get(position);

        // ContentFilter'dan emoji + aniq nom olish
        String emoji = "🐥";
        String title = card.displayText;
        if (contentFilter != null) {
            ContentItem item = contentFilter.findById(card.contentId);
            if (item != null) {
                emoji = item.emoji;
                title = item.titleUz + (card.displayText.isEmpty() ? "" : " — " + card.displayText);
            }
        }

        holder.tvEmoji.setText(emoji);
        holder.tvTitle.setText(title);
        holder.btnPlay.setOnClickListener(v -> onCardClick.onClick(card.contentId));
        holder.itemView.setOnClickListener(v -> onCardClick.onClick(card.contentId));
    }

    @Override
    public int getItemCount() { return cards.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvTitle;
        View btnPlay;
        VH(@NonNull View v) {
            super(v);
            tvEmoji = v.findViewById(R.id.tv_card_emoji);
            tvTitle = v.findViewById(R.id.tv_card_title);
            btnPlay = v.findViewById(R.id.btn_card_play);
        }
    }
}
```

- [ ] **Step 2: KidzoBottomSheet'da ContentFilter va yangi konstruktorni ishlatish**

`KidzoBottomSheet.java`da `agent` maydonidan keyin qo'shing:

```java
    private final @Nullable ContentFilter contentFilter;
```

Konstruktorni yangilang:

```java
    public KidzoBottomSheet(KidzoAgent agent, @Nullable ContentFilter contentFilter) {
        this.agent = agent;
        this.contentFilter = contentFilter;
    }
```

`onStateChanged()` ichida `RECOMMENDATIONS` blokini yangilang:

```java
                case RECOMMENDATIONS:
                    layoutRecommendations.setVisibility(View.VISIBLE);
                    if (payload instanceof List) {
                        List<ContentCard> cards = (List<ContentCard>) payload;
                        rvCards.setAdapter(new KidzoCardAdapter(cards, contentFilter, contentId -> {
                            agent.openContent(contentId);
                            dismiss();
                        }));
                    }
                    break;
```

- [ ] **Step 3: MainActivity'da yangi konstruktorni ishlatish**

`MainActivity.java` dagi `setupKidzo()` ichida BottomSheet yaratish qatorini yangilang:

```java
            btnBird.setOnClickListener(v -> {
                KidzoBottomSheet sheet = new KidzoBottomSheet(kidzoAgent, contentFilter);
                sheet.show(getSupportFragmentManager(), "kidzo");
                kidzoAgent.requestRecommendations();
            });
```

`contentFilter`ni metod darajasida saqlash uchun `kidzoAgent` yaratilgan yerda o'zgaruvchini mahalliy qilib oling:

```java
    private void setupKidzo() {
        ContentFilter contentFilter;
        try {
            contentFilter = ContentFilter.fromAssets(this);
            // ... qolgan kod
        } catch (Exception e) { ... }
        // btnBird listener'da contentFilter ishlatiladi (effectively final)
```

- [ ] **Step 4: Build + install + qo'lda test**

```bash
./gradlew assembleDebug && adb install -r -t app/build/outputs/apk/debug/app-debug.apk
```

Tekshiring: kartalar to'g'ri emoji va nomlar bilan ko'rinishi kerak. Kartaga bosish ertak/qo'shiq ochishi kerak.

- [ ] **Step 5: Barcha testlarni ishga tushirib tasdiqlash**

```bash
./gradlew test
```

Kutilgan natija: `BUILD SUCCESSFUL` — barcha testlar `PASSED`.

- [ ] **Step 6: Commit — P2 yakunlash**

```bash
git add app/src/main/java/uz/kidzone/app/kidzo/KidzoCardAdapter.java \
        app/src/main/java/uz/kidzone/app/kidzo/KidzoBottomSheet.java \
        app/src/main/java/uz/kidzone/app/MainActivity.java
git commit -m "feat(kidzo): P2 complete — emoji lookup, RECOMMENDATIONS state polished"
```

---
## ── Phase 3: Chat + TTS ──
---

## Task 12: Chat Rejimi — CHATTING State

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/kidzo/KidzoAgent.java`

- [ ] **Step 1: Multi-turn history va startChat() qo'shish**

`KidzoAgent.java`ga yangi maydonlar qo'shing (boshqa maydonlar yoniga):

```java
    private final java.util.Deque<String[]> chatHistory = new java.util.ArrayDeque<>();
    private static final int MAX_HISTORY_TURNS = 10;
```

Yangi metodlarni qo'shing `dismiss()` metodidan keyin:

```java
    /** RECOMMENDATIONS → CHATTING: chat rejimini boshlaydi. */
    public void startChat() {
        chatHistory.clear();
        setState(KidzoState.CHATTING, "Salom! Men Kidzo. Nima haqida gaplashamiz? 🐥");
    }

    /** Foydalanuvchi xabar yuborganda: CHATTING → THINKING → CHATTING */
    public void sendChatMessage(String userMessage) {
        // Tarix chegarasi
        if (chatHistory.size() >= MAX_HISTORY_TURNS * 2) {
            chatHistory.pollFirst();
            chatHistory.pollFirst();
        }
        chatHistory.addLast(new String[]{"user", userMessage});
        setState(KidzoState.THINKING, null);

        List<ContentItem> filtered = contentFilter.getFiltered(userMessage);
        String contentBlock = filtered.isEmpty() ? "" : contentFilter.toPromptBlock(filtered);
        String prompt = buildChatPrompt(userMessage, contentBlock);

        geminiCaller.call(prompt,
            text -> {
                chatHistory.addLast(new String[]{"model", text});
                // [OPEN:id] bormi tekshirish
                String contentId = ActionParser.parse(text);
                if (contentId != null && listener != null) {
                    mainThreadRunner.run(() -> listener.onActionRequested(contentId));
                }
                setState(KidzoState.CHATTING, text);
            },
            errorMsg -> setState(KidzoState.ERROR, errorMsg)
        );
    }

    private String buildChatPrompt(String userMessage, String contentBlock) {
        String childName = profile != null ? profile.getChildName() : "Bolam";
        StringBuilder sb = new StringBuilder();
        sb.append("Sen KidZone ilovasidagi \"Kidzo\" qushchasan. Juda mehribon, oddiy gaplashasan.\n");
        sb.append("Bolaning ismi: ").append(childName).append(". Unga ism bilan murojaat qil.\n");
        if (!contentBlock.isEmpty()) {
            sb.append("Agar bola kontent so'rasa — faqat quyidagi ro'yxatdan tanlaydi:\n");
            sb.append(contentBlock).append("\n");
            sb.append("Ochish uchun: [OPEN:content-id] formatini ishlat.\n");
        }
        sb.append("Agar mos kontent yo'q — shunchaki gapir. Javob 2-3 gapdan oshmasin.\n\n");
        sb.append("Foydalanuvchi: ").append(userMessage);
        return sb.toString();
    }
```

- [ ] **Step 2: Build tekshirish**

```bash
./gradlew assembleDebug
```

Kutilgan natija: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/kidzo/KidzoAgent.java
git commit -m "feat(kidzo): CHATTING state — sendChatMessage, multi-turn history"
```

---

## Task 13: TTS Integration

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/kidzo/KidzoAgent.java`
- Modify: `app/src/main/java/uz/kidzone/app/MainActivity.java`

- [ ] **Step 1: TTS maydonlarini KidzoAgent'ga qo'shish**

`KidzoAgent.java`ga import qo'shing:

```java
import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
```

Maydonlar qo'shing:

```java
    private @Nullable TextToSpeech tts;
    private boolean isTtsReady = false;
```

- [ ] **Step 2: TTS'ni initialize qiluvchi metod qo'shish**

`KidzoAgent.java`ga yangi metod qo'shing:

```java
    public void initTts(Context ctx) {
        tts = new TextToSpeech(ctx, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("uz", "UZ"));
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
                isTtsReady = true;
            }
        });
    }

    private void speak(String text) {
        // [OPEN:id] taglarini TTS'dan olib tashlash
        String clean = text.replaceAll("\\[OPEN:[a-z0-9\\-]+\\]", "").trim();
        if (isTtsReady && tts != null && !clean.isEmpty()) {
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "KidzoTTS");
        }
    }

    public void stopSpeaking() {
        if (tts != null) tts.stop();
    }

    public void onDestroy() {
        stopSpeaking();
        if (tts != null) { tts.shutdown(); tts = null; }
    }
```

- [ ] **Step 3: sendChatMessage() ichida TTS chaqiruv qo'shish**

`sendChatMessage()` dagi `geminiCaller.call(...)` success callbackida `setState(KidzoState.CHATTING, text)` qatoridan oldin qo'shing:

```java
                mainThreadRunner.run(() -> speak(text));
```

- [ ] **Step 4: MainActivity'da TTS init va onDestroy**

`setupKidzo()` metodida `kidzoAgent = new KidzoAgent(...)` qatoridan keyin qo'shing:

```java
            kidzoAgent.initTts(this);
```

`onDestroy()`dagi `if (kidzoAgent != null) kidzoAgent.dismiss();` qatorini quyidagicha almashtiring:

```java
        if (kidzoAgent != null) kidzoAgent.onDestroy();
```

- [ ] **Step 5: Final build + install**

```bash
./gradlew assembleDebug && adb install -r -t app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 6: To'liq qo'lda test (P3 tekshiruvi)**

| # | Test | Kutilgan natija |
|---|------|-----------------|
| 1 | FAB bosish | BottomSheet + spinner |
| 2 | Gemini javobi | 3 ta karta emoji + nom bilan |
| 3 | Karta bosish | Ertak/qo'shiq ochiladi, BottomSheet yopiladi |
| 4 | Ikkinchi marta FAB | Tavsiyalar bolaning oxirgi kontentini biladi |
| 5 | "Kidzo bilan gaplash" | Chat UI ochiladi, Kidzo salom beradi + ovoz bilan o'qiydi |
| 6 | Chat xabar yuborish | Kidzo javob beradi + ovoz + agar [OPEN:id] bo'lsa kontent ochiladi |
| 7 | Internet o'chirish + FAB | ERROR holati, "Internet yo'q" xabari |
| 8 | ERROR'da Retry | Qayta THINKING → RECOMMENDATIONS |

- [ ] **Step 7: Barcha testlarni yakuniy ishga tushirish**

```bash
./gradlew test
```

Kutilgan natija: `BUILD SUCCESSFUL` — barcha testlar `PASSED`.

- [ ] **Step 8: Commit — P3 yakunlash**

```bash
git add app/src/main/java/uz/kidzone/app/kidzo/KidzoAgent.java \
        app/src/main/java/uz/kidzone/app/MainActivity.java
git commit -m "feat(kidzo): P3 complete — TTS integration, full Kidzo agent ready"
```

---

## Yakuniy Holat

Barcha 3 faza tugagandan so'ng:

```
app/src/main/java/uz/kidzone/app/
├── MainActivity.java           [mod] setupKidzo() — FAB → KidzoBottomSheet
└── kidzo/
    ├── KidzoState.java         IDLE|THINKING|RECOMMENDATIONS|CHATTING|ERROR
    ├── KidzoStateListener.java onStateChanged() + onActionRequested()
    ├── GeminiCaller.java       interface (test injection)
    ├── MainThreadRunner.java   interface (test injection)
    ├── ContentCard.java        {contentId, displayText}
    ├── ContentItem.java        {id, emoji, titleUz/Ru/En, category}
    ├── ContentFilter.java      JSON parser, filter, toPromptBlock()
    ├── ActionParser.java       parse() + parseRecommendations()
    ├── KidzoProfile.java       SharedPreferences wrapper
    ├── KidzoAgent.java         state machine + Gemini + TTS
    ├── RealGeminiCaller.java   Gemini 1.5 Flash production impl
    ├── KidzoCardAdapter.java   RecyclerView adapter
    └── KidzoBottomSheet.java   BottomSheetDialogFragment UI

app/src/test/java/uz/kidzone/app/kidzo/
    ├── ActionParserTest.java   8 test
    ├── ContentFilterTest.java  10 test
    ├── KidzoProfileTest.java   5 test
    └── KidzoAgentStateTest.java 7 test
                                = 30 unit test jami
```

**Kotlin migratsiya:** `KidzoAgent` → `KidzoViewModel + StateFlow`, `KidzoBottomSheet` → `KidzoScreen (Composable)`, `GeminiCaller` interfeysi o'zgarishsiz saqlanadi.
