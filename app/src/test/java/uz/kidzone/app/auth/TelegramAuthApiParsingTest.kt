package uz.kidzone.app.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** The JSON contract with Kidzone_Backend's TelegramAuthController. */
class TelegramAuthApiParsingTest {

    @Test
    fun `start response gives login id and bot link`() {
        val start = OkHttpTelegramAuthApi.parseStart(
            """{"loginId":"abc","botUrl":"https://t.me/KidZoneBot?start=abc","expiresAt":"2026-10-27T09:05:00Z"}""")

        assertEquals(TelegramLoginStart("abc", "https://t.me/KidZoneBot?start=abc"), start)
    }

    @Test
    fun `status responses map to their states`() {
        assertEquals(TelegramLoginStatus.Pending, OkHttpTelegramAuthApi.parseStatus("""{"status":"PENDING"}"""))
        assertEquals(TelegramLoginStatus.Expired, OkHttpTelegramAuthApi.parseStatus("""{"status":"EXPIRED"}"""))
        assertEquals(
            TelegramLoginStatus.Confirmed("custom", "Aziz"),
            OkHttpTelegramAuthApi.parseStatus("""{"status":"CONFIRMED","customToken":"custom","displayName":"Aziz"}"""),
        )
    }

    @Test
    fun `confirmed without display name is fine`() {
        assertEquals(
            TelegramLoginStatus.Confirmed("custom", null),
            OkHttpTelegramAuthApi.parseStatus("""{"status":"CONFIRMED","customToken":"custom"}"""),
        )
    }

    @Test
    fun `malformed or unknown responses fail as auth errors`() {
        val garbage = assertThrows(AuthException::class.java) { OkHttpTelegramAuthApi.parseStatus("<html>") }
        val unknown = assertThrows(AuthException::class.java) { OkHttpTelegramAuthApi.parseStatus("""{"status":"WAT"}""") }

        assertEquals(AuthException.Reason.FAILED, garbage.reason)
        assertEquals(AuthException.Reason.FAILED, unknown.reason)
    }
}
