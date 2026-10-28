package uz.kidzone.app.ui.viewmodel

import uz.kidzone.app.arch.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Parent-first setup, the way Khan Academy Kids / YouTube Kids / Lingokids do it: the parent
 * picks a language, proves they're an adult, signs in (or skips), creates the child's profile,
 * and only then hands the phone to the child.
 */
enum class OnboardingStep { LANGUAGE, PARENT_GATE, SIGN_IN, CHILD, HANDOFF }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.LANGUAGE,
    val lang: String = "uz",
    val age: String = "2-4",
    val childName: String = "",
)

class OnboardingViewModel : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun setLang(lang: String) { _state.update { it.copy(lang = lang, step = OnboardingStep.PARENT_GATE) } }

    /** A parent who is already signed in (e.g. after a reinstall) doesn't see the sign-in step again. */
    fun parentVerified(alreadySignedIn: Boolean) {
        _state.update { it.copy(step = if (alreadySignedIn) OnboardingStep.CHILD else OnboardingStep.SIGN_IN) }
    }

    /** Called both after a successful sign-in and when the parent taps "later". */
    fun signInFinished() { _state.update { it.copy(step = OnboardingStep.CHILD) } }

    fun submitChild(name: String, age: String) {
        _state.update { it.copy(childName = name.trim(), age = age, step = OnboardingStep.HANDOFF) }
    }

    fun back() {
        _state.update {
            val prev = when (it.step) {
                OnboardingStep.LANGUAGE, OnboardingStep.PARENT_GATE -> OnboardingStep.LANGUAGE
                OnboardingStep.SIGN_IN, OnboardingStep.CHILD -> OnboardingStep.PARENT_GATE
                OnboardingStep.HANDOFF -> OnboardingStep.CHILD
            }
            it.copy(step = prev)
        }
    }
}

/**
 * Neutral age screen (Google Play Families): asks for a birth year without hinting at the
 * answer that unlocks the parent steps.
 */
fun isAdultBirthYear(year: Int?, currentYear: Int): Boolean {
    if (year == null) return false
    val age = currentYear - year
    return age in 18..100
}
