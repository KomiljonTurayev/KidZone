package uz.kidzone.app.auth

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Something that must react when the user behind the app changes (guest ↔ parent account). */
fun interface AccountListener {
    suspend fun onAccountChanged(account: AccountState)
}

/**
 * Runs every [AccountListener] once per distinct account, so per-account setup (profile
 * sync, push token, purchase identity) lives in one place instead of being repeated at
 * each call site that can change the user.
 */
class AccountSessionCoordinator(
    private val account: StateFlow<AccountState>,
    private val listeners: List<AccountListener>,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            account
                .filter { it !is AccountState.Unavailable }
                .distinctUntilChangedBy { it.uid to it.isSignedIn }
                .collect { current -> listeners.forEach { notify(it, current) } }
        }
    }

    // One failing listener (e.g. offline Firestore) must not stop the others.
    private suspend fun notify(listener: AccountListener, account: AccountState) {
        try {
            listener.onAccountChanged(account)
        } catch (e: Exception) {
            Log.w("AccountSession", "${listener.javaClass.simpleName} failed: ${e.message}")
        }
    }
}
