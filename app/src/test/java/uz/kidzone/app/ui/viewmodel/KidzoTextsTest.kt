package uz.kidzone.app.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KidzoTextsTest {

    @Test
    fun `Kidzo greets in the app language`() {
        assertTrue(greeting("uz").startsWith("Salom"))
        assertTrue(greeting("ru").startsWith("Привет"))
        assertTrue(greeting("en").startsWith("Hi"))
    }

    @Test
    fun `an unknown language greets in Uzbek`() {
        assertEquals(greeting("uz"), greeting("de"))
    }

    @Test
    fun `the fallback reply uses the child's name`() {
        assertTrue(fallbackReply("en", "Aziz").startsWith("I hear you, Aziz!"))
        assertTrue(fallbackReply("ru", "Aziz").startsWith("Я тебя слышу, Aziz!"))
    }

    @Test
    fun `without a name the fallback reply still reads naturally`() {
        listOf("uz", "ru", "en").forEach { lang ->
            assertFalse(lang, fallbackReply(lang, "").contains(", !"))
        }
    }
}
