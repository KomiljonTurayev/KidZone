package uz.kidzone.app.ui.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uz.kidzone.app.ParentalStatsManager

data class DashboardState(
    val todayMinutes: Int = 0,
    val weeklyMinutes: List<Int> = List(7) { 0 },
    val todayGames: List<String> = emptyList(),
    val timeLimitMinutes: Int = 0,
    val age: String = "2-4",
    val pushEnabled: Boolean = true,
    val notifHistory: List<String> = emptyList(),
    val firebaseUid: String? = null,
    val firebaseEmail: String? = null,
)

class DashboardViewModel(
    private val statsManager: ParentalStatsManager,
    private val prefs: SharedPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update {
            it.copy(
                todayMinutes = statsManager.getTodayMinutes(),
                weeklyMinutes = statsManager.getWeeklyMinutes().toList(),
                todayGames = statsManager.getTodayGames(),
                timeLimitMinutes = statsManager.getTimeLimitMinutes(),
                age = prefs.getString("kz_age", "2-4") ?: "2-4",
                pushEnabled = prefs.getBoolean("kz_push_enabled", true),
            )
        }
    }

    fun increaseLimit() {
        val current = statsManager.getTimeLimitMinutes()
        val next = if (current >= 180) 180 else current + 15
        statsManager.setTimeLimitMinutes(next)
        _state.update { it.copy(timeLimitMinutes = next) }
    }

    fun decreaseLimit() {
        val current = statsManager.getTimeLimitMinutes()
        val next = if (current <= 0) 0 else current - 15
        statsManager.setTimeLimitMinutes(next)
        _state.update { it.copy(timeLimitMinutes = next) }
    }

    fun setAge(age: String) {
        prefs.edit().putString("kz_age", age).apply()
        _state.update { it.copy(age = age) }
    }

    fun setPushEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("kz_push_enabled", enabled).apply()
        _state.update { it.copy(pushEnabled = enabled) }
    }
}
