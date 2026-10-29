package uz.kidzone.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import uz.kidzone.app.auth.AuthGraph
import uz.kidzone.app.ui.KidZoneApp
import uz.kidzone.app.ui.viewmodel.MainViewModel
import uz.kidzone.app.ui.viewmodel.PromoBannerData

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var statsManager: ParentalStatsManager
    private lateinit var systemUiHelper: SystemUiHelper
    private lateinit var firestoreSync: FirestoreSync
    private val kzPrefs by lazy { getSharedPreferences("kz_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        RevenueCatManager.init(this, this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Draw content edge-to-edge around notches/punch-hole cameras instead of
            // letterboxing around them — statusBarsPadding() in Compose already reserves
            // enough space so nothing renders under the cutout itself.
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        systemUiHelper = SystemUiHelper(window)

        // On a first launch this waits until the parent finishes onboarding, so the system
        // prompt doesn't greet them before they know what the app is.
        if (kzPrefs.getBoolean("kz_onboarding_done", false)) requestNotificationPermissionIfNeeded()

        firestoreSync = FirestoreSync.init(this)
        statsManager = ParentalStatsManager(this)

        // Push-token registration happens in the Application's AccountSessionCoordinator;
        // here we only need the UI-facing checks, re-run whenever the account changes.
        lifecycleScope.launch {
            AuthGraph.auth.state
                .mapNotNull { it.uid }
                .distinctUntilChanged()
                .collect { uid -> checkAccountStatus(uid) }
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

        setContent {
            KidZoneApp(
                prefs = kzPrefs,
                mainViewModel = mainViewModel,
                statsManager = statsManager,
                onOnboardingDone = ::requestNotificationPermissionIfNeeded,
            )
        }
    }

    private fun checkAccountStatus(uid: String) {
        BanChecker.checkAsync(uid, firestoreSync) { status ->
            if (status == BanChecker.Status.BANNED) runOnUiThread { banUser() }
        }
    }

    private fun banUser() {
        if (isFinishing) return
        mainViewModel.setBanned(true)
    }

    override fun onResume() {
        super.onResume()
        systemUiHelper.enableImmersiveMode()
        MusicManager.startMusic(this)
        statsManager.onSessionStart()
        checkPendingUrl()
    }

    override fun onPause() {
        statsManager.onSessionEnd()
        MusicManager.pauseMusic()
        super.onPause()
    }

    private fun checkPendingUrl() {
        val contentId = kzPrefs.getString("kz_pending_url", null) ?: return
        kzPrefs.edit().remove("kz_pending_url").apply()
        mainViewModel.setPendingDeepLinkContentId(contentId)
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