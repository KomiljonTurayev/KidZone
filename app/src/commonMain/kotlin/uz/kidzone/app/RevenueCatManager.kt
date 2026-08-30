package uz.kidzone.app

expect object RevenueCatManager {
    fun purchaseMonthly(onSuccess: () -> Unit, onError: (String) -> Unit)
    fun purchaseAnnual(onSuccess: () -> Unit, onError: (String) -> Unit)
    fun isPremium(): Boolean
    fun checkSubscriptionStatus(onResult: (Boolean) -> Unit)
}
