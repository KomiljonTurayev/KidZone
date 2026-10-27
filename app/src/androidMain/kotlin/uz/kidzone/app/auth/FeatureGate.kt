package uz.kidzone.app.auth

import kotlinx.coroutines.flow.StateFlow

/** Features a guest can see but must sign in (as a parent) to use. */
enum class AccountFeature(val title: String, val pitch: String) {
    AI_COMPANION("Kidzo AI yordamchi", "Kidzo bilan suhbat, ovozli javoblar va sun'iy intellekt ertaklari"),
    CLOUD_SYNC("Bulutli sinxronlash", "Profillar va statistikani boshqa qurilmaga ko'chirish"),
    PREMIUM("Premium obuna", "Obuna hisobingizga bog'lanadi va yangi telefonda ham tiklanadi"),
    PARENT_DASHBOARD("Ota-ona paneli", "Bolangiz statistikasi, vaqt limiti va sozlamalar"),
}

/** Decides whether the current account may use a feature; the rest of the app stays free for guests. */
class FeatureGate(
    private val account: StateFlow<AccountState>,
    private val accountOnly: Set<AccountFeature> = AccountFeature.entries.toSet(),
) {
    fun isAvailable(feature: AccountFeature): Boolean =
        feature !in accountOnly || account.value.isSignedIn
}
