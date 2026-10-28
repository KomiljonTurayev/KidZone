package uz.kidzone.app.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingViewModelTest {

    @Test
    fun `parent-first flow walks language, gate, sign-in, child, handoff`() {
        val vm = OnboardingViewModel()
        assertEquals(OnboardingStep.LANGUAGE, vm.state.value.step)

        vm.setLang("ru")
        assertEquals(OnboardingStep.PARENT_GATE, vm.state.value.step)
        assertEquals("ru", vm.state.value.lang)

        vm.parentVerified(alreadySignedIn = false)
        assertEquals(OnboardingStep.SIGN_IN, vm.state.value.step)

        vm.signInFinished()
        assertEquals(OnboardingStep.CHILD, vm.state.value.step)

        vm.submitChild("  Ali ", "5-7")
        assertEquals(OnboardingStep.HANDOFF, vm.state.value.step)
        assertEquals("Ali", vm.state.value.childName)
        assertEquals("5-7", vm.state.value.age)
    }

    @Test
    fun `signed-in parent skips the sign-in step`() {
        val vm = OnboardingViewModel()
        vm.setLang("uz")
        vm.parentVerified(alreadySignedIn = true)
        assertEquals(OnboardingStep.CHILD, vm.state.value.step)
    }

    @Test
    fun `back from child returns to the parent gate, not to a finished sign-in`() {
        val vm = OnboardingViewModel()
        vm.setLang("uz")
        vm.parentVerified(alreadySignedIn = false)
        vm.signInFinished()
        vm.back()
        assertEquals(OnboardingStep.PARENT_GATE, vm.state.value.step)
    }

    @Test
    fun `only adult birth years pass the parent gate`() {
        assertTrue(isAdultBirthYear(1990, 2026))
        assertTrue(isAdultBirthYear(2008, 2026))
        assertFalse(isAdultBirthYear(2009, 2026))
        assertFalse(isAdultBirthYear(2020, 2026))
        assertFalse(isAdultBirthYear(1900, 2026))
        assertFalse(isAdultBirthYear(null, 2026))
    }
}
