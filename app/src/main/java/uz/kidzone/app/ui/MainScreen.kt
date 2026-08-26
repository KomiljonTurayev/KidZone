package uz.kidzone.app.ui

import android.app.Activity
import android.content.SharedPreferences
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import java.lang.ref.WeakReference
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.KidWebViewManager
import uz.kidzone.app.MusicManager
import uz.kidzone.app.kidzo.ContentFilter
import uz.kidzone.app.kidzo.KidzoAgent
import uz.kidzone.app.ui.screens.KidzoSheet
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModel
import uz.kidzone.app.ui.viewmodel.KidzoViewModel
import uz.kidzone.app.ui.viewmodel.MainViewModel
import uz.kidzone.app.ui.viewmodel.ProfileViewModel

// Matches the WebView games' --kt-accent (app/src/main/assets/www/kids-theme.css)
// so native dialogs feel consistent with the rest of the UI.
private val KidZoneOrange = Color(0xFFFF6B35)

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    prefs: SharedPreferences,
    statsManager: ParentalStatsManager,
    profileViewModel: ProfileViewModel,
    challengeViewModel: DailyChallengeViewModel,
    onOpenDashboard: () -> Unit,
) {
    val uiState by mainViewModel.state.collectAsState()
    val activeProfile by profileViewModel.activeProfile.collectAsState()

    // Profil o'zgarganda challengeViewModel ga xabar ber
    LaunchedEffect(activeProfile) {
        activeProfile?.id?.let { challengeViewModel.onProfileChanged(it) }
    }
    val context = LocalContext.current
    val webMgrRef = remember { mutableStateOf<KidWebViewManager?>(null) }

    // Push bildirishnomadagi "url" maydoni content-id sifatida keladi (masalan
    // "story-005"/"song-003") — main.js'dagi window.playContent xuddi shu formatni
    // kutadi (KidzoSheet'ning onContentSelected'i ham shu yo'lni ishlatadi).
    LaunchedEffect(uiState.pendingDeepLinkContentId) {
        val contentId = uiState.pendingDeepLinkContentId ?: return@LaunchedEffect
        webMgrRef.value?.evaluateJavascript("if(window.playContent)playContent('$contentId')")
        mainViewModel.setPendingDeepLinkContentId(null)
    }
    var showKidzoSheet by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf<Long?>(null) }

    val kidzoAgentRef = remember {
        val filter = try { ContentFilter.fromAssets(context) } catch (e: Exception) { null }
        filter?.let {
            KidzoAgent.createStatic(it) { block -> android.os.Handler(android.os.Looper.getMainLooper()).post(block) }
        }
    }
    val kidzoViewModel = remember(kidzoAgentRef) {
        kidzoAgentRef?.let { KidzoViewModel(it) }
    }

    // statusBarsPadding(): kartani tepadagi tizim swipe-gesture zonasidan chiqaradi, aks holda "O'ynash" tugmasi immersive rejimda bosilmaydi
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { containerSize = it },
        ) {
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
                        NativeBridge(mainViewModel, onOpenDashboard, context as Activity),
                        "AndroidBridge",
                    )
                    mgr.addInterface(
                        ChallengeBridge(challengeViewModel, context),
                        "AndroidChallenge",
                    )
                    // lang/age travel as URL query params, read synchronously by main.js
                    // as soon as it runs. A prior version pushed them via evaluateJavascript
                    // from onPageFinished, but that raced against main.js's own "load"
                    // listener (which reads localStorage to init the translator) and lost
                    // most of the time, so the app's language picker never actually applied
                    // inside games — it also wrote a flat "kz-lang" key that main.js's
                    // per-profile ProfileManager never reads past the very first app launch.
                    mgr.loadUrl(
                        "file:///android_asset/www/index.html?lang=${Uri.encode(lang)}&age=${Uri.encode(age)}"
                    )
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

        // Play-time countdown badge — visible only while a game is open, so it doesn't
        // fight with the WebView's own home-screen header for the same top corner.
        if (uiState.inGame && !uiState.isLocked) {
            val left = remainingSeconds
            if (left != null) {
                val lowTime = left <= 5 * 60L
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 64.dp, end = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = if (lowTime) Color(0xFFBA1A1A) else Color.Black.copy(alpha = 0.55f),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("⏳ ", fontSize = 14.sp)
                        Text(
                            text = "%d:%02d".format(left / 60, left % 60),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
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

        // FAB (Kidzo) — only on home screen (not in-game). Draggable like iOS's
        // AssistiveTouch bubble, since its default bottom-end spot can sit on top of
        // the WebView's own bottom nav bar on some screen sizes.
        //
        // Not a FloatingActionButton: its built-in clickable and an outer
        // pointerInput(detectDragGestures) fight over the same touch sequence (the
        // inner clickable claims the down event first), so neither tap nor drag work
        // reliably. A single pointerInput below manually disambiguates tap vs. drag
        // by touch-slop distance instead.
        if (!uiState.inGame && kidzoViewModel != null) {
            val density = LocalDensity.current
            val viewConfiguration = LocalViewConfiguration.current
            val fabSizePx = with(density) { 56.dp.toPx() }
            val edgePaddingPx = with(density) { 16.dp.toPx() }
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }
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
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .padding(16.dp)
                    .size(56.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    .pointerInput(containerSize) {
                        awaitEachGesture {
                            awaitFirstDown()
                            var dragged = false
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.first()
                                val delta = change.positionChange()
                                if (!dragged && delta.getDistance() > viewConfiguration.touchSlop) {
                                    dragged = true
                                }
                                if (dragged) {
                                    change.consume()
                                    val minX = -(containerSize.width - fabSizePx - edgePaddingPx).coerceAtLeast(0f)
                                    val minY = -(containerSize.height - fabSizePx - edgePaddingPx).coerceAtLeast(0f)
                                    offsetX = (offsetX + delta.x).coerceIn(minX, 0f)
                                    offsetY = (offsetY + delta.y).coerceIn(minY, 0f)
                                }
                            } while (event.changes.any { it.pressed })
                            if (!dragged) {
                                kidzoViewModel.requestRecommendations()
                                showKidzoSheet = true
                            }
                        }
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("🐥", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        } // end inner Box
    } // end Column

    // Time limit countdown — ticks every second so the in-game badge stays live.
    // Locking is edge-triggered (only the instant remaining time first hits zero),
    // not level-triggered, so a parent who unlocks via PIN after the limit is
    // reached isn't immediately slammed back into the lock screen on the next tick.
    LaunchedEffect(activeProfile) {
        var previousRemaining: Long? = null
        while (true) {
            val limit = activeProfile?.timeLimitMinutes ?: 0
            if (limit > 0) {
                val left = (limit * 60L - statsManager.getTodaySeconds()).coerceAtLeast(0L)
                remainingSeconds = left
                if (left == 0L && previousRemaining != 0L) {
                    mainViewModel.showLock()
                }
                previousRemaining = left
            } else {
                remainingSeconds = null
                previousRemaining = null
            }
            delay(1_000)
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
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            icon = { Text(if (uiState.isExitFromGame) "🎮" else "👋", fontSize = 40.sp) },
            title = {
                Text(
                    if (uiState.isExitFromGame) "O’yindan chiqish?" else "KidZone’dan chiqish?",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            text = {
                Text(
                    if (uiState.isExitFromGame) "Menyuga qaytmoqchimisiz?"
                    else "Haqiqatan ham chiqib ketmoqchimisiz?",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mainViewModel.dismissExitDialog()
                        if (uiState.isExitFromGame) {
                            webMgrRef.value?.evaluateJavascript(
                                "(document.getElementById('gv-back')||{click:function(){}}).click();"
                            )
                        } else {
                            (context as? Activity)?.finishAffinity()
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KidZoneOrange),
                ) {
                    Text(if (uiState.isExitFromGame) "Ha, menyuga" else "Ha, chiqish")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { mainViewModel.dismissExitDialog() },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KidZoneOrange),
                    border = BorderStroke(1.dp, KidZoneOrange),
                ) { Text("Yoʼq, qolish") }
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

// Native JavaScript bridge — standalone class (composable functions cannot have inner classes)
private class NativeBridge(
    private val viewModel: MainViewModel,
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
        }
    }

    @android.webkit.JavascriptInterface
    fun hideBanner() {
        onMain {
            viewModel.setInGame(true)
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
    fun gameLaunched(@Suppress("UNUSED_PARAMETER") gameId: String) {
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
    fun onGameOpened(@Suppress("UNUSED_PARAMETER") gameId: String) {
        // future use
    }

    @android.webkit.JavascriptInterface
    fun onGameClosed(gameId: String) {
        onMain { viewModel.onGameClosed(gameId) }
    }
}
