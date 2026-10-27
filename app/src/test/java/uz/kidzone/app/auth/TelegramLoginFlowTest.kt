package uz.kidzone.app.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramLoginFlowTest {

    private val auth = FakeAuthRepository()

    @Test
    fun `start asks the backend for a link as the current guest`() = runTest {
        val api = ScriptedApi()

        val start = flow(api).start()

        assertEquals("https://t.me/KidZoneBot?start=login-1", start.botUrl)
        assertEquals(listOf("guest-id-token"), api.startTokens)
    }

    @Test
    fun `start creates a guest session first when there is none`() = runTest {
        val fresh = FakeAuthRepository(initial = AccountState.Unavailable)

        TelegramLoginFlow(ScriptedApi(), fresh).start()

        assertEquals(AccountState.Guest("guest-uid"), fresh.state.value)
    }

    @Test
    fun `polls until confirmed then signs in with the custom token`() = runTest {
        val api = ScriptedApi(
            TelegramLoginStatus.Pending,
            TelegramLoginStatus.Pending,
            TelegramLoginStatus.Confirmed("custom-token", "Aziz"),
        )

        val name = flow(api).awaitConfirmation("login-1")

        assertEquals("Aziz", name)
        assertEquals(listOf("custom-token"), auth.customTokensUsed)
        assertTrue(auth.state.value.isSignedIn)
        assertEquals(3, api.statusCalls)
        assertEquals(4_000L, currentTime) // two 2-second waits
    }

    @Test
    fun `network hiccups while the parent is in telegram are retried`() = runTest {
        val api = ScriptedApi(
            AuthException(AuthException.Reason.NETWORK),
            TelegramLoginStatus.Confirmed("custom-token", null),
        )

        flow(api).awaitConfirmation("login-1")

        assertEquals(listOf("custom-token"), auth.customTokensUsed)
    }

    @Test
    fun `backend reporting expiry stops the flow`() = runTest {
        val api = ScriptedApi(TelegramLoginStatus.Pending, TelegramLoginStatus.Expired)

        val error = runCatching { flow(api).awaitConfirmation("login-1") }.exceptionOrNull() as AuthException

        assertEquals(AuthException.Reason.EXPIRED, error.reason)
        assertFalse(auth.state.value.isSignedIn)
    }

    @Test
    fun `gives up after the timeout`() = runTest {
        val api = ScriptedApi(repeatLast = TelegramLoginStatus.Pending)

        val error = runCatching { flow(api).awaitConfirmation("login-1") }.exceptionOrNull() as AuthException

        assertEquals(AuthException.Reason.EXPIRED, error.reason)
        assertEquals(5.minutes.inWholeMilliseconds, currentTime)
        assertTrue(auth.customTokensUsed.isEmpty())
    }

    @Test
    fun `non-network errors are not retried`() = runTest {
        val api = ScriptedApi(AuthException(AuthException.Reason.NOT_CONFIGURED))

        val error = runCatching { flow(api).awaitConfirmation("login-1") }.exceptionOrNull() as AuthException

        assertEquals(AuthException.Reason.NOT_CONFIGURED, error.reason)
        assertEquals(1, api.statusCalls)
    }

    @Test
    fun `a guest without a token is offline`() {
        auth.idToken = null

        val error = assertThrows(AuthException::class.java) {
            kotlinx.coroutines.runBlocking { flow(ScriptedApi()).start() }
        }
        assertEquals(AuthException.Reason.NETWORK, error.reason)
    }

    private fun flow(api: TelegramAuthApi) = TelegramLoginFlow(api, auth, pollInterval = 2.seconds, timeout = 5.minutes)

    /** Answers status calls from a script: a [TelegramLoginStatus] is returned, an exception thrown. */
    private class ScriptedApi(
        vararg script: Any,
        private val repeatLast: Any? = null,
    ) : TelegramAuthApi {
        private val remaining = ArrayDeque(script.toList())
        val startTokens = mutableListOf<String>()
        var statusCalls = 0

        override suspend fun start(idToken: String): TelegramLoginStart {
            startTokens += idToken
            return TelegramLoginStart("login-1", "https://t.me/KidZoneBot?start=login-1")
        }

        override suspend fun status(idToken: String, loginId: String): TelegramLoginStatus {
            statusCalls++
            return when (val next = remaining.removeFirstOrNull() ?: repeatLast) {
                is TelegramLoginStatus -> next
                is Exception -> throw next
                else -> error("script exhausted")
            }
        }
    }
}
