package uz.kidzone.app.ui

import android.app.Activity
import android.content.SharedPreferences
import android.view.ViewGroup
import android.webkit.WebView
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import uz.kidzone.app.AdsManager
import uz.kidzone.app.KidWebViewManager
import uz.kidzone.app.MusicManager
import uz.kidzone.app.kidzo.ContentFilter
import uz.kidzone.app.kidzo.KidzoAgent
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
        val filter = try { ContentFilter.fromAssets(context) } catch (e: Exception) { null }
        filter?.let {
            KidzoAgent.createStatic(it) { r -> (context as? Activity)?.runOnUiThread(r) }
        }
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
                    mgr.setup(
                        AdMobBridge(mainViewModel, adsManager, onOpenDashboard),
                        "AndroidAdMob",
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
) {
    @android.webkit.JavascriptInterface
    fun showBanner() {
        viewModel.setInGame(false)
        viewModel.setBannerVisible(true)
    }

    @android.webkit.JavascriptInterface
    fun hideBanner() {
        viewModel.setInGame(true)
        viewModel.setBannerVisible(false)
    }

    @android.webkit.JavascriptInterface
    fun showInterstitial() {
        adsManager.showInterstitial()
    }

    @android.webkit.JavascriptInterface
    fun showRewarded() {
        adsManager.showRewarded { amount ->
            // reward callback — webview JS call could go here if needed
        }
    }

    @android.webkit.JavascriptInterface
    fun toggleMusic(mute: Boolean) {
        MusicManager.setMuted(mute)
    }

    @android.webkit.JavascriptInterface
    fun updateLanguage(lang: String) {
        viewModel.setLanguage(lang)
    }

    @android.webkit.JavascriptInterface
    fun openParentalDashboard() {
        onOpenDashboard()
    }

    @android.webkit.JavascriptInterface
    fun gameLaunched(gameId: String) {
        viewModel.setInGame(true)
    }
}
