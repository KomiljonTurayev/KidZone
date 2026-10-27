package uz.kidzone.app.auth

import android.content.SharedPreferences
import com.revenuecat.purchases.Purchases
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
 * When a parent signs in, their account may already hold profiles from another device: pull
 * and merge those, then push this device's profiles so the server-side play-time limit
 * (which reads `users/{uid}/profiles/{id}`) works for the new uid too.
 */
class ProfileCloudSyncListener(
    private val profileDao: ProfileDao,
    private val syncManager: ProfileSyncManager,
) : AccountListener {
    override suspend fun onAccountChanged(account: AccountState) {
        if (account !is AccountState.SignedIn) return
        val remote = syncManager.pullProfiles(account.uid)
        if (remote.isNotEmpty()) profileDao.mergeProfiles(remote)
        profileDao.getAllSync().forEach(syncManager::pushProfile)
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
