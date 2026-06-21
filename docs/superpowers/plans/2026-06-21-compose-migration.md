# KidZone: To'liq Kotlin + Compose Migratsiya Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Barcha Java/XML kodni to'liq Kotlin + Jetpack Compose ga ko'chirish — bitta ComponentActivity, NavHost, barcha managerlar Kotlin.

**Architecture:** Activity by Activity yondashuv. Har bosqich oxirida `installDebug` — app qurilmada ishlaydi. Barcha Java managerlar Kotlin ga ko'chiriladi (xuddi shu API), XML layoutlar o'chirilib Compose ekranlar bilan almashtiriladi.

**Tech Stack:** Kotlin 1.9.24, Jetpack Compose BOM 2024.04.01, Navigation-Compose 2.7.7, Material3, OkHttp 4.12.0, Firebase BOM 33.7.0

## Global Constraints

- Min SDK 26, Target SDK 35
- Compose BOM 2024.04.01, kotlinCompilerExtensionVersion 1.5.13
- Package: `uz.kidzone.app`
- SharedPreferences key: `"kz_prefs"` (OnboardingActivity.PREFS bilan bir xil)
- Onboarding done key: `"kz_onboarding_done"` (OnboardingActivity.KEY_DONE bilan bir xil)
- PIN prefs key: `"kz_pin"`
- `RecyclerView` dependency `o'chirilishi mumkin` — `LazyRow` bilan almashtiriladi
- Hilt yo'q — manual factory pattern

---

## Task 1: build.gradle — Compose va kotlinOptions qayta qo'shish

**Files:**
- Modify: `app/build.gradle`

**Interfaces:**
- Produces: Kotlin Compose kompilyatsiyasi ishlaydi

- [ ] **Step 1: `buildFeatures`, `composeOptions`, `kotlinOptions` qo'shish**

`app/build.gradle` da `buildFeatures` blokini toping va quyidagicha almashtiring:

```gradle
buildFeatures {
    buildConfig true
    compose true
}

composeOptions {
    kotlinCompilerExtensionVersion '1.5.13'
}

kotlinOptions {
    jvmTarget = '17'
}
```

- [ ] **Step 2: Compose dependencylarni qo'shish**

`dependencies` blokida `// OkHttp` qatoridan OLDIN qo'shing:

```gradle
// Jetpack Compose
implementation platform('androidx.compose:compose-bom:2024.04.01')
implementation 'androidx.compose.ui:ui'
implementation 'androidx.compose.ui:ui-tooling-preview'
implementation 'androidx.compose.material3:material3'
implementation 'androidx.activity:activity-compose:1.9.0'

// Compose Navigation
implementation 'androidx.navigation:navigation-compose:2.7.7'

// ViewModel + Compose
implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'
implementation 'androidx.lifecycle:lifecycle-runtime-compose:2.7.0'

debugImplementation 'androidx.compose.ui:ui-tooling'
```

`RecyclerView` ni o'chiring (LazyRow bilan almashtiriladi):
```gradle
// Bu qatorni o'chiring:
// implementation 'androidx.recyclerview:recyclerview:1.3.2'
```

- [ ] **Step 3: Kompilyatsiya tekshirish**

```powershell
cd D:\android_projects\KidZone
.\gradlew compileDebugKotlin 2>&1 | Select-Object -Last 10
```

Kutilgan: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```powershell
git add app/build.gradle
git commit -m "build: restore Compose BOM + kotlinOptions for migration"
```

---

## Task 2: Kotlin data/model klasslari

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/kidzo/ContentCard.kt`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/ContentItem.kt`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/KidzoState.kt`
- Delete: `ContentCard.java`, `ContentItem.java`, `KidzoState.java`

**Interfaces:**
- Produces: `ContentCard`, `ContentItem`, `KidzoState` — Task 7 da ishlatiladi

- [ ] **Step 1: ContentCard.kt yaratish**

```kotlin
package uz.kidzone.app.kidzo

data class ContentCard(
    val contentId: String,
    val displayText: String,
    val emoji: String = "🐥",
    val type: String = "",
)
```

- [ ] **Step 2: ContentItem.kt yaratish**

```kotlin
package uz.kidzone.app.kidzo

data class ContentItem(
    val id: String,
    val emoji: String,
    val titleUz: String,
    val titleRu: String,
    val titleEn: String,
    val category: String,
) {
    fun getTitle(lang: String): String = when (lang) {
        "uz" -> titleUz
        "ru" -> titleRu
        else -> titleEn
    }

    fun toPromptLine(): String = "$id|$emoji|$titleUz|$category"
}
```

- [ ] **Step 3: KidzoState.kt yaratish**

```kotlin
package uz.kidzone.app.kidzo

enum class KidzoState {
    IDLE, THINKING, RECOMMENDATIONS, CHATTING, ERROR
}
```

- [ ] **Step 4: Java fayllarni o'chirish**

```powershell
Remove-Item "app\src\main\java\uz\kidzone\app\kidzo\ContentCard.java"
Remove-Item "app\src\main\java\uz\kidzone\app\kidzo\ContentItem.java"
Remove-Item "app\src\main\java\uz\kidzone\app\kidzo\KidzoState.java"
Remove-Item "app\src\main\java\uz\kidzone\app\kidzo\KidzoStateListener.java"
Remove-Item "app\src\main\java\uz\kidzone\app\kidzo\MainThreadRunner.java"
```

- [ ] **Step 5: Kompilyatsiya tekshirish**

```powershell
.\gradlew compileDebugKotlin 2>&1 | Select-Object -Last 5
```

Kutilgan: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/uz/kidzone/app/kidzo/
git commit -m "feat(kotlin): ContentCard, ContentItem, KidzoState data classes"
```

---

## Task 3: Utility managerlar → Kotlin

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/PinUtil.kt`
- Create: `app/src/main/java/uz/kidzone/app/MusicManager.kt`
- Create: `app/src/main/java/uz/kidzone/app/SystemUiHelper.kt`
- Create: `app/src/main/java/uz/kidzone/app/AdminConfig.kt`
- Delete: tegishli `.java` fayllar

**Interfaces:**
- `PinUtil.hash(pin: String): String`
- `PinUtil.matches(pin: String, storedHash: String): Boolean`
- `PinUtil.getOrMigrateHash(prefs: SharedPreferences, key: String): String?`
- `MusicManager` — object singleton
- `SystemUiHelper(window: Window).enableImmersiveMode()`

- [ ] **Step 1: PinUtil.kt yaratish**

```kotlin
package uz.kidzone.app

import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object PinUtil {

    fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun matches(pin: String, storedHash: String?): Boolean =
        storedHash != null && storedHash == hash(pin)

    private fun isPlainPin(stored: String?): Boolean =
        stored != null && stored.length == 4 && stored.all { it.isDigit() }

    fun getOrMigrateHash(prefs: SharedPreferences, key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        if (stored.isEmpty()) return stored
        if (isPlainPin(stored)) {
            val hashed = hash(stored)
            prefs.edit().putString(key, hashed).apply()
            return hashed
        }
        return stored
    }
}
```

- [ ] **Step 2: MusicManager.kt yaratish**

```kotlin
package uz.kidzone.app

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

object MusicManager {
    private const val TAG = "MusicManager"
    private var mediaPlayer: MediaPlayer? = null
    private var isMuted = false

    fun setMuted(muted: Boolean) {
        isMuted = muted
        mediaPlayer?.setVolume(if (isMuted) 0f else 0.15f, if (isMuted) 0f else 0.15f)
    }

    fun isMuted(): Boolean = isMuted

    fun startMusic(context: Context) {
        if (mediaPlayer == null) initializeMediaPlayer(context) else resumeMusic()
    }

    private fun initializeMediaPlayer(context: Context) {
        val resId = context.resources.getIdentifier("bg_music", "raw", context.packageName)
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(context.applicationContext, resId)?.apply {
                isLooping = true
                setVolume(if (isMuted) 0f else 0.15f, if (isMuted) 0f else 0.15f)
                if (!isMuted) start()
            }
        } else {
            loadNetworkMusic()
        }
    }

    private fun loadNetworkMusic() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource("https://www.bensound.com/bensound-music/bensound-lullaby.mp3")
                isLooping = true
                setVolume(if (isMuted) 0f else 0.15f, if (isMuted) 0f else 0.15f)
                setOnPreparedListener { if (!isMuted) it.start() }
                setOnErrorListener { mp, what, _ ->
                    Log.w(TAG, "Network music unavailable (what=$what). Running silent.")
                    mp.reset(); mp.release(); mediaPlayer = null; true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start network music: ${e.message}")
            mediaPlayer = null
        }
    }

    fun pauseMusic() {
        if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
    }

    fun resumeMusic() {
        if (mediaPlayer?.isPlaying == false && !isMuted) mediaPlayer?.start()
    }

    fun stopMusic() {
        mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null
    }
}
```

- [ ] **Step 3: SystemUiHelper.kt yaratish**

```kotlin
package uz.kidzone.app

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SystemUiHelper(private val window: Window) {

    fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
```

- [ ] **Step 4: AdminConfig.kt yaratish**

`AdminConfig.java` faylini o'qing (`app/src/main/java/uz/kidzone/app/AdminConfig.java`), keyin xuddi shu konstantalar bilan Kotlin versiyasini yozing. Pattern:

```kotlin
package uz.kidzone.app

object AdminConfig {
    // AdminConfig.java dagi barcha static konstantalar shu yerda
    // masalan:
    // const val BACKEND_URL = "..."
    // const val ADMIN_EMAIL = "..."
}
```

- [ ] **Step 5: Java fayllarni o'chirish**

```powershell
Remove-Item "app\src\main\java\uz\kidzone\app\PinUtil.java"
Remove-Item "app\src\main\java\uz\kidzone\app\MusicManager.java"
Remove-Item "app\src\main\java\uz\kidzone\app\SystemUiHelper.java"
Remove-Item "app\src\main\java\uz\kidzone\app\AdminConfig.java"
Remove-Item "app\src\main\java\uz\kidzone\app\PinDialogHelper.java"
```

- [ ] **Step 6: Kompilyatsiya tekshirish**

```powershell
.\gradlew compileDebugKotlin 2>&1 | Select-Object -Last 5
```

Kutilgan: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/uz/kidzone/app/
git commit -m "feat(kotlin): PinUtil, MusicManager, SystemUiHelper, AdminConfig"
```

---

## Task 4: Firebase va Network managerlar → Kotlin

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/FirebaseManager.kt`
- Create: `app/src/main/java/uz/kidzone/app/FirestoreSync.kt`
- Create: `app/src/main/java/uz/kidzone/app/FcmTokenManager.kt`
- Create: `app/src/main/java/uz/kidzone/app/BackendClient.kt`
- Create: `app/src/main/java/uz/kidzone/app/BanChecker.kt`
- Create: `app/src/main/java/uz/kidzone/app/BannerChecker.kt`
- Delete: tegishli `.java` fayllar

**Interfaces:**
- `FirebaseManager.getInstance(): FirebaseManager`
- `FirebaseManager.getUid(): String?`
- `FirebaseManager.ensureAuthAsync(onReady: () -> Unit)`
- `FirestoreSync.init(context: Context): FirestoreSync`
- `FirestoreSync.getInstance(): FirestoreSync`

- [ ] **Step 1: FirebaseManager.kt yaratish**

```kotlin
package uz.kidzone.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseManager private constructor(private val auth: FirebaseAuth?) {

    interface AuthCallback {
        fun onSuccess(user: FirebaseUser)
        fun onError(message: String)
    }

    companion object {
        @Volatile private var instance: FirebaseManager? = null

        @Synchronized
        fun init(ctx: Context): FirebaseManager {
            return instance ?: run {
                val auth = try { FirebaseAuth.getInstance() } catch (e: IllegalStateException) { null }
                FirebaseManager(auth).also { instance = it }
            }
        }

        @Synchronized
        fun getInstance(): FirebaseManager = instance ?: FirebaseManager(null)
    }

    fun isAvailable(): Boolean = auth != null
    fun getCurrentUser(): FirebaseUser? = auth?.currentUser
    fun getUid(): String? = getCurrentUser()?.uid

    fun signInWithEmail(email: String, password: String, cb: AuthCallback) {
        if (auth == null) { cb.onError("Firebase is not configured"); return }
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { cb.onSuccess(it.user!!) }
            .addOnFailureListener { cb.onError(it.message ?: "Unknown error") }
    }

    fun createAccountWithEmail(email: String, password: String, cb: AuthCallback) {
        if (auth == null) { cb.onError("Firebase is not configured"); return }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { cb.onSuccess(it.user!!) }
            .addOnFailureListener { cb.onError(it.message ?: "Unknown error") }
    }

    fun ensureAuthAsync(onReady: () -> Unit) {
        if (auth == null) { onReady(); return }
        if (auth.currentUser != null) { onReady(); return }
        auth.signInAnonymously().addOnCompleteListener { onReady() }
    }

    fun signOut() { auth?.signOut() }
}
```

- [ ] **Step 2: FirestoreSync.kt yaratish**

`FirestoreSync.java` faylini o'qing (`app/src/main/java/uz/kidzone/app/FirestoreSync.java`), keyin Kotlin ga ko'chiring. `getInstance()` va `init(context)` static metodlar `companion object` da bo'lishi kerak. Java `null` lar Kotlin `?` bilan almashtiriladi.

Pattern:
```kotlin
package uz.kidzone.app

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreSync private constructor(private val db: FirebaseFirestore) {
    companion object {
        @Volatile private var instance: FirestoreSync? = null
        fun init(context: Context): FirestoreSync { /* ... */ }
        fun getInstance(): FirestoreSync = instance ?: throw IllegalStateException("Not init")
    }
    // Java metodlar → Kotlin metodlar (xuddi shu logika)
}
```

- [ ] **Step 3: FcmTokenManager.kt yaratish**

`FcmTokenManager.java` faylini o'qing, Kotlin ga ko'chiring. `registerToken(uid, firestoreSync)` metodi saqlanadi.

- [ ] **Step 4: BackendClient.kt yaratish**

`BackendClient.java` faylini o'qing, Kotlin ga ko'chiring. `sendTopicPush(title, body, url, onDone, onError)` saqlanadi.

- [ ] **Step 5: BanChecker.kt yaratish**

`BanChecker.java` faylini o'qing, Kotlin ga ko'chiring.

```kotlin
package uz.kidzone.app

object BanChecker {
    enum class Status { OK, BANNED }
    // checkAsync(uid, firestoreSync, callback: (Status) -> Unit)
}
```

- [ ] **Step 6: BannerChecker.kt yaratish**

`BannerChecker.java` faylini o'qing, Kotlin ga ko'chiring.

```kotlin
package uz.kidzone.app

data class BannerData(val title: String, val body: String, val url: String)

object BannerChecker {
    // checkAsync(firestoreSync, callback: (BannerData?) -> Unit)
}
```

- [ ] **Step 7: Java fayllarni o'chirish**

```powershell
Remove-Item "app\src\main\java\uz\kidzone\app\FirebaseManager.java"
Remove-Item "app\src\main\java\uz\kidzone\app\FirestoreSync.java"
Remove-Item "app\src\main\java\uz\kidzone\app\FcmTokenManager.java"
Remove-Item "app\src\main\java\uz\kidzone\app\BackendClient.java"
Remove-Item "app\src\main\java\uz\kidzone\app\BanChecker.java"
Remove-Item "app\src\main\java\uz\kidzone\app\BannerChecker.java"
```

- [ ] **Step 8: Kompilyatsiya tekshirish**

```powershell
.\gradlew compileDebugKotlin 2>&1 | Select-Object -Last 5
```

Kutilgan: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```powershell
git add app/src/main/java/uz/kidzone/app/
git commit -m "feat(kotlin): Firebase, FirestoreSync, FCM, BanChecker, BannerChecker"
```

---

## Task 5: Stats / Ads / WebView managerlar → Kotlin

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/IAdsManager.kt`
- Create: `app/src/main/java/uz/kidzone/app/AdsManager.kt`
- Create: `app/src/main/java/uz/kidzone/app/KidWebViewManager.kt`
- Create: `app/src/main/java/uz/kidzone/app/ParentalStatsManager.kt`
- Delete: tegishli `.java` fayllar

**Interfaces:**
- `IAdsManager` — Kotlin interface (loadBanner, showInterstitial, showRewarded, onResume, onPause, onDestroy)
- `AdsManager(activity: Activity) : IAdsManager`
- `KidWebViewManager(webView: WebView)` — setup, loadUrl, evaluateJavascript, canGoBack, goBack, destroy
- `ParentalStatsManager(context: Context)` — onSessionStart, onSessionEnd, getTodayMinutes, isTimeLimitReached, getTimeLimitMinutes, setTimeLimitMinutes, onGameLaunched, getTodayGames, getWeeklyMinutes, getSessionMinutes

- [ ] **Step 1: IAdsManager.kt yaratish**

```kotlin
package uz.kidzone.app

import android.view.ViewGroup

interface IAdsManager {
    fun initialize()
    fun loadBanner(container: ViewGroup, isTablet: Boolean)
    fun showInterstitial()
    fun showRewarded(listener: OnRewardListener)
    fun onResume()
    fun onPause()
    fun onDestroy()

    fun interface OnRewardListener {
        fun onReward(amount: Int)
    }

    interface BannerListener {
        fun onBannerLoaded(heightDp: Int)
        fun onBannerFailed()
    }
}
```

- [ ] **Step 2: AdsManager.kt yaratish**

`AdsManager.java` faylini o'qing, Kotlin ga ko'chiring. Key structural change:

```kotlin
package uz.kidzone.app

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdsManager(private val activity: Activity) : IAdsManager {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var bannerListener: IAdsManager.BannerListener? = null
    private var bannerAdView: AdView? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    override fun initialize() {
        val config = RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
            .build()
        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(activity) {
            loadInterstitial(); loadRewarded()
        }
    }

    fun loadBanner(container: ViewGroup, isTablet: Boolean, listener: IAdsManager.BannerListener?) {
        bannerListener = listener
        bannerAdView?.let { container.removeView(it); it.destroy() }
        bannerAdView = AdView(activity).apply {
            val adSize = if (isTablet) AdSize.LEADERBOARD else AdSize.BANNER
            setAdSize(adSize)
            adUnitId = activity.getString(R.string.banner_ad_unit_id)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    container.visibility = View.VISIBLE
                    bannerListener?.onBannerLoaded(adSize.height)
                }
                override fun onAdFailedToLoad(e: LoadAdError) {
                    bannerListener?.onBannerFailed()
                    mainHandler.postDelayed({ loadBanner(container, isTablet, listener) }, 30_000)
                }
            }
            container.addView(this)
            loadAd(createAdRequest())
        }
    }

    override fun loadBanner(container: ViewGroup, isTablet: Boolean) = loadBanner(container, isTablet, null)

    override fun showInterstitial() {
        interstitialAd?.show(activity) ?: Log.d("AdsManager", "Interstitial not ready")
        interstitialAd = null; loadInterstitial()
    }

    private fun loadInterstitial() {
        InterstitialAd.load(activity, activity.getString(R.string.interstitial_ad_unit_id),
            createAdRequest(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { interstitialAd = null }
            })
    }

    override fun showRewarded(listener: IAdsManager.OnRewardListener) {
        rewardedAd?.show(activity) { listener.onReward(it.amount) } ?: Log.d("AdsManager", "Rewarded not ready")
        rewardedAd = null; loadRewarded()
    }

    private fun loadRewarded() {
        RewardedAd.load(activity, activity.getString(R.string.rewarded_ad_unit_id),
            createAdRequest(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { rewardedAd = null }
            })
    }

    private fun createAdRequest(): AdRequest {
        val extras = android.os.Bundle().apply { putString("npa", "1") }
        return AdRequest.Builder()
            .addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter::class.java, extras)
            .build()
    }

    override fun onResume() { bannerAdView?.resume() }
    override fun onPause() { bannerAdView?.pause() }
    override fun onDestroy() { bannerAdView?.destroy() }
}
```

- [ ] **Step 3: KidWebViewManager.kt yaratish**

```kotlin
package uz.kidzone.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.*

class KidWebViewManager(private val webView: WebView) {
    private val TAG = "KidWebViewManager"
    var currentLanguage: String = "en"
    private var onPageReadyCallback: (() -> Unit)? = null
    private var pageReadyCalled = false

    fun setOnPageReadyCallback(callback: () -> Unit) { onPageReadyCallback = callback }

    @SuppressLint("SetJavaScriptEnabled")
    fun setup(jsInterface: Any, interfaceName: String) {
        WebView.setWebContentsDebuggingEnabled(true)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
        webView.addJavascriptInterface(jsInterface, interfaceName)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (url.startsWith("file://")) return false
                try { view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                catch (e: Exception) { Log.w(TAG, "Cannot open: $url") }
                return true
            }
            override fun onPageFinished(view: WebView, url: String) {
                if (!pageReadyCalled) {
                    pageReadyCalled = true
                    view.post { onPageReadyCallback?.invoke() }
                }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(cm: ConsoleMessage): Boolean {
                Log.d("KZ-JS", "${cm.messageLevel()} [${cm.sourceId()}:${cm.lineNumber()}] ${cm.message()}")
                return true
            }
        }
    }

    fun loadUrl(url: String) { webView.loadUrl(url) }
    fun canGoBack(): Boolean = webView.canGoBack()
    fun goBack() { webView.goBack() }
    fun evaluateJavascript(script: String) { webView.post { webView.evaluateJavascript(script, null) } }
    fun evaluateJavascript(script: String, cb: ValueCallback<String>) { webView.post { webView.evaluateJavascript(script, cb) } }
    fun destroy() { webView.stopLoading(); webView.onPause(); webView.removeAllViews(); webView.destroy() }
}
```

- [ ] **Step 4: ParentalStatsManager.kt yaratish**

`ParentalStatsManager.java` faylini o'qing (`app/src/main/java/uz/kidzone/app/ParentalStatsManager.java`), Kotlin ga ko'chiring. Barcha metodlar saqlanadi:

```kotlin
package uz.kidzone.app

import android.content.Context
import android.content.SharedPreferences

class ParentalStatsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kz_prefs", Context.MODE_PRIVATE)
    private var sessionStartMs: Long = 0L

    // Java fayldan xuddi shu logika:
    fun onSessionStart() { sessionStartMs = System.currentTimeMillis() }
    fun onSessionEnd() { /* save session minutes to kz_pt_YYYYMMDD */ }
    fun getTodayMinutes(): Int { /* read kz_pt_YYYYMMDD */ return 0 }
    fun getTimeLimitMinutes(): Int = prefs.getInt("kz_time_limit", 0)
    fun setTimeLimitMinutes(minutes: Int) { prefs.edit().putInt("kz_time_limit", minutes).apply() }
    fun isTimeLimitReached(): Boolean { /* compare today with limit */ return false }
    fun onGameLaunched(gameId: String) { /* append to kz_gl_YYYYMMDD */ }
    fun getTodayGames(): List<String> { /* read kz_gl_YYYYMMDD */ return emptyList() }
    fun getWeeklyMinutes(): List<Int> { /* read last 7 days */ return List(7) { 0 } }
    fun getSessionMinutes(): Long = if (sessionStartMs == 0L) 0L else (System.currentTimeMillis() - sessionStartMs) / 60_000
    fun getSessionGames(): List<String> = emptyList()
}
```

**Muhim:** Java faylni o'qib barcha metod implementatsiyalarini to'liq ko'chiring.

- [ ] **Step 5: Java fayllarni o'chirish**

```powershell
Remove-Item "app\src\main\java\uz\kidzone\app\IAdsManager.java"
Remove-Item "app\src\main\java\uz\kidzone\app\AdsManager.java"
Remove-Item "app\src\main\java\uz\kidzone\app\KidWebViewManager.java"
Remove-Item "app\src\main\java\uz\kidzone\app\ParentalStatsManager.java"
```

- [ ] **Step 6: Kompilyatsiya tekshirish**

```powershell
.\gradlew compileDebugKotlin 2>&1 | Select-Object -Last 5
```

Kutilgan: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/uz/kidzone/app/
git commit -m "feat(kotlin): IAdsManager, AdsManager, KidWebViewManager, ParentalStatsManager"
```

---

## Task 6: KidZoneApplication + FCM Service → Kotlin

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/KidZoneApplication.kt`
- Create: `app/src/main/java/uz/kidzone/app/KidZoneFirebaseMessagingService.kt`
- Delete: `KidZoneApplication.java`, `KidZoneFirebaseMessagingService.java`

**Interfaces:**
- `KidZoneApplication.getHttpClient(): OkHttpClient` (companion object)
- `KidZoneApplication.CHANNEL_ID: String`

- [ ] **Step 1: KidZoneApplication.kt yaratish**

```kotlin
package uz.kidzone.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.Callback
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uz.kidzone.app.BuildConfig
import java.io.IOException

class KidZoneApplication : Application() {

    companion object {
        const val CHANNEL_ID = "kidzone_push"
        @JvmStatic lateinit var httpClient: OkHttpClient
            private set
    }

    override fun onCreate() {
        super.onCreate()
        httpClient = OkHttpClient()
        if (BuildConfig.DEBUG) FirebaseFirestore.setLoggingEnabled(true)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            Log.d("KZ_DEBUG", "Auth OK: uid=${user.uid}")
            syncToFirestore(user.uid)
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    val uid = it.user?.uid ?: return@addOnSuccessListener
                    Log.d("KZ_DEBUG", "signInAnonymously OK: uid=$uid")
                    syncToFirestore(uid)
                }
                .addOnFailureListener { Log.e("KZ_DEBUG", "signInAnonymously FAILED: ${it.message}") }
        }
        createNotificationChannel()
    }

    private fun syncToFirestore(uid: String) {
        val prefs: SharedPreferences = getSharedPreferences("kz_prefs", MODE_PRIVATE)
        prefs.edit().putString("kz_uid", uid).apply()
        val ageGroup = prefs.getString("kz_age_filter", "3-5") ?: "3-5"
        FirestoreSync.init(this).syncUserProfile(uid, null, null, ageGroup)
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                prefs.edit().putString("kz_fcm_token", token).apply()
                FirestoreSync.getInstance().updateFcmToken(uid, token)
                registerTokenWithBackend(token)
            }
            .addOnFailureListener { Log.w("KZ_DEBUG", "FCM token fetch failed: ${it.message}") }
    }

    private fun registerTokenWithBackend(fcmToken: String) {
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(false)
            ?.addOnSuccessListener { result ->
                val idToken = result.token ?: return@addOnSuccessListener
                val json = """{"fcmToken":"$fcmToken"}"""
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://kidzone-backend-s7to.onrender.com/push/register-token")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(body)
                    .build()
                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.w("KZ_DEBUG", "registerToken failed: ${e.message}")
                    }
                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        Log.d("KZ_DEBUG", "registerToken HTTP ${response.code}")
                        response.close()
                    }
                })
            }
            ?.addOnFailureListener { Log.w("KZ_DEBUG", "getIdToken failed: ${it.message}") }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "KidZone Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
```

- [ ] **Step 2: KidZoneFirebaseMessagingService.kt yaratish**

`KidZoneFirebaseMessagingService.java` faylini o'qing, Kotlin ga ko'chiring:

```kotlin
package uz.kidzone.app

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class KidZoneFirebaseMessagingService : FirebaseMessagingService() {
    // Java fayldan xuddi shu logika:
    // - onNewToken() → FirestoreSync.updateFcmToken
    // - onMessageReceived() → kz_push_enabled tekshirish + notification ko'rsatish + tarix saqlash
}
```

- [ ] **Step 3: Java fayllarni o'chirish**

```powershell
Remove-Item "app\src\main\java\uz\kidzone\app\KidZoneApplication.java"
Remove-Item "app\src\main\java\uz\kidzone\app\KidZoneFirebaseMessagingService.java"
```

- [ ] **Step 4: Kompilyatsiya tekshirish**

```powershell
.\gradlew compileDebugKotlin 2>&1 | Select-Object -Last 5
```

Kutilgan: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/uz/kidzone/app/
git commit -m "feat(kotlin): KidZoneApplication, KidZoneFirebaseMessagingService"
```

---

## Task 7: Kidzo subsystem → Kotlin + Compose

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/kidzo/GeminiCaller.kt`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/RealGeminiCaller.kt`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/ContentFilter.kt`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/ActionParser.kt`
- Create: `app/src/main/java/uz/kidzone/app/kidzo/KidzoAgent.kt`
- Create: `app/src/main/java/uz/kidzone/app/ui/viewmodel/KidzoViewModel.kt`
- Create: `app/src/main/java/uz/kidzone/app/ui/screens/KidzoSheet.kt`
- Delete: tegishli `.java` fayllar

**Interfaces:**
- `KidzoAgent.createStatic(filter, mainThread): KidzoAgent`
- `KidzoAgent.requestRecommendations()`
- `KidzoAgent.sendChatMessage(text: String)`
- `KidzoViewModel.state: StateFlow<KidzoUiState>`
- `KidzoViewModel.cards: StateFlow<List<ContentCard>>`

- [ ] **Step 1: GeminiCaller.kt va RealGeminiCaller.kt yaratish**

`GeminiCaller.java` va `RealGeminiCaller.java` fayllarini o'qing, Kotlin ga ko'chiring. Java interface → Kotlin fun interface:

```kotlin
package uz.kidzone.app.kidzo

fun interface GeminiCaller {
    fun call(prompt: String, onResult: (String) -> Unit, onError: (String) -> Unit)
}
```

- [ ] **Step 2: ContentFilter.kt va ActionParser.kt yaratish**

`ContentFilter.java` va `ActionParser.java` fayllarini o'qing, Kotlin ga ko'chiring. `fromAssets()` factory metod `companion object` da bo'ladi.

- [ ] **Step 3: KidzoAgent.kt yaratish**

`KidzoAgent.java` faylini o'qing (190 qator), Kotlin ga ko'chiring. Key structural changes:
- `KidzoStateListener` → `MutableStateFlow<KidzoState>` 
- `mainThread: Runnable → Unit` → `mainThread: (() -> Unit) -> Unit`
- `createStatic` factory `companion object` da

```kotlin
package uz.kidzone.app.kidzo

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class KidzoAgent private constructor(
    private val gemini: GeminiCaller,
    private val contentFilter: ContentFilter,
    private val mainThread: (() -> Unit) -> Unit,
) {
    private val _state = MutableStateFlow(KidzoState.IDLE)
    val state: StateFlow<KidzoState> = _state

    private val _cards = MutableStateFlow<List<ContentCard>>(emptyList())
    val cards: StateFlow<List<ContentCard>> = _cards

    private val _chatMessages = MutableStateFlow<List<String>>(emptyList())
    val chatMessages: StateFlow<List<String>> = _chatMessages

    private var onContentOpen: ((String) -> Unit)? = null
    fun setOnContentOpen(cb: (String) -> Unit) { onContentOpen = cb }

    companion object {
        fun createStatic(filter: ContentFilter, mainThread: (() -> Unit) -> Unit): KidzoAgent {
            // GeminiCaller.java dagi createStatic logikasi
            return KidzoAgent(GeminiCaller { _, _, _ -> }, filter, mainThread)
        }
        fun create(filter: ContentFilter, mainThread: (() -> Unit) -> Unit, context: Context): KidzoAgent {
            val gemini = RealGeminiCaller(context)
            return KidzoAgent(gemini, filter, mainThread)
        }
    }

    // Java fayldan barcha metodlar: requestRecommendations, sendChatMessage, openContent
    fun requestRecommendations() { /* Java logikasi */ }
    fun sendChatMessage(text: String) { /* Java logikasi */ }
    fun openContent(contentId: String) { onContentOpen?.invoke(contentId) }
}
```

- [ ] **Step 4: KidzoViewModel.kt yaratish**

```kotlin
package uz.kidzone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import uz.kidzone.app.kidzo.ContentCard
import uz.kidzone.app.kidzo.KidzoAgent
import uz.kidzone.app.kidzo.KidzoState

class KidzoViewModel(val agent: KidzoAgent) : ViewModel() {
    val state: StateFlow<KidzoState> = agent.state
    val cards: StateFlow<List<ContentCard>> = agent.cards
    val chatMessages: StateFlow<List<String>> = agent.chatMessages

    fun requestRecommendations() { agent.requestRecommendations() }
    fun sendMessage(text: String) { agent.sendChatMessage(text) }
}
```

- [ ] **Step 5: KidzoSheet.kt yaratish**

```kotlin
package uz.kidzone.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.kidzone.app.kidzo.ContentCard
import uz.kidzone.app.ui.viewmodel.KidzoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidzoSheet(
    viewModel: KidzoViewModel,
    onContentSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cards by viewModel.cards.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    var inputText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("🐥 Kidzo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Content cards
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cards) { card ->
                    KidzoCardItem(card = card, onClick = {
                        onContentSelected(card.contentId)
                        onDismiss()
                    })
                }
            }

            Spacer(Modifier.height(12.dp))

            // Chat messages
            messages.takeLast(3).forEach { msg ->
                Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Chat input
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Savol bering...") },
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                }) { Text("→") }
            }
        }
    }
}

@Composable
private fun KidzoCardItem(card: ContentCard, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(120.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(card.emoji, style = MaterialTheme.typography.headlineMedium)
            Text(card.displayText, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}
```

- [ ] **Step 6: Java fayllarni o'chirish**

```powershell
Remove-Item "app\src\main\java\uz\kidzone\app\kidzo\GeminiCaller.java"
Remove-Item "app\src\main\java\uz\kidzone\app\kidzo\RealGeminiCaller.java"
Remove-Item "app\src\main\java\uz\kidzone\app\kidzo\ContentFilter.java"
Remove-Item "app\src\main\java\uz\kidzone\app\kidzo\ActionParser.java"
Remove-Item "app\src\main\java\uz\kidzone\app\kidzo\KidzoAgent.java"
Remove-Item "app\src\main\java\uz\kidzone\app\kidzo\KidzoBottomSheet.java"
Remove-Item "app\src\main\java\uz\kidzone\app\kidzo\KidzoCardAdapter.java"
```

- [ ] **Step 7: Kompilyatsiya tekshirish**

```powershell
.\gradlew compileDebugKotlin 2>&1 | Select-Object -Last 5
```

Kutilgan: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/java/uz/kidzone/app/kidzo/ app/src/main/java/uz/kidzone/app/ui/
git commit -m "feat(kotlin): Kidzo subsystem — KidzoAgent.kt + KidzoSheet Compose"
```

---

## Task 8: MainActivity + NavHost + MainScreen + MainViewModel

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/MainActivity.kt`
- Create: `app/src/main/java/uz/kidzone/app/ui/KidZoneApp.kt`
- Create: `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt`
- Create: `app/src/main/java/uz/kidzone/app/ui/viewmodel/MainViewModel.kt`
- Delete: `MainActivity.java`
- Delete: `activity_main.xml`, `dialog_exit.xml`, `view_lock_overlay.xml`

**Interfaces:**
- `MainUiState` — til, yosh, inGame, showExitDialog, promoBanner, isLocked, bannerVisible
- `MainViewModel.showExitDialog()`, `dismissExitDialog()`, `showLock()`, `hideLock()`, `setPromoBanner()`
- `MainScreen(mainViewModel, adsManager, onOpenDashboard)`

- [ ] **Step 1: MainViewModel.kt yaratish**

```kotlin
package uz.kidzone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PromoBannerData(val title: String, val body: String, val url: String)

data class MainUiState(
    val language: String = "uz",
    val age: String = "2-4",
    val inGame: Boolean = false,
    val showExitDialog: Boolean = false,
    val isExitFromGame: Boolean = false,
    val promoBanner: PromoBannerData? = null,
    val isLocked: Boolean = false,
    val bannerVisible: Boolean = true,
)

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    fun setLanguage(lang: String) { _state.update { it.copy(language = lang) } }
    fun setAge(age: String) { _state.update { it.copy(age = age) } }
    fun setInGame(inGame: Boolean) { _state.update { it.copy(inGame = inGame) } }
    fun showExitDialog(fromGame: Boolean) { _state.update { it.copy(showExitDialog = true, isExitFromGame = fromGame) } }
    fun dismissExitDialog() { _state.update { it.copy(showExitDialog = false) } }
    fun setPromoBanner(data: PromoBannerData?) { _state.update { it.copy(promoBanner = data) } }
    fun showLock() { _state.update { it.copy(isLocked = true) } }
    fun hideLock() { _state.update { it.copy(isLocked = false) } }
    fun setBannerVisible(visible: Boolean) { _state.update { it.copy(bannerVisible = visible) } }
}
```

- [ ] **Step 2: KidZoneApp.kt yaratish**

```kotlin
package uz.kidzone.app.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.kidzone.app.AdsManager
import uz.kidzone.app.ui.screens.OnboardingScreen
import uz.kidzone.app.ui.screens.ParentDashboardScreen
import uz.kidzone.app.ui.viewmodel.MainViewModel

@Composable
fun KidZoneApp(
    prefs: SharedPreferences,
    mainViewModel: MainViewModel,
    adsManager: AdsManager,
) {
    val navController = rememberNavController()
    val onboardingDone = prefs.getBoolean("kz_onboarding_done", false)

    NavHost(
        navController = navController,
        startDestination = if (onboardingDone) "main" else "onboarding",
    ) {
        composable("onboarding") {
            OnboardingScreen(
                prefs = prefs,
                onDone = {
                    prefs.edit().putBoolean("kz_onboarding_done", true).apply()
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(
                mainViewModel = mainViewModel,
                adsManager = adsManager,
                prefs = prefs,
                onOpenDashboard = { navController.navigate("dashboard") },
            )
        }
        composable("dashboard") {
            ParentDashboardScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
```

- [ ] **Step 3: MainScreen.kt yaratish**

```kotlin
package uz.kidzone.app.ui

import android.content.SharedPreferences
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import uz.kidzone.app.*
import uz.kidzone.app.ui.screens.KidzoSheet
import uz.kidzone.app.ui.viewmodel.KidzoViewModel
import uz.kidzone.app.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    adsManager: AdsManager,
    prefs: SharedPreferences,
    onOpenDashboard: () -> Unit,
) {
    val uiState by mainViewModel.state.collectAsState()
    val context = LocalContext.current
    val webMgrRef = remember { mutableStateOf<KidWebViewManager?>(null) }
    var showKidzoSheet by remember { mutableStateOf(false) }
    val kidzoAgentRef = remember {
        val filter = try { uz.kidzone.app.kidzo.ContentFilter.fromAssets(context) }
                     catch (e: Exception) { null }
        filter?.let { uz.kidzone.app.kidzo.KidzoAgent.createStatic(it) { r -> (context as? android.app.Activity)?.runOnUiThread(r) } }
    }
    val kidzoViewModel = remember(kidzoAgentRef) {
        kidzoAgentRef?.let { KidzoViewModel(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    val mgr = KidWebViewManager(this)
                    val lang = prefs.getString("kz_lang", "uz") ?: "uz"
                    val age = prefs.getString("kz_age", "2-4") ?: "2-4"
                    mgr.setup(AdMobBridge(mainViewModel, adsManager, onOpenDashboard), "AndroidAdMob")
                    mgr.setOnPageReadyCallback {
                        mgr.evaluateJavascript(
                            "localStorage.setItem('kz-lang','$lang');localStorage.setItem('kz-age','$age');"
                        )
                    }
                    mgr.loadUrl("file:///android_asset/www/index.html")
                    webMgrRef.value = mgr
                    addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: android.view.View) {}
                        override fun onViewDetachedFromWindow(v: android.view.View) { mgr.destroy() }
                    })
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Lock overlay
        if (uiState.isLocked) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌙", style = MaterialTheme.typography.displayLarge)
                    Text("Bugungi vaqting tugadi", color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { mainViewModel.hideLock() }) {
                        Text("Ota-ona uchun 🔐")
                    }
                }
            }
        }

        // Promo banner
        AnimatedVisibility(
            visible = uiState.promoBanner != null,
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            uiState.promoBanner?.let { banner ->
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(banner.title, style = MaterialTheme.typography.titleSmall)
                            Text(banner.body, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { mainViewModel.setPromoBanner(null) }) { Text("✕") }
                    }
                }
            }
        }

        // FAB (Kidzo) — faqat main ekranda
        if (!uiState.inGame && kidzoViewModel != null) {
            val pulseScale by animateFloatAsState(
                targetValue = 1.12f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(900),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                ),
                label = "pulse",
            )
            FloatingActionButton(
                onClick = {
                    kidzoViewModel.requestRecommendations()
                    showKidzoSheet = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
            ) {
                Text("🐥", style = MaterialTheme.typography.titleLarge)
            }
        }
    }

    // BackHandler
    BackHandler {
        if (uiState.isLocked) return@BackHandler
        val mgr = webMgrRef.value
        webMgrRef.value?.evaluateJavascript(
            "document.getElementById('gv') ? !document.getElementById('gv').classList.contains('h') : false"
        ) { result ->
            if (result == "true") {
                mainViewModel.showExitDialog(true)
            } else if (mgr?.canGoBack() == true) {
                mgr.goBack()
            } else {
                mainViewModel.showExitDialog(false)
            }
        }
    }

    // Exit dialog
    if (uiState.showExitDialog) {
        AlertDialog(
            onDismissRequest = { mainViewModel.dismissExitDialog() },
            title = { Text(if (uiState.isExitFromGame) "O'yindan chiqish?" else "KidZone'dan chiqish?") },
            text = { Text(if (uiState.isExitFromGame) "Menyuga qaytmoqchimisiz?" else "Haqiqatan ham chiqib ketmoqchimisiz?") },
            confirmButton = {
                TextButton(onClick = {
                    mainViewModel.dismissExitDialog()
                    if (uiState.isExitFromGame) {
                        webMgrRef.value?.evaluateJavascript(
                            "(document.getElementById('gv-back')||{click:function(){}}).click();"
                        )
                    } else {
                        (context as? android.app.Activity)?.finishAffinity()
                    }
                }) { Text(if (uiState.isExitFromGame) "Ha, menyuga" else "Ha, chiqish") }
            },
            dismissButton = {
                TextButton(onClick = { mainViewModel.dismissExitDialog() }) { Text("Yo'q, qolish") }
            },
        )
    }

    // Kidzo sheet
    if (showKidzoSheet && kidzoViewModel != null) {
        KidzoSheet(
            viewModel = kidzoViewModel,
            onContentSelected = { contentId ->
                webMgrRef.value?.evaluateJavascript("if(window.playContent)playContent('$contentId')")
            },
            onDismiss = { showKidzoSheet = false },
        )
    }
}

// AdMob JavaScript bridge
private class AdMobBridge(
    private val viewModel: MainViewModel,
    private val adsManager: AdsManager,
    private val onOpenDashboard: () -> Unit,
) {
    @android.webkit.JavascriptInterface
    fun showBanner() { viewModel.setInGame(false); viewModel.setBannerVisible(true) }

    @android.webkit.JavascriptInterface
    fun hideBanner() { viewModel.setInGame(true); viewModel.setBannerVisible(false) }

    @android.webkit.JavascriptInterface
    fun showInterstitial() { adsManager.showInterstitial() }

    @android.webkit.JavascriptInterface
    fun showRewarded() { /* adsManager.showRewarded { ... } */ }

    @android.webkit.JavascriptInterface
    fun toggleMusic(mute: Boolean) { MusicManager.setMuted(mute) }

    @android.webkit.JavascriptInterface
    fun updateLanguage(lang: String) { viewModel.setLanguage(lang) }

    @android.webkit.JavascriptInterface
    fun openParentalDashboard() { onOpenDashboard() }

    @android.webkit.JavascriptInterface
    fun gameLaunched(gameId: String) { viewModel.setInGame(true) }
}
```

**Izoh:** `animateFloatAsState` va `graphicsLayer` uchun import qo'shiladi. `AdMobBridge` inner class emas — alohida `private class` sifatida.

- [ ] **Step 4: MainActivity.kt yaratish**

```kotlin
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
import uz.kidzone.app.ui.KidZoneApp
import uz.kidzone.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var adsManager: AdsManager
    private lateinit var statsManager: ParentalStatsManager
    private lateinit var systemUiHelper: SystemUiHelper
    private lateinit var firestoreSync: FirestoreSync
    private val kzPrefs by lazy { getSharedPreferences("kz_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

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
                    mainViewModel.setPromoBanner(uz.kidzone.app.ui.viewmodel.PromoBannerData(
                        title = banner.title, body = banner.body, url = banner.url,
                    ))
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
        adsManager.onDestroy()
        super.onDestroy()
    }

    private fun checkPendingUrl() {
        val url = kzPrefs.getString("kz_pending_url", null) ?: return
        kzPrefs.edit().remove("kz_pending_url").apply()
        // openUrl(url) — MainActivity.java dagi openUrl logikasi
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) return
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }
}
```

- [ ] **Step 5: AndroidManifest.xml ni yangilash**

`OnboardingActivity` va `ParentalDashboardActivity` yozuvlarini o'chiring — ular artiq Activity emas:

`app/src/main/AndroidManifest.xml` dan bu blokni o'chiring:
```xml
<!-- Bu blokni O'CHIRING: -->
<activity android:name=".OnboardingActivity" ... />
<activity android:name=".ParentalDashboardActivity" ... />
```

- [ ] **Step 6: Java MainActivity va XML layoutlarni o'chirish**

```powershell
Remove-Item "app\src\main\java\uz\kidzone\app\MainActivity.java"
Remove-Item "app\src\main\res\layout\activity_main.xml"
Remove-Item "app\src\main\res\layout\dialog_exit.xml"
Remove-Item "app\src\main\res\layout\view_lock_overlay.xml"
```

- [ ] **Step 7: `installDebug` — MainScreen ishlashini tekshirish**

```powershell
.\gradlew installDebug 2>&1 | Select-Object -Last 10
```

Kutilgan: `Installed on 1 device.`

App ochilganda WebView va HTML5 o'yinlar ishlashi kerak.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/java/uz/kidzone/app/ app/src/main/AndroidManifest.xml app/src/main/res/layout/
git commit -m "feat(compose): MainActivity + MainScreen + MainViewModel — Bosqich 3"
```

---

## Task 9: OnboardingScreen + OnboardingViewModel

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/ui/screens/OnboardingScreen.kt`
- Create: `app/src/main/java/uz/kidzone/app/ui/viewmodel/OnboardingViewModel.kt`
- Delete: `OnboardingActivity.java`, `activity_onboarding.xml`

**Interfaces:**
- `OnboardingScreen(prefs: SharedPreferences, onDone: () -> Unit)`
- `OnboardingViewModel` — step (0..2), lang, age, name

- [ ] **Step 1: OnboardingViewModel.kt yaratish**

```kotlin
package uz.kidzone.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OnboardingState(
    val step: Int = 0,
    val lang: String = "uz",
    val age: String = "2-4",
    val childName: String = "",
)

class OnboardingViewModel : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun setLang(lang: String) { _state.update { it.copy(lang = lang, step = 1) } }
    fun setAge(age: String) { _state.update { it.copy(age = age) } }
    fun setChildName(name: String) { _state.update { it.copy(childName = name) } }
    fun nextStep() { _state.update { it.copy(step = it.step + 1) } }
    fun prevStep() { if (_state.value.step > 0) _state.update { it.copy(step = it.step - 1) } }
}
```

- [ ] **Step 2: OnboardingScreen.kt yaratish**

`OnboardingActivity.java` faylini o'qing (214 qator). 3 bosqichli flow:

```kotlin
package uz.kidzone.app.ui.screens

import android.content.SharedPreferences
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.kidzone.app.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    prefs: SharedPreferences,
    onDone: () -> Unit,
    vm: OnboardingViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()

    AnimatedContent(targetState = state.step, label = "onboarding_step") { step ->
        when (step) {
            0 -> LangStep(onSelect = { lang ->
                prefs.edit().putString("kz_lang", lang).apply()
                vm.setLang(lang)
            })
            1 -> AgeStep(onSelect = { age ->
                prefs.edit().putString("kz_age", age).apply()
                vm.setAge(age); vm.nextStep()
            })
            2 -> WelcomeStep(lang = state.lang, onDone = {
                prefs.edit().putBoolean("kz_onboarding_done", true).apply()
                onDone()
            })
        }
    }
}

@Composable
private fun LangStep(onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🌍", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("Tilni tanlang / Выберите язык / Choose language", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        listOf("uz" to "O'zbek 🇺🇿", "ru" to "Русский 🇷🇺", "en" to "English 🇬🇧").forEach { (code, label) ->
            Button(onClick = { onSelect(code) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(label)
            }
        }
    }
}

@Composable
private fun AgeStep(onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("👶", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("Yosh guruhini tanlang", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        listOf("2-4" to "2–4 yosh 🐣", "5-7" to "5–7 yosh 🐥", "8+" to "8+ yosh 🐦").forEach { (code, label) ->
            Button(onClick = { onSelect(code) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(label)
            }
        }
    }
}

@Composable
private fun WelcomeStep(lang: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        val welcomeText = when (lang) {
            "uz" -> "KidZone ga xush kelibsiz!"
            "ru" -> "Добро пожаловать в KidZone!"
            else -> "Welcome to KidZone!"
        }
        Text(welcomeText, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(when (lang) { "uz" -> "Boshlash 🚀"; "ru" -> "Начать 🚀"; else -> "Start 🚀" })
        }
    }
}
```

- [ ] **Step 3: Java faylni o'chirish**

```powershell
Remove-Item "app\src\main\java\uz\kidzone\app\OnboardingActivity.java"
Remove-Item "app\src\main\res\layout\activity_onboarding.xml"
```

- [ ] **Step 4: `installDebug` — Onboarding ishlashini tekshirish**

```powershell
.\gradlew installDebug 2>&1 | Select-Object -Last 10
```

Yangi qurilmada app ochilganda OnboardingScreen ko'rinishi kerak.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/uz/kidzone/app/ui/ app/src/main/res/layout/
git commit -m "feat(compose): OnboardingScreen + OnboardingViewModel — Bosqich 4"
```

---

## Task 10: ParentDashboardScreen + DashboardViewModel

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt`
- Create: `app/src/main/java/uz/kidzone/app/ui/viewmodel/DashboardViewModel.kt`
- Delete: `ParentalDashboardActivity.java`, `activity_parental_dashboard.xml`, `dialog_pin.xml`, `dialog_email_auth.xml`

**Interfaces:**
- `ParentDashboardScreen(prefs: SharedPreferences, onBack: () -> Unit)`
- `DashboardViewModel(statsManager: ParentalStatsManager, prefs: SharedPreferences)`

- [ ] **Step 1: DashboardViewModel.kt yaratish**

```kotlin
package uz.kidzone.app.ui.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.kidzone.app.ParentalStatsManager

data class DashboardState(
    val todayMinutes: Int = 0,
    val weeklyMinutes: List<Int> = List(7) { 0 },
    val todayGames: List<String> = emptyList(),
    val timeLimitMinutes: Int = 0,
    val age: String = "2-4",
    val pushEnabled: Boolean = true,
    val notifHistory: List<String> = emptyList(),
    val firebaseUid: String? = null,
    val firebaseEmail: String? = null,
)

class DashboardViewModel(
    private val statsManager: ParentalStatsManager,
    private val prefs: SharedPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update {
            it.copy(
                todayMinutes = statsManager.getTodayMinutes().toInt(),
                weeklyMinutes = statsManager.getWeeklyMinutes(),
                todayGames = statsManager.getTodayGames(),
                timeLimitMinutes = statsManager.getTimeLimitMinutes(),
                age = prefs.getString("kz_age", "2-4") ?: "2-4",
                pushEnabled = prefs.getBoolean("kz_push_enabled", true),
            )
        }
    }

    fun increaseLimit() {
        val current = statsManager.getTimeLimitMinutes()
        val next = if (current >= 180) 180 else current + 15
        statsManager.setTimeLimitMinutes(next)
        _state.update { it.copy(timeLimitMinutes = next) }
    }

    fun decreaseLimit() {
        val current = statsManager.getTimeLimitMinutes()
        val next = if (current <= 0) 0 else current - 15
        statsManager.setTimeLimitMinutes(next)
        _state.update { it.copy(timeLimitMinutes = next) }
    }

    fun setAge(age: String) {
        prefs.edit().putString("kz_age", age).apply()
        _state.update { it.copy(age = age) }
    }

    fun setPushEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("kz_push_enabled", enabled).apply()
        _state.update { it.copy(pushEnabled = enabled) }
    }
}
```

- [ ] **Step 2: ParentDashboardScreen.kt yaratish**

`ParentalDashboardActivity.java` faylini o'qing (805 qator). Compose versiyasi (seksiyalar saqlanadi, admin panel qoldiriladi):

```kotlin
package uz.kidzone.app.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.PinUtil
import uz.kidzone.app.ui.viewmodel.DashboardViewModel

@Composable
fun ParentDashboardScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val statsManager = remember { ParentalStatsManager(context) }
    val vm = remember { DashboardViewModel(statsManager, prefs) }
    val state by vm.state.collectAsState()

    // PIN gate
    var pinVerified by remember { mutableStateOf(false) }
    val savedPinHash = remember { PinUtil.getOrMigrateHash(prefs, "kz_pin") }

    if (!pinVerified) {
        PinGate(
            hasPinSet = !savedPinHash.isNullOrEmpty(),
            onPinCorrect = { pin ->
                if (savedPinHash.isNullOrEmpty() || PinUtil.matches(pin, savedPinHash)) {
                    pinVerified = true
                }
            },
            onBack = onBack,
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ota-ona paneli") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.padding(padding)) {

            // 1. Statistika
            item {
                Text("📊 Bugun: ${state.todayMinutes} daqiqa", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                WeeklyChart(state.weeklyMinutes)
                Spacer(Modifier.height(16.dp))
            }

            // 2. Yosh guruhi
            item {
                Text("👶 Yosh guruhi", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("2-4", "5-7", "8+").forEach { age ->
                        FilterChip(
                            selected = state.age == age,
                            onClick = { vm.setAge(age) },
                            label = { Text(age) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 3. Vaqt limiti
            item {
                Text("⏰ Vaqt limiti", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { vm.decreaseLimit() }) { Text("−") }
                    Spacer(Modifier.width(12.dp))
                    Text(if (state.timeLimitMinutes == 0) "Limit yo'q" else "${state.timeLimitMinutes} daqiqa",
                         style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = { vm.increaseLimit() }) { Text("+") }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 4. PIN o'zgartirish
            item {
                var showChangePinDialog by remember { mutableStateOf(false) }
                Button(onClick = { showChangePinDialog = true }) { Text("🔐 PIN o'zgartirish") }
                if (showChangePinDialog) {
                    ChangePinDialog(
                        prefs = prefs,
                        currentHash = savedPinHash,
                        onDismiss = { showChangePinDialog = false },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // 5. Push bildirishnomalar
            item {
                Text("🔔 Push bildirishnomalar", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bildirishnomalar", modifier = Modifier.weight(1f))
                    Switch(checked = state.pushEnabled, onCheckedChange = { vm.setPushEnabled(it) })
                }
            }
        }
    }
}

@Composable
private fun WeeklyChart(weeklyMinutes: List<Int>) {
    val maxMin = (weeklyMinutes.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
    val barColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        val barWidth = size.width / 7
        weeklyMinutes.forEachIndexed { i, minutes ->
            val barHeight = (minutes / maxMin) * size.height
            drawRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(i * barWidth + 4f, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth - 8f, barHeight),
            )
        }
    }
}

@Composable
private fun PinGate(
    hasPinSet: Boolean,
    onPinCorrect: (String) -> Unit,
    onBack: () -> Unit,
) {
    var entered by remember { mutableStateOf("") }
    var shakeError by remember { mutableStateOf(false) }

    if (!hasPinSet) { onPinCorrect(""); return }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🔐 PIN kiriting", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (i < entered.length) "●" else "○", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        PinKeypad(onDigit = { d ->
            if (entered.length < 4) {
                entered += d
                if (entered.length == 4) onPinCorrect(entered)
            }
        }, onBackspace = {
            if (entered.isNotEmpty()) entered = entered.dropLast(1)
        })
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text("Bekor qilish") }
    }
}

@Composable
private fun PinKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","⌫")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { key ->
                    if (key.isEmpty()) { Box(Modifier.size(64.dp)) }
                    else {
                        FilledTonalButton(
                            onClick = { if (key == "⌫") onBackspace() else onDigit(key) },
                            modifier = Modifier.size(64.dp),
                        ) { Text(key) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChangePinDialog(prefs: SharedPreferences, currentHash: String?, onDismiss: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN o'zgartirish") },
        text = {
            Column {
                when (step) {
                    0 -> {
                        Text("Joriy PINni kiriting")
                        OutlinedTextField(value = currentPin, onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) currentPin = it },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                    }
                    1 -> {
                        Text("Yangi PINni kiriting")
                        OutlinedTextField(value = newPin, onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) newPin = it },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                    }
                    2 -> {
                        var confirmPin by remember { mutableStateOf("") }
                        Text("Yangi PINni tasdiqlang")
                        OutlinedTextField(value = confirmPin, onValueChange = {
                            if (it.length <= 4 && it.all(Char::isDigit)) {
                                confirmPin = it
                                if (it.length == 4) {
                                    if (it == newPin) {
                                        prefs.edit().putString("kz_pin", PinUtil.hash(newPin)).apply()
                                        onDismiss()
                                    } else { error = "PIN mos kelmadi"; step = 1; newPin = "" }
                                }
                            }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                    }
                }
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (step) {
                    0 -> {
                        if (currentHash.isNullOrEmpty() || PinUtil.matches(currentPin, currentHash)) { step = 1; error = "" }
                        else { error = "PIN noto'g'ri" }
                    }
                    1 -> { if (newPin.length == 4) { step = 2; error = "" } }
                }
            }) { Text("Keyingi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor") } },
    )
}
```

- [ ] **Step 3: Java fayllarni o'chirish**

```powershell
Remove-Item "app\src\main\java\uz\kidzone\app\ParentalDashboardActivity.java"
Remove-Item "app\src\main\res\layout\activity_parental_dashboard.xml"
Remove-Item "app\src\main\res\layout\dialog_pin.xml"
Remove-Item "app\src\main\res\layout\dialog_email_auth.xml"
Remove-Item "app\src\main\res\layout\bottom_sheet_kidzo.xml"
Remove-Item "app\src\main\res\layout\item_kidzo_card.xml"
```

- [ ] **Step 4: `installDebug` — Dashboard ishlashini tekshirish**

```powershell
.\gradlew installDebug 2>&1 | Select-Object -Last 10
```

O'yinlar ekranida "🔒" belgisi → PIN → Dashboard ochilishi kerak.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/uz/kidzone/app/ui/ app/src/main/res/layout/
git commit -m "feat(compose): ParentDashboardScreen + DashboardViewModel — Bosqich 5-6"
```

---

## Spec coverage tekshiruvi

| Spec talabi | Task |
|-------------|------|
| Barcha Java fayllar o'chirilgan | Task 3-10 |
| Compose BOM 2024.04.01 | Task 1 |
| 3 Activity → 1 ComponentActivity + NavHost | Task 8 |
| MusicManager.kt object singleton | Task 3 |
| PinUtil.kt SHA-256 | Task 3 |
| FirebaseManager.kt | Task 4 |
| AdsManager.kt COPPA compliant | Task 5 |
| KidWebViewManager.kt | Task 5 |
| KidzoSheet ModalBottomSheet | Task 7 |
| OnboardingScreen 3 bosqich | Task 9 |
| ParentDashboardScreen PIN gate + stats | Task 10 |
| ChangePinDialog 3 bosqich | Task 10 |
