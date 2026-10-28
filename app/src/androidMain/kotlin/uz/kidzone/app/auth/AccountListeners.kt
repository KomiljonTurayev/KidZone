package uz.kidzone.app.auth

import android.content.SharedPreferences
import com.revenuecat.purchases.Purchases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import uz.kidzone.app.FcmTokenManager
import uz.kidzone.app.FirestoreSync
import uz.kidzone.app.data.ProfileDao
import uz.kidzone.app.data.ProfileOwner
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
 * Keeps the parent's profiles the same on every device of their account — or of their family,
 * if they joined one (Faza 16): then the owner's `users/{ownerUid}/profiles` are used. On
 * sign-in it follows `users/{uid}.familyOwner`; whenever that changes (join, leave, removed)
 * it pulls and merges the profiles, pushes this device's, and listens for later changes.
 * Guests and signed-out users don't sync.
 */
class ProfileCloudSyncListener(
    private val profileDao: ProfileDao,
    private val syncManager: ProfileSyncManager,
    private val scope: CoroutineScope,
) : AccountListener {
    private var familyWatch: AutoCloseable? = null
    private var live: AutoCloseable? = null
    private var syncedOwner: String? = null

    override suspend fun onAccountChanged(account: AccountState) {
        familyWatch?.close()
        familyWatch = null
        stopProfiles()
        ProfileOwner.familyOwnerUid = null
        if (account !is AccountState.SignedIn) return
        startProfiles(account.uid)
        familyWatch = syncManager.listenFamilyOwner(account.uid) { familyOwner ->
            scope.launch {
                ProfileOwner.familyOwnerUid = familyOwner
                val owner = familyOwner ?: account.uid
                if (owner != syncedOwner) {
                    stopProfiles()
                    startProfiles(owner)
                }
            }
        }
    }

    private suspend fun startProfiles(ownerUid: String) {
        syncedOwner = ownerUid
        val remote = syncManager.pullProfiles(ownerUid)
        if (remote.isNotEmpty()) profileDao.mergeProfiles(remote)
        profileDao.getAllSync().forEach(syncManager::pushProfile)
        live = syncManager.listenProfiles(ownerUid) { profiles ->
            scope.launch { profileDao.mergeProfiles(profiles) }
        }
    }

    private fun stopProfiles() {
        live?.close()
        live = null
        syncedOwner = null
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
