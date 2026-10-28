package uz.kidzone.app.data

/**
 * Whose `users/{uid}/profiles` this device works with. A parent who joined someone's family
 * (Faza 16) uses the owner's profiles; everyone else uses their own. Set by
 * [uz.kidzone.app.auth.ProfileCloudSyncListener] from `users/{uid}.familyOwner`.
 */
object ProfileOwner {
    @Volatile var familyOwnerUid: String? = null

    fun uidFor(currentUid: String?): String? = familyOwnerUid ?: currentUid
}
