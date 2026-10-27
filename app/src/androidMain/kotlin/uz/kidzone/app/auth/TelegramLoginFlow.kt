package uz.kidzone.app.auth

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * "Sign in with Telegram" from the app's side:
 * 1. [start] asks the backend for a one-time bot link (as the current guest);
 * 2. the caller opens it, the parent presses Start in the KidZone bot;
 * 3. [awaitConfirmation] polls until the backend hands back a Firebase custom token and
 *    signs in with it.
 */
class TelegramLoginFlow(
    private val api: TelegramAuthApi,
    private val auth: AuthRepository,
    private val pollInterval: Duration = 2.seconds,
    private val timeout: Duration = 5.minutes,
) {

    suspend fun start(): TelegramLoginStart {
        auth.ensureGuestSession()
        return api.start(requireIdToken())
    }

    /** @return the Telegram display name of the account that signed in */
    suspend fun awaitConfirmation(loginId: String): String? {
        // The guest's token is fetched once: it stays valid far longer than [timeout].
        val idToken = requireIdToken()
        val confirmed = withTimeoutOrNull(timeout) { pollUntilConfirmed(idToken, loginId) }
            ?: throw AuthException(AuthException.Reason.EXPIRED)
        auth.signInWithCustomToken(confirmed.customToken)
        return confirmed.displayName
    }

    private suspend fun pollUntilConfirmed(idToken: String, loginId: String): TelegramLoginStatus.Confirmed {
        while (true) {
            when (val status = pollOnce(idToken, loginId)) {
                is TelegramLoginStatus.Confirmed -> return status
                TelegramLoginStatus.Expired -> throw AuthException(AuthException.Reason.EXPIRED)
                TelegramLoginStatus.Pending, null -> delay(pollInterval)
            }
        }
    }

    /** A dropped connection while the parent is in Telegram is normal; just poll again. */
    private suspend fun pollOnce(idToken: String, loginId: String): TelegramLoginStatus? = try {
        api.status(idToken, loginId)
    } catch (e: AuthException) {
        if (e.reason != AuthException.Reason.NETWORK) throw e
        null
    }

    // A guest session exists by now, so a missing token almost always means no connectivity.
    private suspend fun requireIdToken(): String =
        auth.idToken() ?: throw AuthException(
            if (auth.state.value.uid == null) AuthException.Reason.NO_SESSION else AuthException.Reason.NETWORK,
        )
}
