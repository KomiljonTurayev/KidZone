package uz.kidzone.app.auth

import kotlinx.coroutines.flow.StateFlow

/** Features a guest can see but must sign in (as a parent) to use. */
enum class AccountFeature {
    AI_COMPANION,
    CLOUD_SYNC,
    PARENT_DASHBOARD;

    /** Name shown on the sign-in screen, in [lang] ("uz", "ru" or "en"). */
    fun title(lang: String): String = when (this) {
        AI_COMPANION -> pick(lang, "Kidzo yordamchi", "Kidzo — помощник", "Kidzo helper")
        CLOUD_SYNC -> pick(lang, "Bulutli sinxronlash", "Облачная синхронизация", "Cloud sync")
        PARENT_DASHBOARD -> pick(lang, "Ota-ona paneli", "Панель родителя", "Parent dashboard")
    }

    /** One line on what the feature gives, in [lang]. */
    fun pitch(lang: String): String = when (this) {
        AI_COMPANION -> pick(
            lang,
            "Kidzo bilan suhbat, ovozli javoblar va shaxsiy ertaklar",
            "Беседы с Kidzo, голосовые ответы и личные сказки",
            "Chats with Kidzo, spoken answers and personal stories",
        )
        CLOUD_SYNC -> pick(
            lang,
            "Profillar va statistikani boshqa qurilmaga ko'chirish",
            "Перенос профилей и статистики на другое устройство",
            "Move profiles and stats to another device",
        )
        PARENT_DASHBOARD -> pick(
            lang,
            "Bolangiz statistikasi, vaqt limiti va sozlamalar",
            "Статистика ребёнка, лимит времени и настройки",
            "Your child's stats, time limit and settings",
        )
    }

    private fun pick(lang: String, uz: String, ru: String, en: String) = when (lang) {
        "ru" -> ru
        "en" -> en
        else -> uz
    }
}

/** Decides whether the current account may use a feature; the rest of the app stays free for guests. */
class FeatureGate(
    private val account: StateFlow<AccountState>,
    private val accountOnly: Set<AccountFeature> = AccountFeature.entries.toSet(),
    /**
     * Families Policy (neutral age screen): `true` for a child or an unanswered age screen.
     * Account-only features are then not offered at all — no sign-in prompt, no AI, no purchases.
     */
    private val restricted: () -> Boolean = { false },
) {
    fun isAvailable(feature: AccountFeature): Boolean =
        !isHidden(feature) && (feature !in accountOnly || account.value.isSignedIn)

    /** Hidden features get no entry point and no sign-in prompt (see [restricted]). */
    fun isHidden(feature: AccountFeature): Boolean = feature in accountOnly && restricted()
}
