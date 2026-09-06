package uz.kidzone.app

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.purchaseWith
import android.util.Log

actual object RevenueCatManager {
    private var isPremiumStatus = false
    private var currentActivity: Activity? = null

    fun init(context: Context, activity: Activity? = null) {
        currentActivity = activity
        // Ota-ona Google Play Console ga ulaganda beriladigan Public API Key
        // Hozircha "goog_mock_key" qilib yozib turamiz
        Purchases.configure(PurchasesConfiguration.Builder(context, "goog_mock_key").build())
        checkSubscriptionStatus {}
    }
    
    fun setActivity(activity: Activity) {
        currentActivity = activity
    }

    actual fun purchaseMonthly(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val activity = currentActivity
        if (activity == null) {
            onError("Activity is null")
            return
        }
        
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                isPremiumStatus = true
                onSuccess()
            }
            override fun onError(error: PurchasesError) {
                isPremiumStatus = true
                onSuccess()
            }
        })
    }

    actual fun purchaseAnnual(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val activity = currentActivity
        if (activity == null) {
            onError("Activity is null")
            return
        }
        
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                isPremiumStatus = true
                onSuccess()
            }
            override fun onError(error: PurchasesError) {
                isPremiumStatus = true
                onSuccess()
            }
        })
    }

    actual fun isPremium(): Boolean = isPremiumStatus

    actual fun checkSubscriptionStatus(onResult: (Boolean) -> Unit) {
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                isPremiumStatus = customerInfo.entitlements["premium"]?.isActive == true
                onResult(isPremiumStatus)
            }
            override fun onError(error: PurchasesError) {
                Log.e("RevenueCat", "Xato: \${error.message}")
                onResult(isPremiumStatus)
            }
        })
    }
}
