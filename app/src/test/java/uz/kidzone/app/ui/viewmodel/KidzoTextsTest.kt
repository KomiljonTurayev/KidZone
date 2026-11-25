package uz.kidzone.app.ui.viewmodel

import org.junit.Assert.assertEquals
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
}
