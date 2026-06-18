package uz.kidzone.app;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class BannerCheckerTest {

    @Test
    public void unavailableSync_returnsNull() {
        AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
        FirestoreSync sync = new FirestoreSync(null);
        BannerChecker.checkAsync(sync, result::set);
        assertNull(result.get());
    }

    @Test
    public void activeBanner_returnsData() {
        AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
        BannerChecker.checkAsync(
            cb -> cb.onDoc(true, true, "Sarlavha", "Matn", "https://example.com"),
            result::set
        );
        assertNotNull(result.get());
        assertEquals("Sarlavha", result.get().title);
        assertEquals("Matn", result.get().body);
        assertEquals("https://example.com", result.get().url);
    }

    @Test
    public void inactiveBanner_returnsNull() {
        AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
        BannerChecker.checkAsync(
            cb -> cb.onDoc(true, false, "title", "body", "https://example.com"),
            result::set
        );
        assertNull(result.get());
    }

    @Test
    public void noDocument_returnsNull() {
        AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
        BannerChecker.checkAsync(
            cb -> cb.onDoc(false, false, null, null, null),
            result::set
        );
        assertNull(result.get());
    }

    @Test
    public void missingUrl_returnsNull() {
        AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
        BannerChecker.checkAsync(
            cb -> cb.onDoc(true, true, "title", "body", null),
            result::set
        );
        assertNull(result.get());
    }

    @Test
    public void error_returnsNull() {
        AtomicReference<BannerChecker.BannerData> result = new AtomicReference<>();
        BannerChecker.checkAsync(cb -> cb.onError(), result::set);
        assertNull(result.get());
    }
}
