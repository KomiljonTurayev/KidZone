package uz.kidzone.app;

import org.junit.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class FirestoreSyncAdminTest {

    @Test
    public void getAllUsers_unavailable_returnsEmpty() {
        FirestoreSync sync = new FirestoreSync(null);
        AtomicReference<List<FirestoreSync.UserInfo>> result = new AtomicReference<>();
        sync.getAllUsers(result::set);
        assertNotNull(result.get());
        assertEquals(0, result.get().size());
    }

    @Test
    public void setUserStatus_unavailable_doesNothing() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.setUserStatus("uid123", "banned", null);
        // must not throw
    }

    @Test
    public void setUserStatus_nullUid_doesNothing() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.setUserStatus(null, "banned", null);
        // must not throw
    }

    @Test
    public void setBanner_unavailable_doesNothing() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.setBanner("title", "body", "https://example.com", "uid");
        // must not throw
    }

    @Test
    public void clearBanner_unavailable_doesNothing() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.clearBanner();
        // must not throw
    }

    @Test
    public void userInfo_fields_correct() {
        FirestoreSync.UserInfo info = new FirestoreSync.UserInfo("uid1", "test@mail.com", "active");
        assertEquals("uid1", info.uid);
        assertEquals("test@mail.com", info.email);
        assertEquals("active", info.status);
    }
}
