package uz.kidzone.app.auth

import kotlinx.coroutines.delay
import java.util.Base64
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

    /** Wakes the backend while the parent is still reading the sign-in screen. */
    suspend fun prepare() = api.warmUp()

    suspend fun start(): TelegramLoginStart {
        auth.ensureGuestSession()
        return api.start(requireIdToken())
    }

    /** @return the Telegram display name of the account that signed in */
    suspend fun awaitConfirmation(loginId: String): String? {
        val confirmed = awaitConfirmed(loginId)
        auth.signInWithCustomToken(confirmed.customToken)
        return confirmed.displayName
    }

    /**
     * Like [awaitConfirmation], but only checks that the Telegram account that pressed Start is
     * the one already signed in. It never signs in, so a different account can't take over.
     */
    suspend fun awaitReconfirmation(loginId: String) {
        val confirmed = awaitConfirmed(loginId)
        if (customTokenUid(confirmed.customToken) != auth.state.value.uid) {
            throw AuthException(AuthException.Reason.WRONG_ACCOUNT)
        }
    }

    private suspend fun awaitConfirmed(loginId: String): TelegramLoginStatus.Confirmed {
        // The session's token is fetched once: it stays valid far longer than [timeout].
        val idToken = requireIdToken()
        return withTimeoutOrNull(timeout) { pollUntilConfirmed(idToken, loginId) }
            ?: throw AuthException(AuthException.Reason.EXPIRED)
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

private val UID_CLAIM = Regex("\"uid\"\\s*:\\s*\"([^\"]+)\"")

/**
 * The `uid` claim of a Firebase custom token (a JWT). Read without verifying the signature: the
 * token came from our backend over TLS, and is only compared against the current session.
 */
internal fun customTokenUid(customToken: String): String? = runCatching {
    val payload = String(Base64.getUrlDecoder().decode(customToken.split('.')[1]))
    UID_CLAIM.find(payload)?.groupValues?.get(1)
}.getOrNull()
