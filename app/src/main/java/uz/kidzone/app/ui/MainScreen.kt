package uz.kidzone.app.ui

import android.app.Activity
import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import java.lang.ref.WeakReference
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import uz.kidzone.app.PinUtil
import uz.kidzone.app.ui.screens.PinGate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import uz.kidzone.app.AdsManager
import uz.kidzone.app.IAdsManager
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.KidWebViewManager
import uz.kidzone.app.MusicManager
import uz.kidzone.app.kidzo.ContentFilter
import uz.kidzone.app.kidzo.KidzoAgent
import uz.kidzone.app.ui.screens.DailyChallengeCard
import uz.kidzone.app.ui.screens.KidzoSheet
import uz.kidzone.app.ui.viewmodel.ChallengeState
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModel
import uz.kidzone.app.ui.viewmodel.KidzoViewModel
import uz.kidzone.app.ui.viewmodel.MainViewModel
import uz.kidzone.app.ui.viewmodel.ProfileViewModel

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    adsManager: AdsManager,
    prefs: SharedPreferences,
    statsManager: ParentalStatsManager,
    profileViewModel: ProfileViewModel,
    challengeViewModel: DailyChallengeViewModel,
    onOpenDashboard: () -> Unit,
) {
    val uiState by mainViewModel.state.collectAsState()
    val activeProfile by profileViewModel.activeProfile.collectAsState()
    val challengeState by challengeViewModel.state.collectAsState()

    // Profil o'zgarganda challengeViewModel ga xabar ber
    LaunchedEffect(activeProfile) {
        activeProfile?.id?.let { challengeViewModel.onProfileChanged(it) }
    }
    val context = LocalContext.current
    val webMgrRef = remember { mutableStateOf<KidWebViewManager?>(null) }
    var showKidzoSheet by remember { mutableStateOf(false) }

    val kidzoAgentRef = remember {
        val filter = try { ContentFilter.fromAssets(context) } catch (e: Exception) { null }
        filter?.let {
            KidzoAgent.createStatic(it) { block -> android.os.Handler(android.os.Looper.getMainLooper()).post(block) }
        }
    }
    val kidzoViewModel = remember(kidzoAgentRef) {
        kidzoAgentRef?.let { KidzoViewModel(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Daily Challenge Card — o'yin ko'rinisida emas, lock holatida emas
        DailyChallengeCard(
            streakCount = challengeState.streakCount,
            challenge = challengeState.challenge,
            visible = !uiState.inGame && !uiState.isLocked,
            onPlay = { gameId ->
                webMgrRef.value?.evaluateJavascript(
                    "if(window.app){app.openGame(app.games.find(function(g){return g.id==='$gameId';}))||null}"
                )
            },
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        // WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    val mgr = KidWebViewManager(this)
                    val lang = activeProfile?.language ?: prefs.getString("kz_lang", "uz") ?: "uz"
                    val age = prefs.getString("kz_age", "2-4") ?: "2-4"
                    mgr.setup(
                        AdMobBridge(mainViewModel, adsManager, onOpenDashboard, context as Activity),
                        "AndroidAdMob",
                    )
                    mgr.addInterface(
                        ChallengeBridge(challengeViewModel, context as Activity),
                        "AndroidChallenge",
                    )
                    mgr.setOnPageReadyCallback {
                        mgr.evaluateJavascript(
                            "localStorage.setItem('kz-lang','$lang');" +
                                "localStorage.setItem('kz-age','$age');"
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
            var showUnlockPin by remember { mutableStateOf(false) }
            val savedPinHash = activeProfile?.pinHash
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌙", style = MaterialTheme.typography.displayLarge)
                    Text("Bugungi vaqting tugadi", color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showUnlockPin = true }) {
                        Text("Ota-ona uchun 🔐")
                    }
                }
            }
            if (showUnlockPin) {
                PinGate(
                    hasPinSet = !savedPinHash.isNullOrEmpty(),
                    onPinCorrect = { pin ->
                        if (savedPinHash.isNullOrEmpty() || PinUtil.matches(pin, savedPinHash)) {
                            mainViewModel.hideLock()
                            showUnlockPin = false
                        }
                    },
                    onBack = { showUnlockPin = false },
                )
            }
        }

        // Promo banner
        androidx.compose.animation.AnimatedVisibility(
            visible = uiState.promoBanner != null,
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            uiState.promoBanner?.let { banner ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(banner.title, style = MaterialTheme.typography.titleSmall)
                            Text(banner.body, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { mainViewModel.setPromoBanner(null) }) { Text("✕") }
                    }
                }
            }
        }

        // FAB (Kidzo) — only on home screen (not in-game)
        if (!uiState.inGame && kidzoViewModel != null) {
            val infiniteTransition = rememberInfiniteTransition(label = "kidzo_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900),
                    repeatMode = RepeatMode.Reverse,
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
        } // end inner Box

        // Banner
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    val isTablet = ctx.resources.configuration.smallestScreenWidthDp >= 600
                    adsManager.loadBanner(this, isTablet, object : IAdsManager.BannerListener {
                        override fun onBannerLoaded(heightDp: Int) {
                            mainViewModel.setBannerLoaded(true)
                        }
                        override fun onBannerFailed() {}
                    })
                }
            },
            update = { container ->
                val show = uiState.bannerVisible && uiState.bannerLoaded
                container.visibility = if (show) View.VISIBLE else View.GONE
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
    } // end Column

    // Time limit check — every 30 seconds
    LaunchedEffect(activeProfile) {
        while (true) {
            delay(30_000)
            val limit = activeProfile?.timeLimitMinutes ?: 0
            if (limit > 0 && statsManager.getTodayMinutes() >= limit) {
                mainViewModel.showLock()
            }
        }
    }

    // BackHandler
    BackHandler {
        if (uiState.isLocked) return@BackHandler
        val mgr = webMgrRef.value
        webMgrRef.value?.evaluateJavascript(
            """(function(){
                var lv=document.getElementById('lyrics-viewer');
                if(lv&&!lv.classList.contains('h')){closeLyrics();return 'overlay';}
                var ai=document.getElementById('ai-viewer');
                if(ai&&!ai.classList.contains('h')){closeAi();return 'overlay';}
                var gv=document.getElementById('gv');
                return(gv&&!gv.classList.contains('h'))?'game':'none';
            })()"""
        ) { result ->
            when (result?.trim('"')) {
                "overlay" -> { }
                "game" -> mainViewModel.showExitDialog(true)
                else -> if (mgr?.canGoBack() == true) { mgr.goBack() } else { mainViewModel.showExitDialog(false) }
            }
        }
    }

    // Exit dialog
    if (uiState.showExitDialog) {
        AlertDialog(
            onDismissRequest = { mainViewModel.dismissExitDialog() },
            title = {
                Text(
                    if (uiState.isExitFromGame) "O’yindan chiqish?" else "KidZone’dan chiqish?"
                )
            },
            text = {
                Text(
                    if (uiState.isExitFromGame) "Menyuga qaytmoqchimisiz?"
                    else "Haqiqatan ham chiqib ketmoqchimisiz?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    mainViewModel.dismissExitDialog()
                    if (uiState.isExitFromGame) {
                        webMgrRef.value?.evaluateJavascript(
                            "(document.getElementById('gv-back')||{click:function(){}}).click();"
                        )
                    } else {
                        (context as? Activity)?.finishAffinity()
                    }
                }) {
                    Text(if (uiState.isExitFromGame) "Ha, menyuga" else "Ha, chiqish")
                }
            },
            dismissButton = {
                TextButton(onClick = { mainViewModel.dismissExitDialog() }) { Text("Yoʼq, qolish") }
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

// AdMob JavaScript bridge — standalone class (composable functions cannot have inner classes)
private class AdMobBridge(
    private val viewModel: MainViewModel,
    private val adsManager: AdsManager,
    private val onOpenDashboard: () -> Unit,
    activity: Activity,
) {
    private val activity = WeakReference(activity)

    private fun onMain(block: () -> Unit) {
        this.activity.get()?.runOnUiThread(block)
    }

    @android.webkit.JavascriptInterface
    fun showBanner() {
        onMain {
            viewModel.setInGame(false)
            viewModel.setBannerVisible(true)
        }
    }

    @android.webkit.JavascriptInterface
    fun hideBanner() {
        onMain {
            viewModel.setInGame(true)
            viewModel.setBannerVisible(false)
        }
    }

    @android.webkit.JavascriptInterface
    fun showInterstitial() {
        onMain {
            adsManager.showInterstitial()
        }
    }

    @android.webkit.JavascriptInterface
    fun showRewarded() {
        onMain {
            adsManager.showRewarded { amount ->
                // reward callback — webview JS call could go here if needed
            }
        }
    }

    @android.webkit.JavascriptInterface
    fun toggleMusic(mute: Boolean) {
        onMain {
            MusicManager.setMuted(mute)
        }
    }

    @android.webkit.JavascriptInterface
    fun updateLanguage(lang: String) {
        onMain {
            viewModel.setLanguage(lang)
        }
    }

    @android.webkit.JavascriptInterface
    fun openParentalDashboard() {
        onMain {
            onOpenDashboard()
        }
    }

    @android.webkit.JavascriptInterface
    fun gameLaunched(gameId: String) {
        onMain {
            viewModel.setInGame(true)
        }
    }
}

private class ChallengeBridge(
    private val viewModel: DailyChallengeViewModel,
    activity: Activity,
) {
    private val activity = WeakReference(activity)

    private fun onMain(block: () -> Unit) {
        activity.get()?.runOnUiThread(block)
    }

    @android.webkit.JavascriptInterface
    fun onGamesLoaded(json: String) {
        onMain { viewModel.updateGamesList(json) }
    }

    @android.webkit.JavascriptInterface
    fun onGameOpened(gameId: String) {
        // future use
    }

    @android.webkit.JavascriptInterface
    fun onGameClosed(gameId: String) {
        onMain { viewModel.onGameClosed(gameId) }
    }
}
