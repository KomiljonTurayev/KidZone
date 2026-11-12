package uz.kidzone.app.ui.viewmodel

import uz.kidzone.app.arch.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uz.kidzone.app.agegate.AgeBracket

/**
 * First-launch setup: language → neutral age screen (Google Play Families Policy) → sign in (or
 * skip) → child profile → handoff.
 *
 * The age screen is answered once ([uz.kidzone.app.agegate.AgeGate]) and never shown again: going
 * back from a later step returns to the language picker, and the age step is then skipped. A child
 * ([AgeBracket.CHILD]) never sees the sign-in step.
 */
enum class OnboardingStep { LANGUAGE, AGE, SIGN_IN, CHILD, HANDOFF }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.LANGUAGE,
    val lang: String = "uz",
    val age: String = "2-4",
    val childName: String = "",
    /** Answer of the neutral age screen; `null` until it is answered. */
    val bracket: AgeBracket? = null,
) {
    val isChild: Boolean get() = bracket == AgeBracket.CHILD
}

class OnboardingViewModel(
    /** Already-saved answer (e.g. after a reinstall that kept app data); skips the age step. */
    initialBracket: AgeBracket? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState(bracket = initialBracket))
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun setLang(lang: String, alreadySignedIn: Boolean = false) {
        _state.update {
            val next = it.bracket?.let { b -> stepAfterAge(b, alreadySignedIn) } ?: OnboardingStep.AGE
            it.copy(lang = lang, step = next)
        }
    }

    /** The neutral age screen was answered (and saved). */
    fun ageAnswered(bracket: AgeBracket, alreadySignedIn: Boolean) {
        _state.update { it.copy(bracket = bracket, step = stepAfterAge(bracket, alreadySignedIn)) }
    }

    /** Called both after a successful sign-in and when the parent taps "later". */
    fun signInFinished() { _state.update { it.copy(step = OnboardingStep.CHILD) } }

    fun submitChild(name: String, age: String) {
        _state.update { it.copy(childName = name.trim(), age = age, step = OnboardingStep.HANDOFF) }
    }

    fun back() {
        _state.update {
            val prev = when (it.step) {
                OnboardingStep.LANGUAGE, OnboardingStep.AGE -> OnboardingStep.LANGUAGE
                // The age answer is final, so there is no way back to the age screen.
                OnboardingStep.SIGN_IN, OnboardingStep.CHILD -> OnboardingStep.LANGUAGE
                OnboardingStep.HANDOFF -> OnboardingStep.CHILD
            }
            it.copy(step = prev)
        }
    }

    private fun stepAfterAge(bracket: AgeBracket, alreadySignedIn: Boolean): OnboardingStep = when {
        // Families Policy: no sign-in (and so no accounts, AI or purchases) for a child.
        bracket == AgeBracket.CHILD -> OnboardingStep.CHILD
        // A parent who is already signed in (e.g. after a reinstall) doesn't see sign-in again.
        alreadySignedIn -> OnboardingStep.CHILD
        else -> OnboardingStep.SIGN_IN
    }
}
