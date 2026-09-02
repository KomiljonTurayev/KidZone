package uz.kidzone.app.ui.viewmodel

import uz.kidzone.app.arch.AppPreferences
import uz.kidzone.app.arch.ViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import uz.kidzone.app.FirebaseManager
import uz.kidzone.app.FirestoreSync
import uz.kidzone.app.ParentalStatsManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

import androidx.compose.runtime.Immutable

@Immutable
data class DashboardState(
    val todayMinutes: Int = 0,
    val weeklyMinutes: List<Int> = List(7) { 0 },
    val todayGames: List<String> = emptyList(),
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
    private val prefs: AppPreferences,
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
                age = prefs.getString("kz_age", "2-4"),
                pushEnabled = prefs.getBoolean("kz_push_enabled", true),
            )
        }
    }

    private fun initFirebase() {
        val user = FirebaseManager.getInstance().getCurrentUser()
        _state.update { it.copy(firebaseUid = user?.uid, firebaseEmail = user?.email) }
    }

    fun login(email: String, password: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, loginError = null) }
            try {
                // "Callback hell" o'rniga suspend funksiya orqali kutamiz
                val user = suspendSignIn(email, password)
                _state.update {
                    it.copy(
                        firebaseUid = user.uid,
                        firebaseEmail = user.email,
                        isSyncing = false,
                        loginError = null,
                    )
                }
                onDone(true)
            } catch (e: Exception) {
                _state.update { it.copy(loginError = e.message, isSyncing = false) }
                onDone(false)
            }
        }
    }

    fun register(email: String, password: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, loginError = null) }
            try {
                val user = suspendCreateAccount(email, password)
                _state.update {
                    it.copy(
                        firebaseUid = user.uid,
                        firebaseEmail = user.email,
                        isSyncing = false,
                        loginError = null,
                    )
                }
                onDone(true)
            } catch (e: Exception) {
                _state.update { it.copy(loginError = e.message, isSyncing = false) }
                onDone(false)
            }
        }
    }

    fun logout() {
        FirebaseManager.getInstance().signOut()
        _state.update {
            it.copy(
                firebaseUid = null,
                firebaseEmail = null,
                lastSyncTime = null,
                loginError = null,
                isSyncing = false,
            )
        }
    }

    fun syncNow() {
        val uid = _state.value.firebaseUid ?: return
        val email = _state.value.firebaseEmail
        val ageGroup = prefs.getString("kz_age", "2-4") ?: "2-4"

        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            
            // Tarmoq so'rovlarini asosiy Ui Thread ni qotirmasligi uchun IO ga o'tkazamiz
            withContext(Dispatchers.IO) {
                val sync = FirestoreSync.getInstance()
                sync.syncUserProfile(uid, null, email, ageGroup)
                val minutes = statsManager.getTodayMinutes().toLong()
                val games = statsManager.getTodayGames().associateWith { "played" }
                sync.recordSession(uid, minutes, games, false)
                
                // Fire-and-forget funksiyalar tez ishlab ketgani uchun UI (Progress Indicator) 
                // go'zal ishlashi uchun qisqacha sun'iy kutish qo'shildi
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

    // --- Suspend Wrappers (Callbacklarni Coroutines'ga aylantirish) ---

    fun deleteAccount(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, loginError = null) }
            try {
                suspendDeleteAccount()
                _state.update {
                    it.copy(
                        firebaseUid = null,
                        firebaseEmail = null,
                        isSyncing = false,
                        loginError = null,
                    )
                }
                onDone(true)
            } catch (e: Exception) {
                _state.update { it.copy(loginError = e.message, isSyncing = false) }
                onDone(false)
            }
        }
    }

    private suspend fun suspendSignIn(email: String, password: String): FirebaseUser {
        return suspendCancellableCoroutine { cont ->
            FirebaseManager.getInstance().signInWithEmail(email, password, object : FirebaseManager.AuthCallback {
                override fun onSuccess(user: FirebaseUser) {
                    cont.resume(user)
                }
                override fun onError(message: String) {
                    cont.resumeWithException(Exception(message))
                }
            })
        }
    }

    private suspend fun suspendCreateAccount(email: String, password: String): FirebaseUser {
        return suspendCancellableCoroutine { cont ->
            FirebaseManager.getInstance().createAccountWithEmail(email, password, object : FirebaseManager.AuthCallback {
                override fun onSuccess(user: FirebaseUser) {
                    cont.resume(user)
                }
                override fun onError(message: String) {
                    cont.resumeWithException(Exception(message))
                }
            })
        }
    }

    private suspend fun suspendDeleteAccount() {
        return suspendCancellableCoroutine { cont ->
            FirebaseManager.getInstance().deleteAccount(object : FirebaseManager.DeleteCallback {
                override fun onSuccess() {
                    cont.resume(Unit)
                }
                override fun onError(message: String) {
                    cont.resumeWithException(Exception(message))
                }
            })
        }
    }
}
