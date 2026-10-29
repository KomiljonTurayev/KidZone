package uz.kidzone.app.auth

import android.content.SharedPreferences
import com.revenuecat.purchases.Purchases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import uz.kidzone.app.FcmTokenManager
import uz.kidzone.app.FirestoreSync
import uz.kidzone.app.data.LocalProfilesOwner
import uz.kidzone.app.data.ProfileDao
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.data.ProfileOwner
import uz.kidzone.app.data.ProfileSyncManager
import java.util.UUID

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
 * sign-in it waits for `users/{uid}.familyOwner`, and whenever the owner changes (join, leave,
 * removed) it syncs that owner's profiles and listens for later changes. A guest's profiles are
 * uploaded to the parent's own account on the first sign-in; any other profiles on the device
 * are replaced by the owner's, not uploaded. Guests and signed-out users don't sync.
 */
class ProfileCloudSyncListener(
    private val profileDao: ProfileDao,
    private val syncManager: ProfileSyncManager,
    private val scope: CoroutineScope,
    private val localOwner: LocalProfilesOwner,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
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
        familyWatch = syncManager.listenFamilyOwner(account.uid) { familyOwner ->
            scope.launch {
                val owner = familyOwner ?: account.uid
                if (owner != syncedOwner) {
                    stopProfiles()
                    startProfiles(owner, familyOwner)
                }
            }
        }
    }

    /**
     * If another owner's profiles can't be fetched, this device keeps working with the profiles
     * it has (and their owner) rather than wiping them while offline.
     */
    private suspend fun startProfiles(ownerUid: String, familyOwner: String?) {
        val previousOwner = localOwner.uid
        // A guest's profiles become the account's own, but never join someone else's family.
        if (previousOwner == ownerUid || (previousOwner == null && familyOwner == null)) {
            ProfileOwner.familyOwnerUid = familyOwner
            val remote = syncManager.pullProfiles(ownerUid)
            if (remote.isNotEmpty()) profileDao.mergeProfiles(remote)
            profileDao.getAllSync().forEach(syncManager::pushProfile)
        } else {
            val remote = syncManager.pullProfilesFromServer(ownerUid) ?: return
            ProfileOwner.familyOwnerUid = familyOwner
            profileDao.replaceProfiles(remote)
            if (remote.none { !it.deleted }) {
                // A new owner with no profiles yet: start them with one, as a fresh install does.
                val now = nowMs()
                val first = ProfileEntity(
                    id = newId(), name = "Asosiy", avatarPath = null, language = "uz",
                    timeLimitMinutes = 0, pinHash = null, isDefault = true,
                    createdAt = now, updatedAt = now,
                )
                profileDao.insert(first)
                syncManager.pushProfile(first)
            }
        }
        localOwner.uid = ownerUid
        syncedOwner = ownerUid
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
