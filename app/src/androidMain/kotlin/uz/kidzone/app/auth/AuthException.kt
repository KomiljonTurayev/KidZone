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
        /** The parent confirmed with a different account than the one signed in. */
        WRONG_ACCOUNT,
        NO_SESSION,
        FAILED,
    }
}

/** Copy shown to the parent for each failure, in [lang] ("uz", "ru" or "en"; anything else is Uzbek). */
fun AuthException.Reason.userMessage(lang: String = "uz"): String? {
    fun t(uz: String, ru: String, en: String) = when (lang) { "ru" -> ru; "en" -> en; else -> uz }
    return when (this) {
        AuthException.Reason.CANCELLED -> null
        AuthException.Reason.NOT_CONFIGURED -> t(
            "Bu kirish usuli hozircha sozlanmagan.",
            "Этот способ входа пока не настроен.",
            "This sign-in method isn't set up yet.",
        )
        AuthException.Reason.NETWORK -> t(
            "Internet aloqasini tekshirib, qayta urinib ko'ring.",
            "Проверьте подключение к интернету и попробуйте снова.",
            "Check your internet connection and try again.",
        )
        AuthException.Reason.EXPIRED -> t(
            "Vaqt tugadi. Qaytadan urinib ko'ring.",
            "Время истекло. Попробуйте снова.",
            "Time ran out. Please try again.",
        )
        AuthException.Reason.REAUTH_REQUIRED -> t(
            "Xavfsizlik uchun avval qaytadan kiring, so'ng hisobni o'chiring.",
            "В целях безопасности войдите заново, затем удалите аккаунт.",
            "For security, sign in again first, then delete the account.",
        )
        AuthException.Reason.WRONG_ACCOUNT -> t(
            "Bu boshqa hisob. Ilovaga kirgan hisobingiz bilan tasdiqlang.",
            "Это другой аккаунт. Подтвердите тем аккаунтом, которым вы вошли в приложение.",
            "That's a different account. Confirm with the account you're signed in with.",
        )
        AuthException.Reason.NO_SESSION -> t(
            "Ilova hali tayyor emas. Birozdan so'ng qayta urinib ko'ring.",
            "Приложение ещё не готово. Попробуйте чуть позже.",
            "The app isn't ready yet. Please try again in a moment.",
        )
        AuthException.Reason.FAILED -> t(
            "Kirib bo'lmadi. Qayta urinib ko'ring.",
            "Не удалось войти. Попробуйте снова.",
            "Couldn't sign in. Please try again.",
        )
    }
}
