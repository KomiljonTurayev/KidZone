package uz.kidzone.app

import android.app.Notification
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
class KidZoneFirebaseMessagingServiceKtTest {

    private lateinit var service: KidZoneFirebaseMessagingService

    @Before
    fun setUp() {
        service = Robolectric.buildService(KidZoneFirebaseMessagingService::class.java)
                .create().get()
    }

    @Test
    fun buildNotification_withTitleAndBody_isNotNull() {
        val n = service.buildNotification("Salom", "Yangi o'yin mavjud!")
        assertNotNull(n)
    }

    @Test
    fun buildNotification_emptyTitle_notificationNotNull() {
        val n = service.buildNotification("", "body text")
        assertNotNull(n)
        assertNotNull(n.extras.getString(Notification.EXTRA_TITLE))
    }

    @Test
    fun buildNotification_normalTitle_titleMatches() {
        val n = service.buildNotification("Test Title", "Test Body")
        assertEquals("Test Title", n.extras.getString(Notification.EXTRA_TITLE))
    }
}
