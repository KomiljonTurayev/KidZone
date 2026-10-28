package uz.kidzone.app.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory [AuthRepository]: starts as a guest and records what was asked of it. */
class FakeAuthRepository(
    initial: AccountState = AccountState.Guest("guest-uid"),
) : AuthRepository {

    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<AccountState> = _state

    var idToken: String? = "guest-id-token"
    var failNextSignInWith: AuthException? = null
    val customTokensUsed = mutableListOf<String>()
    val googleTokensUsed = mutableListOf<String>()
    val googleReauthTokens = mutableListOf<String>()
    /** uid of the Google account the next re-authentication proves; null = the current one. */
    var reauthGoogleUid: String? = null
    var signOutCount = 0

    override suspend fun idToken(): String? = idToken

    override suspend fun ensureGuestSession() {
        if (_state.value == AccountState.Unavailable) _state.value = AccountState.Guest("guest-uid")
    }

    override suspend fun signInWithGoogle(googleIdToken: String) {
        failNextSignInWith?.let { failNextSignInWith = null; throw it }
        googleTokensUsed += googleIdToken
        _state.value = AccountState.SignedIn(_state.value.uid ?: "google-uid", "Parent", AuthProvider.GOOGLE)
    }

    override suspend fun reauthenticateWithGoogle(googleIdToken: String) {
        val current = _state.value as? AccountState.SignedIn ?: throw AuthException(AuthException.Reason.NO_SESSION)
        if ((reauthGoogleUid ?: current.uid) != current.uid) throw AuthException(AuthException.Reason.WRONG_ACCOUNT)
        googleReauthTokens += googleIdToken
    }

    override suspend fun signInWithCustomToken(customToken: String) {
        failNextSignInWith?.let { failNextSignInWith = null; throw it }
        customTokensUsed += customToken
        _state.value = AccountState.SignedIn("telegram:777", "Aziz", AuthProvider.TELEGRAM)
    }

    override suspend fun signOut() {
        signOutCount++
        _state.value = AccountState.Guest("new-guest-uid")
    }

    override suspend fun deleteAccount() {
        _state.value = AccountState.Guest("new-guest-uid")
    }

    fun emit(state: AccountState) {
        _state.value = state
    }
}
