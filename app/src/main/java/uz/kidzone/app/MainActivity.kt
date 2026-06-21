package uz.kidzone.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import uz.kidzone.app.ui.KidZoneApp
import uz.kidzone.app.ui.viewmodel.MainViewModel
import uz.kidzone.app.ui.viewmodel.PromoBannerData
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {

    companion object {
        /**
         * Weak reference kept for legacy Java code (ParentalDashboardActivity) that still calls
         * evaluateJs / injectJs. Will be removed when Task 10 replaces ParentalDashboardActivity
         * with a Compose screen.
         */
        @JvmField
        var instance: WeakReference<MainActivity>? = null
    }

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var adsManager: AdsManager
    private lateinit var statsManager: ParentalStatsManager
    private lateinit var systemUiHelper: SystemUiHelper
    private lateinit var firestoreSync: FirestoreSync
    private val kzPrefs by lazy { getSharedPreferences("kz_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        instance = WeakReference(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        systemUiHelper = SystemUiHelper(window)

        requestNotificationPermissionIfNeeded()

        FirebaseManager.init(this)
        firestoreSync = FirestoreSync.init(this)
        statsManager = ParentalStatsManager(this)
        adsManager = AdsManager(this)
        adsManager.initialize()

        FirebaseManager.getInstance().ensureAuthAsync {
            val uid = FirebaseManager.getInstance().getUid()
            if (uid != null) {
                FcmTokenManager.registerToken(uid, firestoreSync)
                BanChecker.checkAsync(uid, firestoreSync) { status ->
                    if (status == BanChecker.Status.BANNED) runOnUiThread { banUser() }
                }
            }
            BannerChecker.checkAsync(firestoreSync) { banner ->
                if (banner != null) runOnUiThread {
                    mainViewModel.setPromoBanner(
                        PromoBannerData(
                            title = banner.title,
                            body = banner.body,
                            url = banner.url,
                        )
                    )
                }
            }
        }

        setContent {
            KidZoneApp(
                prefs = kzPrefs,
                mainViewModel = mainViewModel,
                adsManager = adsManager,
            )
        }
    }

    private fun banUser() {
        if (isFinishing) return
        kzPrefs.edit().putBoolean("kz_onboarding_done", false).apply()
        recreate()
    }

    override fun onResume() {
        super.onResume()
        systemUiHelper.enableImmersiveMode()
        adsManager.onResume()
        MusicManager.startMusic(this)
        statsManager.onSessionStart()
        checkPendingUrl()
    }

    override fun onPause() {
        statsManager.onSessionEnd()
        adsManager.onPause()
        MusicManager.pauseMusic()
        super.onPause()
    }

    override fun onDestroy() {
        instance = null
        adsManager.onDestroy()
        super.onDestroy()
    }

    /**
     * Legacy JS injection used by ParentalDashboardActivity (Java).
     * WebView is now owned by MainScreen composable; this is a no-op stub
     * until Task 10 replaces ParentalDashboardActivity with Compose.
     */
    fun injectJs(script: String) {
        // no-op: WebView reference lives in MainScreen composable state
    }

    /**
     * Legacy JS evaluation used by ParentalDashboardActivity (Java).
     * No-op stub until Task 10 replaces ParentalDashboardActivity with Compose.
     */
    fun evaluateJs(script: String, callback: ValueCallback<String>) {
        // no-op: WebView reference lives in MainScreen composable state
    }

    private fun checkPendingUrl() {
        val url = kzPrefs.getString("kz_pending_url", null) ?: return
        kzPrefs.edit().remove("kz_pending_url").apply()
        // TODO: openUrl(url) — deep-link handling; implement after MainScreen exposes webMgr ref
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) return
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1001,
        )
    }
}
