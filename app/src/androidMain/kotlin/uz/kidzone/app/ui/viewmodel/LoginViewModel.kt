package uz.kidzone.app.ui.viewmodel

import android.app.Activity
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.kidzone.app.arch.ViewModel
import uz.kidzone.app.auth.AuthException
import uz.kidzone.app.auth.AuthRepository
import uz.kidzone.app.auth.GoogleIdTokenSource
import uz.kidzone.app.auth.TelegramLoginFlow
import uz.kidzone.app.auth.userMessage

@Immutable
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object SigningInWithGoogle : LoginUiState
    data object StartingTelegram : LoginUiState
    /** The bot link is open; waiting for the parent to press Start in Telegram. */
    data class WaitingForTelegram(val botUrl: String) : LoginUiState
    data object SignedIn : LoginUiState
    data class Failed(val message: String) : LoginUiState
}

val LoginUiState.isBusy: Boolean
    get() = this is LoginUiState.SigningInWithGoogle ||
        this is LoginUiState.StartingTelegram ||
        this is LoginUiState.WaitingForTelegram

class LoginViewModel(
    private val auth: AuthRepository,
    private val telegramLogin: TelegramLoginFlow,
    private val googleSignIn: GoogleIdTokenSource,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private var attempt: Job? = null

    fun signInWithGoogle(activity: Activity) = launchAttempt(LoginUiState.SigningInWithGoogle) {
        auth.signInWithGoogle(googleSignIn.requestIdToken(activity))
    }

    /** @param openUrl opens the bot link (Telegram app, or the browser if it isn't installed) */
    fun signInWithTelegram(openUrl: (String) -> Unit) = launchAttempt(LoginUiState.StartingTelegram) {
        val login = telegramLogin.start()
        _state.value = LoginUiState.WaitingForTelegram(login.botUrl)
        openUrl(login.botUrl)
        telegramLogin.awaitConfirmation(login.loginId)
    }

    fun cancel() {
        attempt?.cancel()
        _state.value = LoginUiState.Idle
    }

    private fun launchAttempt(busy: LoginUiState, signIn: suspend () -> Unit) {
        if (_state.value.isBusy) return
        _state.value = busy
        attempt = viewModelScope.launch {
            _state.value = try {
                signIn()
                LoginUiState.SignedIn
            } catch (e: AuthException) {
                e.reason.userMessage()?.let { LoginUiState.Failed(it) } ?: LoginUiState.Idle
            }
        }
    }
}
