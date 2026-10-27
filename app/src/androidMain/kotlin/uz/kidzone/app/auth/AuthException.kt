package uz.kidzone.app.auth

class AuthException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {

    enum class Reason {
        /** The user closed the sign-in sheet; not an error worth showing. */
        CANCELLED,
        /** Google/Telegram sign-in is not set up in this build or on the backend. */
        NOT_CONFIGURED,
        NETWORK,
        /** The Telegram link was not confirmed in time. */
        EXPIRED,
        /** Deleting the account needs a fresh sign-in first. */
        REAUTH_REQUIRED,
        NO_SESSION,
        FAILED,
    }
}

/** Uzbek copy shown to the parent for each failure. */
fun AuthException.Reason.userMessage(): String? = when (this) {
    AuthException.Reason.CANCELLED -> null
    AuthException.Reason.NOT_CONFIGURED -> "Bu kirish usuli hozircha sozlanmagan."
    AuthException.Reason.NETWORK -> "Internet aloqasini tekshirib, qayta urinib ko'ring."
    AuthException.Reason.EXPIRED -> "Vaqt tugadi. Qaytadan urinib ko'ring."
    AuthException.Reason.REAUTH_REQUIRED -> "Xavfsizlik uchun avval qaytadan kiring, so'ng hisobni o'chiring."
    AuthException.Reason.NO_SESSION -> "Ilova hali tayyor emas. Birozdan so'ng qayta urinib ko'ring."
    AuthException.Reason.FAILED -> "Kirib bo'lmadi. Qayta urinib ko'ring."
}
