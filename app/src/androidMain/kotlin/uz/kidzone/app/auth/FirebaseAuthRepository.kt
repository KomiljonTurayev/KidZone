package uz.kidzone.app.auth

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/** [AuthRepository] on Firebase Auth. [auth] is null when the build has no google-services.json. */
class FirebaseAuthRepository(private val auth: FirebaseAuth?) : AuthRepository {

    private val _state = MutableStateFlow(stateOf(auth?.currentUser))
    override val state: StateFlow<AccountState> = _state.asStateFlow()

    init {
        auth?.addAuthStateListener { publish(it.currentUser) }
    }

    override suspend fun idToken(): String? = try {
        withTimeoutOrNull(NETWORK_TIMEOUT) { auth?.currentUser?.getIdToken(false)?.await()?.token }
    } catch (e: Exception) {
        Log.w(TAG, "getIdToken failed: ${e.message}")
        null
    }

    override suspend fun ensureGuestSession() {
        val auth = auth ?: return
        if (auth.currentUser == null) call { auth.signInAnonymously().await() }
    }

    override suspend fun signInWithGoogle(googleIdToken: String) {
        val auth = requireAuth()
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val guest = auth.currentUser?.takeIf { it.isAnonymous }
        if (guest != null) {
            try {
                call { guest.linkWithCredential(credential).await() }
                // Linking keeps the same user object, so no auth-state event fires.
                publish(auth.currentUser)
                return
            } catch (e: AuthException) {
                if (e.cause !is FirebaseAuthUserCollisionException) throw e
                // This Google account already has a KidZone account: sign into it instead.
            }
        }
        call { auth.signInWithCredential(credential).await() }
    }

    override suspend fun signInWithCustomToken(customToken: String) {
        val auth = requireAuth()
        call { auth.signInWithCustomToken(customToken).await() }
    }

    override suspend fun reauthenticateWithGoogle(googleIdToken: String) {
        val user = requireAuth().currentUser?.takeUnless { it.isAnonymous }
            ?: throw AuthException(AuthException.Reason.NO_SESSION)
        call { user.reauthenticate(GoogleAuthProvider.getCredential(googleIdToken, null)).await() }
    }

    override suspend fun signOut() {
        val auth = requireAuth()
        auth.signOut()
        ensureGuestSession()
    }

    override suspend fun deleteAccount() {
        val auth = requireAuth()
        val user = auth.currentUser ?: throw AuthException(AuthException.Reason.NO_SESSION)
        call { user.delete().await() }
        ensureGuestSession()
    }

    private fun publish(user: FirebaseUser?) {
        _state.value = stateOf(user)
    }

    private fun requireAuth(): FirebaseAuth = auth ?: throw AuthException(AuthException.Reason.NOT_CONFIGURED)

    // Offline, Firebase Auth keeps retrying for about a minute before failing; give up sooner
    // so the parent sees "check your internet" instead of a spinner that never ends.
    private suspend fun <T> call(block: suspend () -> T): T = try {
        withTimeout(NETWORK_TIMEOUT) { block() }
    } catch (e: TimeoutCancellationException) {
        throw AuthException(AuthException.Reason.NETWORK, e)
    } catch (e: CancellationException) {
        throw e // the caller cancelled (e.g. the parent pressed "Bekor qilish"): not a failure
    } catch (e: AuthException) {
        throw e
    } catch (e: FirebaseNetworkException) {
        throw AuthException(AuthException.Reason.NETWORK, e)
    } catch (e: FirebaseAuthRecentLoginRequiredException) {
        throw AuthException(AuthException.Reason.REAUTH_REQUIRED, e)
    } catch (e: FirebaseAuthInvalidUserException) {
        val reason = if (e.errorCode == "ERROR_USER_MISMATCH") AuthException.Reason.WRONG_ACCOUNT else AuthException.Reason.FAILED
        throw AuthException(reason, e)
    } catch (e: Exception) {
        Log.w(TAG, "Firebase Auth call failed: ${e.javaClass.simpleName}: ${e.message}")
        throw AuthException(AuthException.Reason.FAILED, e)
    }

    private companion object {
        const val TAG = "FirebaseAuthRepository"
        val NETWORK_TIMEOUT = 15.seconds

        fun stateOf(user: FirebaseUser?): AccountState = accountStateOf(
            uid = user?.uid,
            isAnonymous = user?.isAnonymous ?: false,
            providerIds = user?.providerData?.map { it.providerId }.orEmpty(),
            displayName = user?.displayName,
            email = user?.email,
        )
    }
}
