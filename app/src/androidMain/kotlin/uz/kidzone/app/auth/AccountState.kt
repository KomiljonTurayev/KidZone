package uz.kidzone.app.auth

enum class AuthProvider { GOOGLE, TELEGRAM, EMAIL, UNKNOWN }

/**
 * Who is using the app right now. Every install starts as a [Guest] (an anonymous Firebase
 * account, so play-time limits and stats work without signing in); a parent can upgrade to
 * [SignedIn] with Google or Telegram to unlock the account-only features.
 */
sealed interface AccountState {
    val uid: String?

    /** Firebase is not configured in this build, or the first guest session is still starting. */
    data object Unavailable : AccountState {
        override val uid: String? = null
    }

    data class Guest(override val uid: String) : AccountState

    data class SignedIn(
        override val uid: String,
        val displayName: String?,
        val provider: AuthProvider,
    ) : AccountState
}

val AccountState.isSignedIn: Boolean get() = this is AccountState.SignedIn

/** Uid prefix the backend gives accounts created through the Telegram bot. */
internal const val TELEGRAM_UID_PREFIX = "telegram:"

/**
 * Maps a Firebase user to an [AccountState]. Kept free of Firebase types so it can be unit
 * tested; [providerIds] are the user's `providerData[].providerId` values.
 */
internal fun accountStateOf(
    uid: String?,
    isAnonymous: Boolean,
    providerIds: List<String>,
    displayName: String?,
    email: String?,
): AccountState = when {
    uid == null -> AccountState.Unavailable
    isAnonymous -> AccountState.Guest(uid)
    else -> AccountState.SignedIn(
        uid = uid,
        displayName = displayName?.takeIf { it.isNotBlank() } ?: email,
        provider = when {
            uid.startsWith(TELEGRAM_UID_PREFIX) -> AuthProvider.TELEGRAM
            "google.com" in providerIds -> AuthProvider.GOOGLE
            "password" in providerIds -> AuthProvider.EMAIL
            else -> AuthProvider.UNKNOWN
        },
    )
}
