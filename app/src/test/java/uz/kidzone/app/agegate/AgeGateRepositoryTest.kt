package uz.kidzone.app.agegate

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class AgeGateRepositoryTest {

    private val today = LocalDate.of(2026, 11, 12)
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        prefs = RuntimeEnvironment.getApplication()
            .getSharedPreferences("age_gate_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    private fun repo() = AgeGateRepository(prefs) { today }

    // region bracketFor — 13 yosh va kichik = CHILD

    @Test
    fun `13 and younger is a child, 14 and older is not`() {
        // Nov 2013 → exactly 13 on 2026-11-01
        assertEquals(AgeBracket.CHILD, AgeGateRepository.bracketFor(2013, 11, today))
        // Dec 2012 → still 13 until December
        assertEquals(AgeBracket.CHILD, AgeGateRepository.bracketFor(2012, 12, today))
        // Nov 2012 → 14
        assertEquals(AgeBracket.TEEN_OR_ADULT, AgeGateRepository.bracketFor(2012, 11, today))
        assertEquals(AgeBracket.TEEN_OR_ADULT, AgeGateRepository.bracketFor(1990, 5, today))
        assertEquals(AgeBracket.CHILD, AgeGateRepository.bracketFor(2020, 1, today))
    }

    @Test
    fun `plausible years are within the last 120 years`() {
        assertTrue(AgeGateRepository.isPlausibleYear(2026, today))
        assertTrue(AgeGateRepository.isPlausibleYear(1906, today))
        assertFalse(AgeGateRepository.isPlausibleYear(1905, today))
        assertFalse(AgeGateRepository.isPlausibleYear(2027, today))
    }

    // endregion

    // region save — bir marta, qayta yozilmaydi

    @Test
    fun `nothing is answered on a fresh install and the app is restricted`() {
        val state = repo().current()
        assertFalse(state.completed)
        assertNull(state.bracket)
        assertTrue(state.isRestricted)
    }

    @Test
    fun `an adult answer unlocks the full experience`() {
        val repo = repo()
        assertTrue(repo.save(1990, 6))
        assertEquals(AgeGateState(completed = true, bracket = AgeBracket.TEEN_OR_ADULT), repo.state.value)
        assertFalse(repo.current().isRestricted)
    }

    @Test
    fun `the answer is saved once and cannot be changed by trying again`() {
        val repo = repo()
        assertTrue(repo.save(2018, 3))
        assertFalse(repo.save(1990, 6))
        assertEquals(AgeBracket.CHILD, repo.current().bracket)
        assertTrue(repo.current().isRestricted)
    }

    @Test
    fun `the answer survives a restart`() {
        repo().save(2018, 3)
        val reopened = repo()
        assertTrue(reopened.current().completed)
        assertEquals(AgeBracket.CHILD, reopened.current().bracket)
        assertFalse(reopened.save(1990, 6))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an implausible year is rejected`() {
        repo().save(1800, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an invalid month is rejected`() {
        repo().save(1990, 13)
    }

    // endregion

    // region isValidBirthDate — ekran validatsiyasi, yoshga bog'liq emas

    @Test
    fun `screen validation only rejects impossible dates`() {
        assertTrue(isValidBirthDate(2026, 11, today))
        assertFalse(isValidBirthDate(2026, 12, today)) // kelajakda
        assertFalse(isValidBirthDate(2027, 1, today))
        assertFalse(isValidBirthDate(1800, 1, today))
        assertFalse(isValidBirthDate(1990, 0, today))
        // Child and adult answers are both valid — the check gives no hint about the "right" age.
        assertTrue(isValidBirthDate(2020, 5, today))
        assertTrue(isValidBirthDate(1985, 5, today))
    }

    // endregion
}
