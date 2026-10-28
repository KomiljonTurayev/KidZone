package uz.kidzone.app.auth

import android.content.SharedPreferences
import com.revenuecat.purchases.Purchases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import uz.kidzone.app.FcmTokenManager
import uz.kidzone.app.FirestoreSync
import uz.kidzone.app.data.ProfileDao
import uz.kidzone.app.data.ProfileSyncManager

/** Keeps `users/{uid}` and the push token registered for whichever account is active. */
class CloudUserListener(
    private val firestoreSync: () -> FirestoreSync,
    private val prefs: SharedPreferences,
) : AccountListener {
    override suspend fun onAccountChanged(account: AccountState) {
        val uid = account.uid ?: return
        prefs.edit().putString("kz_uid", uid).apply()
        val sync = firestoreSync()
        val displayName = (account as? AccountState.SignedIn)?.displayName
        sync.syncUserProfile(uid, displayName, null, prefs.getString("kz_age_filter", "3-5") ?: "3-5")
        FcmTokenManager.registerToken(uid, sync)
    }
}

/**
 * Keeps the parent's profiles the same on every device of their account. On sign-in: pull and
 * merge what other devices saved, push this device's profiles (the server-side play-time limit
 * reads `users/{uid}/profiles/{id}`), then keep listening so later changes from other devices
 * arrive while the app is open. Guests and signed-out users don't sync.
 */
class ProfileCloudSyncListener(
    private val profileDao: ProfileDao,
    private val syncManager: ProfileSyncManager,
    private val scope: CoroutineScope,
) : AccountListener {
    private var live: AutoCloseable? = null

    override suspend fun onAccountChanged(account: AccountState) {
        live?.close()
        live = null
        if (account !is AccountState.SignedIn) return
        val remote = syncManager.pullProfiles(account.uid)
        if (remote.isNotEmpty()) profileDao.mergeProfiles(remote)
        profileDao.getAllSync().forEach(syncManager::pushProfile)
        live = syncManager.listenProfiles(account.uid) { profiles ->
            scope.launch { profileDao.mergeProfiles(profiles) }
        }
    }
}

/** Ties purchases to the parent account so Premium follows them to a new phone. */
class PurchasesIdentityListener : AccountListener {
    override suspend fun onAccountChanged(account: AccountState) {
        if (!Purchases.isConfigured) return
        val purchases = Purchases.sharedInstance
        when (account) {
            is AccountState.SignedIn -> purchases.logIn(account.uid)
            is AccountState.Guest -> if (!purchases.isAnonymous) purchases.logOut()
            AccountState.Unavailable -> Unit
        }
    }
}
