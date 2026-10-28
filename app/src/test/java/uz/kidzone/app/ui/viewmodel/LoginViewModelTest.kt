package uz.kidzone.app.ui.viewmodel

import android.app.Activity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import uz.kidzone.app.auth.AccountState
import uz.kidzone.app.auth.AuthException
import uz.kidzone.app.auth.AuthProvider
import uz.kidzone.app.auth.FakeAuthRepository
import uz.kidzone.app.auth.GoogleIdTokenSource
import uz.kidzone.app.auth.TelegramAuthApi
import uz.kidzone.app.auth.TelegramLoginFlow
import uz.kidzone.app.auth.TelegramLoginStart
import uz.kidzone.app.auth.TelegramLoginStatus
import uz.kidzone.app.auth.isSignedIn

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val activity = mock(Activity::class.java)
    private val auth = FakeAuthRepository()
    private val telegramApi = GatedTelegramApi()
    private var googleResult: () -> String = { "google-id-token" }
    private lateinit var vm: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = LoginViewModel(
            auth = auth,
            telegramLogin = TelegramLoginFlow(telegramApi, auth),
            googleSignIn = GoogleIdTokenSource { googleResult() },
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening the sign-in screen wakes the backend once`() = runTest(dispatcher) {
        advanceUntilIdle()

        assertEquals(1, telegramApi.warmUps)
        assertEquals(LoginUiState.Idle, vm.state.value)
    }

    @Test
    fun `google sign-in exchanges the google token and reports success`() = runTest(dispatcher) {
        vm.signInWithGoogle(activity)
        assertEquals(LoginUiState.SigningInWithGoogle, vm.state.value)

        advanceUntilIdle()

        assertEquals(LoginUiState.SignedIn, vm.state.value)
        assertEquals(listOf("google-id-token"), auth.googleTokensUsed)
    }

    @Test
    fun `closing the google sheet quietly returns to idle`() = runTest(dispatcher) {
        googleResult = { throw AuthException(AuthException.Reason.CANCELLED) }

        vm.signInWithGoogle(activity)
        advanceUntilIdle()

        assertEquals(LoginUiState.Idle, vm.state.value)
    }

    @Test
    fun `failures show a parent-friendly message`() = runTest(dispatcher) {
        auth.failNextSignInWith = AuthException(AuthException.Reason.NETWORK)

        vm.signInWithGoogle(activity)
        advanceUntilIdle()

        assertEquals(LoginUiState.Failed(AuthException.Reason.NETWORK), vm.state.value)
    }

    @Test
    fun `telegram sign-in opens the bot link and waits for confirmation`() = runTest(dispatcher) {
        val opened = mutableListOf<String>()

        vm.signInWithTelegram { opened += it }
        runCurrent() // start + first (pending) poll; now parked on the poll delay

        assertEquals(listOf("https://t.me/KidZoneBot?start=l1"), opened)
        assertEquals(LoginUiState.WaitingForTelegram("https://t.me/KidZoneBot?start=l1"), vm.state.value)

        telegramApi.confirm()
        advanceTimeBy(2_001)
        runCurrent()

        assertEquals(LoginUiState.SignedIn, vm.state.value)
        assertTrue(auth.state.value.isSignedIn)
    }

    @Test
    fun `cancel stops waiting for telegram`() = runTest(dispatcher) {
        vm.signInWithTelegram {}
        runCurrent()

        vm.cancel()
        telegramApi.confirm()
        advanceUntilIdle()

        assertEquals(LoginUiState.Idle, vm.state.value)
        assertTrue(auth.customTokensUsed.isEmpty())
    }

    @Test
    fun `a second tap while busy is ignored`() = runTest(dispatcher) {
        vm.signInWithTelegram {}
        vm.signInWithGoogle(activity)
        runCurrent()

        assertTrue(auth.googleTokensUsed.isEmpty())
        vm.cancel()
    }

    @Test
    fun `in confirm-only mode google re-checks the signed-in parent instead of signing in`() = runTest(dispatcher) {
        auth.emit(AccountState.SignedIn("parent-uid", "Parent", AuthProvider.GOOGLE))
        val confirm = confirmOnlyViewModel()

        confirm.signInWithGoogle(activity)
        advanceUntilIdle()

        assertEquals(LoginUiState.SignedIn, confirm.state.value)
        assertEquals(listOf("google-id-token"), auth.googleReauthTokens)
        assertTrue(auth.googleTokensUsed.isEmpty())
    }

    @Test
    fun `in confirm-only mode another google account is refused`() = runTest(dispatcher) {
        auth.emit(AccountState.SignedIn("parent-uid", "Parent", AuthProvider.GOOGLE))
        auth.reauthGoogleUid = "someone-else"
        val confirm = confirmOnlyViewModel()

        confirm.signInWithGoogle(activity)
        advanceUntilIdle()

        assertEquals(LoginUiState.Failed(AuthException.Reason.WRONG_ACCOUNT), confirm.state.value)
        assertEquals("parent-uid", auth.state.value.uid)
    }

    private fun confirmOnlyViewModel() = LoginViewModel(
        auth = auth,
        telegramLogin = TelegramLoginFlow(telegramApi, auth),
        googleSignIn = GoogleIdTokenSource { googleResult() },
        confirmOnly = true,
    )

    /** Stays PENDING until [confirm] is called. */
    private class GatedTelegramApi : TelegramAuthApi {
        private val confirmed = CompletableDeferred<Unit>()
        var warmUps = 0

        override suspend fun warmUp() {
            warmUps++
        }

        fun confirm() {
            confirmed.complete(Unit)
        }

        override suspend fun start(idToken: String) = TelegramLoginStart("l1", "https://t.me/KidZoneBot?start=l1")

        override suspend fun status(idToken: String, loginId: String): TelegramLoginStatus =
            if (confirmed.isCompleted) TelegramLoginStatus.Confirmed("custom", "Aziz") else TelegramLoginStatus.Pending
    }
}
