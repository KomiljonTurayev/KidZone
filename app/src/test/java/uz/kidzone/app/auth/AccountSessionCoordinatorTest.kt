package uz.kidzone.app.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSessionCoordinatorTest {

    private val guest = AccountState.Guest("g1")
    private val parent = AccountState.SignedIn("g1", "Malika", AuthProvider.GOOGLE)

    @Test
    fun `listeners run once per distinct account and skip the unavailable state`() = runTest(UnconfinedTestDispatcher()) {
        val account = MutableStateFlow<AccountState>(AccountState.Unavailable)
        val seen = mutableListOf<AccountState>()
        AccountSessionCoordinator(account, listOf(AccountListener { seen += it })).start(backgroundScope)

        account.value = guest
        account.value = parent                               // guest upgraded in place (same uid)
        account.value = parent.copy(displayName = "Other")   // profile edit, same account
        account.value = AccountState.Guest("g2")             // signed out → fresh guest

        assertEquals(listOf(guest, parent, AccountState.Guest("g2")), seen)
    }

    @Test
    fun `a failing listener does not stop the others`() = runTest(UnconfinedTestDispatcher()) {
        val seen = mutableListOf<AccountState>()
        AccountSessionCoordinator(
            MutableStateFlow(guest),
            listOf(AccountListener { throw IllegalStateException("offline") }, AccountListener { seen += it }),
        ).start(backgroundScope)

        assertEquals(listOf<AccountState>(guest), seen)
    }
}
