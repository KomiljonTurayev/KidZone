// app/src/main/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModel.kt
package uz.kidzone.app.ui.viewmodel

import uz.kidzone.app.arch.ViewModel
import androidx.lifecycle.ViewModelProvider
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

import androidx.compose.runtime.Immutable

@Immutable
data class ChallengeState(
    val challenge: DailyChallengeEntity? = null,
    /** Today's three tasks (play, new level, stars); empty until the game list arrives. */
    val tasks: List<DailyChallengeEntity> = emptyList(),
    val streakCount: Int = 0,
    val isLoading: Boolean = true,
    val celebrateMilestone: Int? = null,
)

open class DailyChallengeViewModel(
    private val repository: DailyChallengeRepository,
    private val ageProvider: () -> String = { "5-7" },
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
            val tasks = repository.getTodayTasks(profileId, ageProvider()).orEmpty()
            val streak = repository.getStreak(profileId)
            if (activeProfileId != profileId) return@launch
            _state.update {
                ChallengeState(
                    challenge = tasks.firstOrNull(),
                    tasks = tasks,
                    streakCount = streak.count,
                    isLoading = false,
                )
            }
        }
    }

    fun gameLabel(id: String, lang: String): String? = repository.gameLabel(id, lang)

    fun gameIds(): List<String> = repository.gameIds()

    fun updateGamesList(json: String) {
        repository.updateGamesList(json)
        activeProfileId?.let { loadChallenge(it) }
    }

    fun onGameClosed(gameId: String) {
        val profileId = activeProfileId ?: return
        viewModelScope.launch {
            refreshAfter(profileId, repository.markChallengeCompleted(profileId, gameId))
        }
    }

    /** The child reached a new best level in some game. */
    fun onLevelUp() {
        val profileId = activeProfileId ?: return
        viewModelScope.launch { refreshAfter(profileId, repository.onLevelUp(profileId)) }
    }

    fun onStarsEarned(stars: Int) {
        val profileId = activeProfileId ?: return
        viewModelScope.launch { refreshAfter(profileId, repository.onStarsEarned(profileId, stars)) }
    }

    private suspend fun refreshAfter(profileId: String, milestone: Int?) {
        val tasks = repository.getTodayTasks(profileId, ageProvider()).orEmpty()
        val streak = repository.getStreak(profileId)
        if (activeProfileId != profileId) return
        _state.update {
            it.copy(
                challenge = tasks.firstOrNull(),
                tasks = tasks,
                streakCount = streak.count,
                celebrateMilestone = milestone ?: it.celebrateMilestone,
            )
        }
    }

    fun onCelebrationShown() {
        _state.update { it.copy(celebrateMilestone = null) }
    }
}

class DailyChallengeViewModelFactory(
    private val repository: DailyChallengeRepository,
    private val ageProvider: () -> String = { "5-7" },
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        DailyChallengeViewModel(repository, ageProvider) as T
}
