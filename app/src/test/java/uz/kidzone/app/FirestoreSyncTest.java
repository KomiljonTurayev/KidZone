package uz.kidzone.app;

import org.junit.Test;
import java.util.Collections;

import static org.junit.Assert.*;

public class FirestoreSyncTest {

    @Test
    public void notConfigured_isAvailableFalse() {
        FirestoreSync sync = new FirestoreSync(null);
        assertFalse(sync.isAvailable());
    }

    @Test
    public void normalizeAgeGroup_twoToFour_returnsThreeToFive() {
        assertEquals("3-5", FirestoreSync.normalizeAgeGroup("2-4"));
    }

    @Test
    public void normalizeAgeGroup_threeToFive_unchanged() {
        assertEquals("3-5", FirestoreSync.normalizeAgeGroup("3-5"));
    }

    @Test
    public void normalizeAgeGroup_fiveToSeven_unchanged() {
        assertEquals("5-7", FirestoreSync.normalizeAgeGroup("5-7"));
    }

    @Test
    public void normalizeAgeGroup_sevenPlus_unchanged() {
        assertEquals("7+", FirestoreSync.normalizeAgeGroup("7+"));
    }

    @Test
    public void normalizeAgeGroup_null_defaultsToThreeToFive() {
        assertEquals("3-5", FirestoreSync.normalizeAgeGroup(null));
    }

    @Test
    public void normalizeAgeGroup_unknown_defaultsToThreeToFive() {
        assertEquals("3-5", FirestoreSync.normalizeAgeGroup("unknown"));
    }

    @Test
    public void notConfigured_syncUserProfile_doesNotThrow() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.syncUserProfile("uid", "Name", "a@b.com", "3-5");
    }

    @Test
    public void notConfigured_updateFcmToken_doesNotThrow() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.updateFcmToken("uid", "token123");
    }

    @Test
    public void notConfigured_recordSession_doesNotThrow() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.recordSession("uid", 10, Collections.emptyMap(), true);
    }

    @Test
    public void recordSession_zeroMinutes_doesNotThrow() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.recordSession("uid", 0, Collections.emptyMap(), false);
    }

    @Test
    public void recordSession_nullUid_doesNotThrow() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.recordSession(null, 5, Collections.emptyMap(), true);
    }

    @Test
    public void syncUserProfile_nullDisplayName_usesEmptyString() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.syncUserProfile("uid", null, "a@b.com", "3-5");
        // no exception
    }

    @Test
    public void syncUserProfile_nullEmail_usesEmptyString() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.syncUserProfile("uid", "Name", null, "3-5");
        // no exception
    }
}
