package uz.kidzone.app;

import android.app.Notification;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class KidZoneFirebaseMessagingServiceTest {

    private KidZoneFirebaseMessagingService service;

    @Before
    public void setUp() {
        service = Robolectric.buildService(KidZoneFirebaseMessagingService.class)
                .create().get();
    }

    @Test
    public void buildNotification_withTitleAndBody_isNotNull() {
        Notification n = service.buildNotification("Salom", "Yangi o'yin mavjud!");
        assertNotNull(n);
    }

    @Test
    public void buildNotification_emptyTitle_notificationNotNull() {
        Notification n = service.buildNotification("", "body text");
        assertNotNull(n);
        assertNotNull(n.extras.getString(Notification.EXTRA_TITLE));
    }

    @Test
    public void buildNotification_normalTitle_titleMatches() {
        Notification n = service.buildNotification("Test Title", "Test Body");
        assertEquals("Test Title", n.extras.getString(Notification.EXTRA_TITLE));
    }
}
