package uz.kidzone.app.auth

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountStateTest {

    @Test
    fun `no firebase user means unavailable`() {
        assertEquals(AccountState.Unavailable, accountStateOf(null, false, emptyList(), null, null))
    }

    @Test
    fun `anonymous user is a guest`() {
        assertEquals(AccountState.Guest("g1"), accountStateOf("g1", true, emptyList(), null, null))
    }

    @Test
    fun `google account is detected from provider data`() {
        val state = accountStateOf("u1", false, listOf("firebase", "google.com"), "Malika", "m@gmail.com")

        assertEquals(AccountState.SignedIn("u1", "Malika", AuthProvider.GOOGLE), state)
    }

    @Test
    fun `telegram account is detected from the backend's uid prefix`() {
        val state = accountStateOf("telegram:777", false, listOf("firebase"), "Aziz", null)

        assertEquals(AccountState.SignedIn("telegram:777", "Aziz", AuthProvider.TELEGRAM), state)
    }

    @Test
    fun `blank display name falls back to email`() {
        val state = accountStateOf("u1", false, listOf("password"), "  ", "p@kidzone.uz") as AccountState.SignedIn

        assertEquals("p@kidzone.uz", state.displayName)
        assertEquals(AuthProvider.EMAIL, state.provider)
    }

    @Test
    fun `account-only features are locked for guests and open once signed in`() {
        val account = MutableStateFlow<AccountState>(AccountState.Guest("g1"))
        val gate = FeatureGate(account)

        AccountFeature.entries.forEach { assertFalse(gate.isAvailable(it)) }

        account.value = AccountState.SignedIn("u1", null, AuthProvider.GOOGLE)
        AccountFeature.entries.forEach { assertTrue(gate.isAvailable(it)) }
    }

    @Test
    fun `features outside the account-only set stay free for guests`() {
        val gate = FeatureGate(MutableStateFlow(AccountState.Guest("g1")), setOf(AccountFeature.PREMIUM))

        assertTrue(gate.isAvailable(AccountFeature.AI_COMPANION))
        assertFalse(gate.isAvailable(AccountFeature.PREMIUM))
    }
}
