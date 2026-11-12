package uz.kidzone.app.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.kidzone.app.agegate.AgeBracket

class OnboardingViewModelTest {

    @Test
    fun `adult flow walks language, age, sign-in, child, handoff`() {
        val vm = OnboardingViewModel()
        assertEquals(OnboardingStep.LANGUAGE, vm.state.value.step)

        vm.setLang("ru")
        assertEquals(OnboardingStep.AGE, vm.state.value.step)
        assertEquals("ru", vm.state.value.lang)

        vm.ageAnswered(AgeBracket.TEEN_OR_ADULT, alreadySignedIn = false)
        assertEquals(OnboardingStep.SIGN_IN, vm.state.value.step)
        assertFalse(vm.state.value.isChild)

        vm.signInFinished()
        assertEquals(OnboardingStep.CHILD, vm.state.value.step)

        vm.submitChild("  Ali ", "5-7")
        assertEquals(OnboardingStep.HANDOFF, vm.state.value.step)
        assertEquals("Ali", vm.state.value.childName)
        assertEquals("5-7", vm.state.value.age)
    }

    @Test
    fun `a child never sees the sign-in step`() {
        val vm = OnboardingViewModel()
        vm.setLang("uz")
        vm.ageAnswered(AgeBracket.CHILD, alreadySignedIn = false)
        assertEquals(OnboardingStep.CHILD, vm.state.value.step)
        assertTrue(vm.state.value.isChild)
    }

    @Test
    fun `a child skips sign-in even on a signed-in device`() {
        val vm = OnboardingViewModel()
        vm.setLang("uz")
        vm.ageAnswered(AgeBracket.CHILD, alreadySignedIn = true)
        assertEquals(OnboardingStep.CHILD, vm.state.value.step)
    }

    @Test
    fun `signed-in adult skips the sign-in step`() {
        val vm = OnboardingViewModel()
        vm.setLang("uz")
        vm.ageAnswered(AgeBracket.TEEN_OR_ADULT, alreadySignedIn = true)
        assertEquals(OnboardingStep.CHILD, vm.state.value.step)
    }

    @Test
    fun `the age screen cannot be revisited once answered`() {
        val vm = OnboardingViewModel()
        vm.setLang("uz")
        vm.ageAnswered(AgeBracket.CHILD, alreadySignedIn = false)

        vm.back()
        assertEquals(OnboardingStep.LANGUAGE, vm.state.value.step)

        // Picking a language again goes past the age screen, keeping the first answer.
        vm.setLang("en")
        assertEquals(OnboardingStep.CHILD, vm.state.value.step)
        assertEquals(AgeBracket.CHILD, vm.state.value.bracket)
    }

    @Test
    fun `an answer saved before onboarding skips the age step`() {
        val vm = OnboardingViewModel(initialBracket = AgeBracket.TEEN_OR_ADULT)
        vm.setLang("uz")
        assertEquals(OnboardingStep.SIGN_IN, vm.state.value.step)
    }

    @Test
    fun `back from the unanswered age screen returns to the language picker`() {
        val vm = OnboardingViewModel()
        vm.setLang("uz")
        vm.back()
        assertEquals(OnboardingStep.LANGUAGE, vm.state.value.step)
        vm.setLang("uz")
        assertEquals(OnboardingStep.AGE, vm.state.value.step)
    }
}
