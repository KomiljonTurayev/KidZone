package uz.kidzone.app

actual object RevenueCatManager {
    private var isPremiumStatus = false

    actual fun purchaseMonthly(onSuccess: () -> Unit, onError: (String) -> Unit) {
        // iOS qismi uchun RevenueCat Swift orqali KMP ga ulanadi
        // Yoki KMP Purchases kutubxonasiga o'tkazilganda to'ldiriladi
        onError("Hozircha iOS to'lovlari faollashtirilmagan")
    }

    actual fun purchaseAnnual(onSuccess: () -> Unit, onError: (String) -> Unit) {
        onError("Hozircha iOS to'lovlari faollashtirilmagan")
    }

    actual fun isPremium(): Boolean = isPremiumStatus

    actual fun checkSubscriptionStatus(onResult: (Boolean) -> Unit) {
        onResult(isPremiumStatus)
    }
}
