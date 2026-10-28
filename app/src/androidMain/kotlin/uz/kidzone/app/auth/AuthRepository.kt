package uz.kidzone.app.auth

import kotlinx.coroutines.flow.StateFlow

/** Supplies the Firebase ID token the KidZone backend expects as `Authorization: Bearer …`. */
fun interface IdTokenProvider {
    /** null when there is no session or the token could not be fetched (e.g. offline). */
    suspend fun idToken(): String?
}

interface AuthRepository : IdTokenProvider {

    val state: StateFlow<AccountState>

    /** Starts an anonymous guest session if nobody is signed in yet. */
    suspend fun ensureGuestSession()

    /**
     * Signs in with a Google ID token. A guest is upgraded in place (same uid, so their data
     * stays); if that Google account already has a KidZone account, it is signed into instead.
     */
    suspend fun signInWithGoogle(googleIdToken: String)

    /** Signs in with a Firebase custom token minted by the backend (Telegram login). */
    suspend fun signInWithCustomToken(customToken: String)

    /**
     * Proves the person holding the phone controls the signed-in account, without ever switching
     * accounts: a different Google account fails with [AuthException.Reason.WRONG_ACCOUNT].
     */
    suspend fun reauthenticateWithGoogle(googleIdToken: String)

    /** Signs out and immediately starts a fresh guest session, so the app keeps working. */
    suspend fun signOut()

    /** Permanently deletes the signed-in account, then continues as a fresh guest. */
    suspend fun deleteAccount()
}
