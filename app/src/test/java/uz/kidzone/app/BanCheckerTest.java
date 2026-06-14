package uz.kidzone.app;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class BanCheckerTest {

    @Test
    public void nullUid_returnsError() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        BanChecker.checkAsync(null, (uid, cb) -> cb.onDoc(true, "active"), result::set);
        assertEquals(BanChecker.Status.ERROR, result.get());
    }

    @Test
    public void unavailableSync_returnsError() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        FirestoreSync sync = new FirestoreSync(null);
        BanChecker.checkAsync("uid-123", sync, result::set);
        assertEquals(BanChecker.Status.ERROR, result.get());
    }

    @Test
    public void docNotExists_returnsActive() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        BanChecker.checkAsync("uid", (uid, cb) -> cb.onDoc(false, null), result::set);
        assertEquals(BanChecker.Status.ACTIVE, result.get());
    }

    @Test
    public void statusActive_returnsActive() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        BanChecker.checkAsync("uid", (uid, cb) -> cb.onDoc(true, "active"), result::set);
        assertEquals(BanChecker.Status.ACTIVE, result.get());
    }

    @Test
    public void statusBanned_returnsBanned() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        BanChecker.checkAsync("uid", (uid, cb) -> cb.onDoc(true, "banned"), result::set);
        assertEquals(BanChecker.Status.BANNED, result.get());
    }

    @Test
    public void statusMissing_returnsActive() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        BanChecker.checkAsync("uid", (uid, cb) -> cb.onDoc(true, null), result::set);
        assertEquals(BanChecker.Status.ACTIVE, result.get());
    }

    @Test
    public void unknownStatus_returnsActive() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        BanChecker.checkAsync("uid", (uid, cb) -> cb.onDoc(true, "suspended"), result::set);
        assertEquals(BanChecker.Status.ACTIVE, result.get());
    }
}
