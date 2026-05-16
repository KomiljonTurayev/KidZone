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
    @SuppressWarnings("unchecked")
    public void requestRecommendations_emptyGeminiResponse_usesFallback() {
        KidzoAgent agent = new KidzoAgent(
            contentFilter,
            (prompt, onSuccess, onError) -> onSuccess.accept(""),
            syncRunner
        );
        agent.setListener(listener);

        agent.requestRecommendations();

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(listener).onStateChanged(eq(KidzoState.RECOMMENDATIONS), payloadCaptor.capture());
        List<ContentCard> cards = (List<ContentCard>) payloadCaptor.getValue();
        assertEquals(2, cards.size()); // story-001 + song-001 from TEST_JSON
        assertEquals("story-001", cards.get(0).contentId);
    }

    @Test
    public void dismiss_fromThinkingState_setsIdleState() {
        KidzoAgent agent = new KidzoAgent(
            contentFilter,
            (prompt, onSuccess, onError) -> { /* never calls back */ },
            syncRunner
        );
        agent.setListener(listener);
        agent.requestRecommendations(); // → THINKING (no callback)

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
        agent.openContent("story-001");

        verify(listener).onActionRequested("story-001");
    }

    @Test
    public void contentCard_twoArgConstructor_defaultsEmoji() {
        ContentCard card = new ContentCard("story-001", "Sher");
        assertEquals("🐥", card.emoji);
        assertEquals("", card.type);
    }

    @Test
    public void contentCard_fourArgConstructor_storesAll() {
        ContentCard card = new ContentCard("song-001", "Alla", "🌙", "Qo'shiq");
        assertEquals("🌙", card.emoji);
        assertEquals("Qo'shiq", card.type);
    }

    @Test
    public void requestRecommendations_geminiResponse_enrichesCardsWithEmoji() {
        String fakeResponse =
            "[OPEN:story-001] Sher va Sichqon — ajoyib!\n"
          + "[OPEN:song-001] Alla — uxlash vaqti!";

        KidzoAgent agent = new KidzoAgent(
            contentFilter,
            (prompt, onSuccess, onError) -> onSuccess.accept(fakeResponse),
            syncRunner
        );
        agent.setListener(listener);
        agent.requestRecommendations();

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(listener, atLeast(2)).onStateChanged(any(), payloadCaptor.capture());
        List<Object> payloads = payloadCaptor.getAllValues();
        @SuppressWarnings("unchecked")
        List<ContentCard> cards = (List<ContentCard>) payloads.get(payloads.size() - 1);

        assertEquals("🦁", cards.get(0).emoji);
        assertEquals("Ertak", cards.get(0).type);
        assertEquals("🌙", cards.get(1).emoji);
        assertEquals("Qo'shiq", cards.get(1).type);
    }

    @Test
    public void requestRecommendations_fallback_includesEmojiAndType() {
        KidzoAgent agent = new KidzoAgent(
            contentFilter,
            (prompt, onSuccess, onError) -> onSuccess.accept("no valid tags here"),
            syncRunner
        );
        agent.setListener(listener);
        agent.requestRecommendations();

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(listener, atLeast(2)).onStateChanged(any(), payloadCaptor.capture());
        List<Object> payloads = payloadCaptor.getAllValues();
        @SuppressWarnings("unchecked")
        List<ContentCard> cards = (List<ContentCard>) payloads.get(payloads.size() - 1);

        assertFalse(cards.isEmpty());
        assertFalse(cards.get(0).emoji.isEmpty());
        assertFalse(cards.get(0).type.isEmpty());
    }
}
