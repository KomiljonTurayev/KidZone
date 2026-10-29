package uz.kidzone.app.ui.viewmodel

import uz.kidzone.app.arch.AppPreferences
import uz.kidzone.app.arch.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.kidzone.app.FamilyApiClient
import uz.kidzone.app.FirestoreSync
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.auth.AccountState
import uz.kidzone.app.auth.AuthException
import uz.kidzone.app.auth.AuthRepository
import uz.kidzone.app.auth.userMessage

import androidx.compose.runtime.Immutable

@Immutable
data class DashboardState(
    val todayMinutes: Int = 0,
    val weeklyMinutes: List<Int> = List(7) { 0 },
    val todayGames: List<String> = emptyList(),
    val age: String = "2-4",
    val pushEnabled: Boolean = true,
    val notifHistory: List<String> = emptyList(),
    val account: AccountState = AccountState.Unavailable,
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    /** Shown by the screen as [uz.kidzone.app.auth.userMessage] in the app language. */
    val accountError: AuthException.Reason? = null,
)

class DashboardViewModel(
    private val statsManager: ParentalStatsManager,
    private val prefs: AppPreferences,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState(account = auth.state.value))
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            auth.state.collect { account -> _state.update { it.copy(account = account) } }
        }
    }

    fun refresh() {
        _state.update {
            it.copy(
                todayMinutes = statsManager.getTodayMinutes(),
                weeklyMinutes = statsManager.getWeeklyMinutes().toList(),
                todayGames = statsManager.getTodayGames(),
                age = prefs.getString("kz_age", "2-4"),
                pushEnabled = prefs.getBoolean("kz_push_enabled", true),
            )
        }
    }

    /** Signing out returns the app to guest mode; the dashboard then asks to sign in again. */
    fun logout() = runAccountAction { auth.signOut() }

    // Family links go first: once the account is gone it can no longer ask the backend to
    // drop them. Best effort: if this call fails, the members keep reading the old profiles.
    fun deleteAccount(onDone: (Boolean) -> Unit) = runAccountAction(onDone) {
        FamilyApiClient.forgetMe()
        auth.deleteAccount()
    }

    fun syncNow() {
        val account = _state.value.account as? AccountState.SignedIn ?: return
        val ageGroup = prefs.getString("kz_age", "2-4")

        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            withContext(Dispatchers.IO) {
                val sync = FirestoreSync.getInstance()
                sync.syncUserProfile(account.uid, account.displayName, null, ageGroup)
                val minutes = statsManager.getTodayMinutes().toLong()
                val games = statsManager.getTodayGames().associateWith { "played" }
                sync.recordSession(account.uid, minutes, games, false)
                // The writes above are fire-and-forget; keep the spinner up long enough to read.
                delay(800)
            }
            _state.update { it.copy(isSyncing = false, lastSyncTime = "Hozirgina") }
        }
    }

    fun setAge(age: String) {
        prefs.putString("kz_age", age)
        _state.update { it.copy(age = age) }
    }

    fun setPushEnabled(enabled: Boolean) {
        prefs.putBoolean("kz_push_enabled", enabled)
        _state.update { it.copy(pushEnabled = enabled) }
    }

    private fun runAccountAction(onDone: (Boolean) -> Unit = {}, action: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, accountError = null) }
            val ok = try {
                action()
                true
            } catch (e: AuthException) {
                _state.update { it.copy(accountError = e.reason) }
                false
            }
            _state.update { it.copy(isSyncing = false, lastSyncTime = if (ok) null else it.lastSyncTime) }
            onDone(ok)
        }
    }
}
