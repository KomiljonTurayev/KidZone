package uz.kidzone.app.ui.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uz.kidzone.app.FirebaseManager
import uz.kidzone.app.FirestoreSync
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
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val loginError: String? = null,
)

class DashboardViewModel(
    private val statsManager: ParentalStatsManager,
    private val prefs: SharedPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        refresh()
        initFirebase()
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

    fun initFirebase() {
        val user = FirebaseManager.getInstance().getCurrentUser()
        _state.update { it.copy(firebaseUid = user?.uid, firebaseEmail = user?.email) }
    }

    fun login(email: String, password: String, onDone: (Boolean) -> Unit) {
        _state.update { it.copy(isSyncing = true, loginError = null) }
        FirebaseManager.getInstance().signInWithEmail(email, password, object : FirebaseManager.AuthCallback {
            override fun onSuccess(user: FirebaseUser) {
                _state.update { it.copy(
                    firebaseUid = user.uid,
                    firebaseEmail = user.email,
                    isSyncing = false,
                    loginError = null,
                ) }
                onDone(true)
            }
            override fun onError(message: String) {
                _state.update { it.copy(loginError = message, isSyncing = false) }
                onDone(false)
            }
        })
    }

    fun register(email: String, password: String, onDone: (Boolean) -> Unit) {
        _state.update { it.copy(isSyncing = true, loginError = null) }
        FirebaseManager.getInstance().createAccountWithEmail(email, password, object : FirebaseManager.AuthCallback {
            override fun onSuccess(user: FirebaseUser) {
                _state.update { it.copy(
                    firebaseUid = user.uid,
                    firebaseEmail = user.email,
                    isSyncing = false,
                    loginError = null,
                ) }
                onDone(true)
            }
            override fun onError(message: String) {
                _state.update { it.copy(loginError = message, isSyncing = false) }
                onDone(false)
            }
        })
    }

    fun logout() {
        FirebaseManager.getInstance().signOut()
        _state.update { it.copy(
            firebaseUid = null,
            firebaseEmail = null,
            lastSyncTime = null,
            loginError = null,
        ) }
    }

    fun syncNow() {
        val uid = _state.value.firebaseUid ?: return
        val email = _state.value.firebaseEmail
        val ageGroup = prefs.getString("kz_age", "2-4") ?: "2-4"
        _state.update { it.copy(isSyncing = true) }
        val sync = FirestoreSync.getInstance()
        sync.syncUserProfile(uid, null, email, ageGroup)
        val minutes = statsManager.getTodayMinutes().toLong()
        val games = statsManager.getTodayGames().associateWith { "played" }
        sync.recordSession(uid, minutes, games, false)
        _state.update { it.copy(isSyncing = false, lastSyncTime = "Az vaqt oldin") }
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
