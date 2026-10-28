package uz.kidzone.app.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthTextsTest {

    @Test
    fun `every failure has its own uz, ru and en text`() {
        AuthException.Reason.entries.filter { it != AuthException.Reason.CANCELLED }.forEach { reason ->
            val texts = listOf("uz", "ru", "en").map { reason.userMessage(it) }
            assertEquals("$reason: ${texts}", 3, texts.filterNotNull().toSet().size)
        }
    }

    @Test
    fun `an unknown language falls back to Uzbek`() {
        assertEquals(AuthException.Reason.NETWORK.userMessage("uz"), AuthException.Reason.NETWORK.userMessage("de"))
        assertEquals(AccountFeature.PREMIUM.title("uz"), AccountFeature.PREMIUM.title("de"))
    }

    @Test
    fun `a cancelled sign-in shows nothing`() {
        assertNull(AuthException.Reason.CANCELLED.userMessage("ru"))
    }

    @Test
    fun `features are named in the chosen language`() {
        AccountFeature.entries.forEach { feature ->
            assertNotEquals(feature.title("uz"), feature.title("ru"))
            assertNotEquals(feature.pitch("uz"), feature.pitch("en"))
        }
    }
}
