package uz.kidzone.app.data

import android.content.SharedPreferences

/**
 * Whose `users/{uid}/profiles` this device works with. A parent who joined someone's family
 * (Faza 16) uses the owner's profiles; everyone else uses their own. Set by
 * [uz.kidzone.app.auth.ProfileCloudSyncListener] from `users/{uid}.familyOwner`.
 */
object ProfileOwner {
    @Volatile var familyOwnerUid: String? = null

    fun uidFor(currentUid: String?): String? = familyOwnerUid ?: currentUid
}

/**
 * Whose cloud profiles the rows in Room came from; null while they are only a guest's. When
 * another owner's profiles are synced (joining or leaving a family, another account signing
 * in), Room is replaced instead of merged, so one family's profiles never leak into another's.
 */
interface LocalProfilesOwner {
    var uid: String?
}

class PrefsLocalProfilesOwner(private val prefs: SharedPreferences) : LocalProfilesOwner {
    override var uid: String?
        get() = prefs.getString(KEY, null)
        set(value) = prefs.edit().putString(KEY, value).apply()

    private companion object {
        const val KEY = "kz_profiles_owner"
    }
}
