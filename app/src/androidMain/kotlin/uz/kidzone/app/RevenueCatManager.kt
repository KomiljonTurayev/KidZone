package uz.kidzone.app

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreTransaction

private const val ENTITLEMENT_PREMIUM = "premium"

actual object RevenueCatManager {
    private var isPremiumStatus = false
    private var currentActivity: Activity? = null

    fun init(context: Context, activity: Activity? = null) {
        currentActivity = activity
        val apiKey = BuildConfig.REVENUECAT_API_KEY
        if (apiKey.isBlank()) {
            Log.e("RevenueCat", "REVENUECAT_API_KEY sozlanmagan — xaridlar ishlamaydi")
            return
        }
        Purchases.configure(PurchasesConfiguration.Builder(context, apiKey).build())
        checkSubscriptionStatus {}
    }

    fun setActivity(activity: Activity) {
        currentActivity = activity
    }

    actual fun purchaseMonthly(onSuccess: () -> Unit, onError: (String) -> Unit) {
        purchasePlan(onSuccess, onError) { it.monthly }
    }

    actual fun purchaseAnnual(onSuccess: () -> Unit, onError: (String) -> Unit) {
        purchasePlan(onSuccess, onError) { it.annual }
    }

    private fun purchasePlan(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        selectPackage: (Offering) -> Package?
    ) {
        val activity = currentActivity
        if (activity == null) {
            onError("Activity is null")
            return
        }

        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                val offering = offerings.current
                val packageToPurchase = offering?.let(selectPackage)
                if (packageToPurchase == null) {
                    onError("Reja hozircha mavjud emas")
                    return
                }

                Purchases.sharedInstance.purchase(
                    PurchaseParams.Builder(activity, packageToPurchase).build(),
                    object : PurchaseCallback {
                        override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                            isPremiumStatus = customerInfo.entitlements[ENTITLEMENT_PREMIUM]?.isActive == true
                            if (isPremiumStatus) {
                                onSuccess()
                            } else {
                                onError("To'lov qabul qilindi, lekin obuna faollashmadi")
                            }
                        }

                        override fun onError(error: PurchasesError, userCancelled: Boolean) {
                            if (!userCancelled) {
                                Log.e("RevenueCat", "Xarid xatosi: ${error.message}")
                            }
                            onError(error.message)
                        }
                    }
                )
            }

            override fun onError(error: PurchasesError) {
                Log.e("RevenueCat", "Takliflarni olishda xato: ${error.message}")
                onError(error.message)
            }
        })
    }

    actual fun isPremium(): Boolean = isPremiumStatus

    actual fun checkSubscriptionStatus(onResult: (Boolean) -> Unit) {
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                isPremiumStatus = customerInfo.entitlements[ENTITLEMENT_PREMIUM]?.isActive == true
                onResult(isPremiumStatus)
            }
            override fun onError(error: PurchasesError) {
                Log.e("RevenueCat", "Xato: ${error.message}")
                onResult(isPremiumStatus)
            }
        })
    }
}
