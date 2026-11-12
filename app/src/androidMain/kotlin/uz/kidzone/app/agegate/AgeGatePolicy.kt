package uz.kidzone.app.agegate

import android.content.Context
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Neytral yosh ekrani natijasiga qarab bolalar uchun tasdiqlanmagan SDK'larni yoqadi/o'chiradi.
 *
 * Manifest'da Analytics, Crashlytics va FCM auto-init sukut bo'yicha O'CHIQ — ekran o'tilmaguncha
 * hech narsa yig'ilmaydi. Faqat [AgeBracket.TEEN_OR_ADULT] ularni yoqadi; [AgeBracket.CHILD] va
 * hali o'tilmagan ekran cheklangan rejimda qoladi. Kidzo AI, kirish, to'lovlar (RevenueCat) va push
 * esa UI/initsializatsiya darajasida [AgeGateState.isRestricted] orqali yopiladi.
 */
object AgeGatePolicy {
    private const val TAG = "KZ_AGE_GATE"

    fun apply(context: Context, state: AgeGateState) {
        val adult = !state.isRestricted
        runCatching { FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(adult) }
            .onFailure { Log.w(TAG, "analytics toggle failed: ${it.message}") }
        runCatching {
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(adult)
                // Ekrangacha yig'ilgan, hali yuborilmagan crash hisobotlari bola uchun yuborilmaydi.
                if (state.bracket == AgeBracket.CHILD) deleteUnsentReports()
            }
        }.onFailure { Log.w(TAG, "crashlytics toggle failed: ${it.message}") }
        runCatching {
            FirebaseMessaging.getInstance().apply {
                isAutoInitEnabled = adult
                // Avval katta deb ishlatilgan qurilma endi bola bo'lsa — eski push token o'chiriladi.
                if (state.bracket == AgeBracket.CHILD) deleteToken()
            }
        }.onFailure { Log.w(TAG, "messaging toggle failed: ${it.message}") }
    }
}
