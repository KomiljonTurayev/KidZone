package uz.kidzone.app;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class FcmTokenManagerTest {

    @Test
    public void nullUid_doesNotCallProvider() {
        AtomicBoolean called = new AtomicBoolean(false);
        FirestoreSync sync = new FirestoreSync(null);
        FcmTokenManager.registerToken(null, sync, cb -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    public void unavailableSync_doesNotCallProvider() {
        AtomicBoolean called = new AtomicBoolean(false);
        FirestoreSync sync = new FirestoreSync(null); // isAvailable() = false
        FcmTokenManager.registerToken("uid-123", sync, cb -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    public void validUidAndUnavailableSync_guardFiresFirst() {
        AtomicBoolean called = new AtomicBoolean(false);
        FirestoreSync sync = new FirestoreSync(null);
        FcmTokenManager.registerToken("uid-xyz", sync, cb -> {
            called.set(true);
            cb.onToken(null);
        });
        assertFalse(called.get());
    }
}
