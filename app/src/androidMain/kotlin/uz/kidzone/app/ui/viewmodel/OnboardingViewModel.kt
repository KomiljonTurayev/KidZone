package uz.kidzone.app.ui.viewmodel

import uz.kidzone.app.arch.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OnboardingState(
    val step: Int = 0,
    val lang: String = "uz",
    val age: String = "2-4",
    val childName: String = "",
)

class OnboardingViewModel : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun setLang(lang: String) { _state.update { it.copy(lang = lang, step = 1) } }
    fun setAge(age: String) { _state.update { it.copy(age = age) } }
    fun setChildName(name: String) { _state.update { it.copy(childName = name) } }
    fun nextStep() { _state.update { it.copy(step = it.step + 1) } }
    fun prevStep() { if (_state.value.step > 0) _state.update { it.copy(step = it.step - 1) } }
}
