// app/src/test/java/uz/kidzone/app/data/AppClockTest.kt
package uz.kidzone.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AppClockTest {

    @Test
    fun `ZONE is Asia-Tashkent`() {
        assertEquals(ZoneId.of("Asia/Tashkent"), AppClock.ZONE)
    }

    @Test
    fun `today returns date in Asia-Tashkent zone`() {
        val expected = LocalDate.now(ZoneId.of("Asia/Tashkent")).toString()
        assertEquals(expected, AppClock.today())
    }
}
