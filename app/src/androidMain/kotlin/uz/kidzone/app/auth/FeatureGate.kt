package uz.kidzone.app.auth

import kotlinx.coroutines.flow.StateFlow

/** Features a guest can see but must sign in (as a parent) to use. */
enum class AccountFeature {
    AI_COMPANION,
    CLOUD_SYNC,
    PREMIUM,
    PARENT_DASHBOARD;

    /** Name shown on the sign-in screen, in [lang] ("uz", "ru" or "en"). */
    fun title(lang: String): String = when (this) {
        AI_COMPANION -> pick(lang, "Kidzo AI yordamchi", "Kidzo — ИИ-помощник", "Kidzo AI helper")
        CLOUD_SYNC -> pick(lang, "Bulutli sinxronlash", "Облачная синхронизация", "Cloud sync")
        PREMIUM -> pick(lang, "Premium obuna", "Подписка Premium", "Premium subscription")
        PARENT_DASHBOARD -> pick(lang, "Ota-ona paneli", "Панель родителя", "Parent dashboard")
    }

    /** One line on what the feature gives, in [lang]. */
    fun pitch(lang: String): String = when (this) {
        AI_COMPANION -> pick(
            lang,
            "Kidzo bilan suhbat, ovozli javoblar va sun'iy intellekt ertaklari",
            "Беседы с Kidzo, голосовые ответы и сказки от ИИ",
            "Chats with Kidzo, spoken answers and AI stories",
        )
        CLOUD_SYNC -> pick(
            lang,
            "Profillar va statistikani boshqa qurilmaga ko'chirish",
            "Перенос профилей и статистики на другое устройство",
            "Move profiles and stats to another device",
        )
        PREMIUM -> pick(
            lang,
            "Obuna hisobingizga bog'lanadi va yangi telefonda ham tiklanadi",
            "Подписка привязывается к аккаунту и восстанавливается на новом телефоне",
            "The subscription is tied to your account and restored on a new phone",
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
) {
    fun isAvailable(feature: AccountFeature): Boolean =
        feature !in accountOnly || account.value.isSignedIn
}
