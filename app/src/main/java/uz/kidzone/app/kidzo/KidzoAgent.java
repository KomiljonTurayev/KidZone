package uz.kidzone.app.kidzo;

import androidx.annotation.Nullable;
import java.util.List;

public class KidzoAgent {

    private KidzoState currentState = KidzoState.IDLE;
    private final ContentFilter contentFilter;
    private final GeminiCaller geminiCaller;
    private final MainThreadRunner mainThreadRunner;
    private @Nullable KidzoStateListener listener;

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
        setState(KidzoState.THINKING, null);
        List<ContentItem> top5 = contentFilter.getTop5();
        String promptBlock = contentFilter.toPromptBlock(top5);
        String prompt = buildRecommendationPrompt("Bolam", null, promptBlock);

        geminiCaller.call(prompt,
            text -> {
                List<ContentCard> cards = ActionParser.parseRecommendations(text);
                if (cards.isEmpty()) {
                    for (ContentItem item : top5) {
                        cards.add(new ContentCard(item.id, item.titleUz));
                    }
                }
                setState(KidzoState.RECOMMENDATIONS, cards);
            },
            errorMsg -> setState(KidzoState.ERROR, errorMsg)
        );
    }

    /** Open content via card or chat. */
    public void openContent(String contentId) {
        if (listener != null) {
            mainThreadRunner.run(() -> listener.onActionRequested(contentId));
        }
    }

    /** Stub for P1/P2 compilation. Fully implemented in Task 12. */
    public void startChat() {
        setState(KidzoState.CHATTING, "Salom! Men Kidzo. Nima haqida gaplashamiz? 🐥");
    }

    /** Stub for P1/P2 compilation. Fully implemented in Task 12. */
    public void sendChatMessage(String userMessage) {
        setState(KidzoState.THINKING, null);
        geminiCaller.call(
            "Sen Kidzo. Qisqa javob ber: " + userMessage,
            text -> setState(KidzoState.CHATTING, text),
            err  -> setState(KidzoState.ERROR, err)
        );
    }

    /** Return to IDLE from any state. */
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
