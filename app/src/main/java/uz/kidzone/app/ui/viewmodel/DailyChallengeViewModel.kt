// app/src/main/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModel.kt
package uz.kidzone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.kidzone.app.data.DailyChallengeEntity
import uz.kidzone.app.data.DailyChallengeRepository
import uz.kidzone.app.data.StreakEntity

data class ChallengeState(
    val challenge: DailyChallengeEntity? = null,
    val streakCount: Int = 0,
    val isLoading: Boolean = true,
)

open class DailyChallengeViewModel(
    private val repository: DailyChallengeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChallengeState())
    val state: StateFlow<ChallengeState> = _state.asStateFlow()

    val allStreaks: StateFlow<List<StreakEntity>> = repository.getAllStreaks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var activeProfileId: String? = null

    open fun onProfileChanged(profileId: String) {
        if (activeProfileId == profileId) return
        activeProfileId = profileId
        loadChallenge(profileId)
    }

    private fun loadChallenge(profileId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val challenge = repository.getTodayChallenge(profileId)
            val streak = repository.getStreak(profileId)
            _state.update {
                ChallengeState(
                    challenge = challenge,
                    streakCount = streak.count,
                    isLoading = false,
                )
            }
        }
    }

    fun updateGamesList(json: String) {
        repository.updateGamesList(json)
        activeProfileId?.let { loadChallenge(it) }
    }

    fun onGameClosed(gameId: String) {
        val profileId = activeProfileId ?: return
        viewModelScope.launch {
            repository.markChallengeCompleted(profileId, gameId)
            val challenge = repository.getTodayChallenge(profileId)
            val streak = repository.getStreak(profileId)
            _state.update { it.copy(challenge = challenge, streakCount = streak.count) }
        }
    }
}

class DailyChallengeViewModelFactory(
    private val repository: DailyChallengeRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DailyChallengeViewModel(repository) as T
}
