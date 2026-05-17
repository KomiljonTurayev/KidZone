package uz.kidzone.app.kidzo;

import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KidzoAgent {

    private KidzoState currentState = KidzoState.IDLE;
    private final ContentFilter contentFilter;
    private final GeminiCaller geminiCaller;
    private final MainThreadRunner mainThreadRunner;
    private @Nullable KidzoStateListener listener;
    private volatile int callGeneration = 0;

    /** Production factory — wires RealGeminiCaller. */
    public static KidzoAgent create(ContentFilter contentFilter, MainThreadRunner runner) {
        return new KidzoAgent(contentFilter, new RealGeminiCaller(), runner);
    }

    /** Static factory — no Gemini API needed; shows content.json top-5 directly. */
    public static KidzoAgent createStatic(ContentFilter contentFilter, MainThreadRunner runner) {
        GeminiCaller staticCaller = (prompt, onSuccess, onError) -> new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            if (prompt.startsWith("Sen KidZone")) {
                onSuccess.accept("");   // triggers top-5 fallback in requestRecommendations()
            } else {
                onSuccess.accept("Salom! 🐥 Bugun qaysi ertakni tinglaysiz?");
            }
        }).start();
        return new KidzoAgent(contentFilter, staticCaller, runner);
    }

    /** Test constructor — GeminiCaller and MainThreadRunner are injected. */
    KidzoAgent(ContentFilter contentFilter,
               GeminiCaller geminiCaller,
               MainThreadRunner mainThreadRunner) {
        this.contentFilter    = contentFilter;
        this.geminiCaller     = geminiCaller;
        this.mainThreadRunner = mainThreadRunner;
    }

    public void setListener(@Nullable KidzoStateListener listener) {
        this.listener = listener;
    }

    public KidzoState getCurrentState() { return currentState; }

    /** FAB pressed: IDLE → THINKING → RECOMMENDATIONS */
    public void requestRecommendations() {
        final int gen = ++callGeneration;
        setState(KidzoState.THINKING, null);
        List<ContentItem> top5 = contentFilter.getTop5();
        String promptBlock = contentFilter.toPromptBlock(top5);
        String prompt = buildRecommendationPrompt("Bolam", null, promptBlock);

        // Build id→item map for emoji lookup
        final Map<String, ContentItem> itemMap = new HashMap<>();
        for (ContentItem item : top5) itemMap.put(item.id, item);

        geminiCaller.call(prompt,
            text -> {
                if (gen != callGeneration) return;
                List<ContentCard> parsed = ActionParser.parseRecommendations(text);
                List<ContentCard> cards = enrich(parsed, itemMap);
                if (cards.isEmpty()) {
                    for (ContentItem item : top5) {
                        cards.add(new ContentCard(item.id, item.titleUz,
                            item.emoji, typeOf(item.id)));
                    }
                }
                setState(KidzoState.RECOMMENDATIONS, cards);
            },
            errorMsg -> {
                if (gen != callGeneration) return;
                setState(KidzoState.ERROR, errorMsg);
            }
        );
    }

    /** Open content via card or chat. */
    public void openContent(String contentId) {
        if (listener != null) {
            mainThreadRunner.run(() -> listener.onActionRequested(contentId));
        }
    }

    public void startChat() {
        setState(KidzoState.CHATTING, "Salom! Men Kidzo. Nima haqida gaplashamiz? 🐥");
    }

    public void sendChatMessage(String userMessage) {
        final int gen = ++callGeneration;
        setState(KidzoState.THINKING, null);
        geminiCaller.call(
            "Sen Kidzo. Qisqa javob ber: " + userMessage,
            text -> { if (gen == callGeneration) setState(KidzoState.CHATTING, text); },
            err  -> { if (gen == callGeneration) setState(KidzoState.ERROR, err); }
        );
    }

    public void dismiss() {
        callGeneration++;
        setState(KidzoState.IDLE, null);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private List<ContentCard> enrich(List<ContentCard> parsed,
                                      Map<String, ContentItem> itemMap) {
        List<ContentCard> result = new ArrayList<>();
        for (ContentCard card : parsed) {
            ContentItem item = itemMap.get(card.contentId);
            String emoji = (item != null && !item.emoji.isEmpty()) ? item.emoji : "🐥";
            result.add(new ContentCard(card.contentId, card.displayText,
                emoji, typeOf(card.contentId)));
        }
        return result;
    }

    private static String typeOf(String contentId) {
        return contentId.startsWith("song-") ? "Qo'shiq" : "Ertak";
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
