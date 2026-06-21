package uz.kidzone.app

object AdminConfig {
    // Firebase Console → Authentication → Users'dan o'z UID'ingizni oling
    @JvmField
    val ADMIN_UID: String = "REPLACE_WITH_YOUR_FIREBASE_UID"

    init {
        if (BuildConfig.DEBUG && ADMIN_UID.startsWith("REPLACE")) {
            throw AssertionError("AdminConfig.ADMIN_UID not configured")
        }
    }
}
