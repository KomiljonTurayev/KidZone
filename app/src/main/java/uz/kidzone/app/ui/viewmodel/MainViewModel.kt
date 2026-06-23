package uz.kidzone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PromoBannerData(val title: String, val body: String, val url: String)

data class MainUiState(
    val language: String = "uz",
    val age: String = "2-4",
    val inGame: Boolean = false,
    val showExitDialog: Boolean = false,
    val isExitFromGame: Boolean = false,
    val promoBanner: PromoBannerData? = null,
    val isLocked: Boolean = false,
    val bannerVisible: Boolean = true,
    val bannerLoaded: Boolean = false,
)

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    fun setLanguage(lang: String) { _state.update { it.copy(language = lang) } }
    fun setAge(age: String) { _state.update { it.copy(age = age) } }
    fun setInGame(inGame: Boolean) { _state.update { it.copy(inGame = inGame) } }
    fun showExitDialog(fromGame: Boolean) { _state.update { it.copy(showExitDialog = true, isExitFromGame = fromGame) } }
    fun dismissExitDialog() { _state.update { it.copy(showExitDialog = false) } }
    fun setPromoBanner(data: PromoBannerData?) { _state.update { it.copy(promoBanner = data) } }
    fun showLock() { _state.update { it.copy(isLocked = true) } }
    fun hideLock() { _state.update { it.copy(isLocked = false) } }
    fun setBannerVisible(visible: Boolean) { _state.update { it.copy(bannerVisible = visible) } }
    fun setBannerLoaded(loaded: Boolean) { _state.update { it.copy(bannerLoaded = loaded) } }
}
