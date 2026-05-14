package uz.kidzone.app.kidzo;

import java.util.List;

public interface KidzoStateListener {
    /**
     * Called when state changes.
     * @param newState new state
     * @param payload RECOMMENDATIONS → List<ContentCard>, CHATTING → String (response text),
     *                ERROR → String (message), others → null
     */
    void onStateChanged(KidzoState newState, Object payload);

    /**
     * Called when child wants to open content.
     * @param contentId e.g. "story-003" or "song-001"
     */
    void onActionRequested(String contentId);
}
