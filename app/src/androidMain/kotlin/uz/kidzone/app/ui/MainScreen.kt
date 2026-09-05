package uz.kidzone.app.ui

import android.app.Activity
import android.content.SharedPreferences
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import uz.kidzone.app.kidzo.ContentFilter
import uz.kidzone.app.kidzo.KidzoAgent
import uz.kidzone.app.ui.screens.KidzoSheet
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModel
import uz.kidzone.app.ui.viewmodel.KidzoViewModel
import uz.kidzone.app.ui.viewmodel.MainViewModel
import uz.kidzone.app.ui.viewmodel.ProfileViewModel

private val KidZoneOrange = Color(0xFFFF6B35)

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    prefs: SharedPreferences,
    statsManager: ParentalStatsManager,
    profileViewModel: ProfileViewModel,
    challengeViewModel: DailyChallengeViewModel,
    onOpenDashboard: () -> Unit,
    onOpenSleep: () -> Unit = {},
) {
    // Yaxshilangan: collectAsState o'rniga collectAsStateWithLifecycle ishlatildi. 
    // Bu orqafon (background) da batareya isrofgarchiligini oldini oladi.
    val uiState by mainViewModel.state.collectAsStateWithLifecycle()
    val activeProfile by profileViewModel.activeProfile.collectAsStateWithLifecycle()

    LaunchedEffect(activeProfile) {
        activeProfile?.id?.let { challengeViewModel.onProfileChanged(it) }
    }
    
    val context = LocalContext.current
    val webMgrRef = remember { mutableStateOf<KidWebViewManager?>(null) }

    LaunchedEffect(uiState.pendingDeepLinkContentId) {
        val contentId = uiState.pendingDeepLinkContentId ?: return@LaunchedEffect
        webMgrRef.value?.evaluateJavascript("if(window.playContent)playContent('$contentId')")
        mainViewModel.setPendingDeepLinkContentId(null)
    }
    
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

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
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
                        val nativeBridge = NativeBridge(
                            viewModel = mainViewModel,
                            onOpenDashboard = onOpenDashboard,
                            onOpenSleep = onOpenSleep,
                            onOpenKidzo = {
                                kidzoViewModel?.requestRecommendations()
                                showKidzoSheet = true
                            },
                            onAskKidzo = { query ->
                                val curLang = activeProfile?.language ?: prefs.getString("kz_lang", "uz") ?: "uz"
                                val cName = activeProfile?.name ?: ""
                                kidzoViewModel?.askKidzo(query, curLang, cName, context)
                                showKidzoSheet = true
                            },
                            evalJs = mgr::evaluateJavascript,
                            activity = context as Activity
                        )
                        mgr.setup(nativeBridge, "AndroidBridge")
                        mgr.addInterface(
                            ChallengeBridge(challengeViewModel, context),
                            "AndroidChallenge",
                        )
                        mgr.loadUrl(
                            "${KidWebViewManager.ASSET_BASE_URL}index.html?lang=${Uri.encode(lang)}&age=${Uri.encode(age)}"
                        )
                        webMgrRef.value = mgr
                        addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: android.view.View) {}
                            override fun onViewDetachedFromWindow(v: android.view.View) {
                                mgr.destroy()
                                nativeBridge.shutdownTts()
                            }
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
                        Button(
                            onClick = { showUnlockPin = true },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                            modifier = Modifier.height(56.dp).padding(horizontal = 24.dp)
                        ) {
                            Text("Ota-ona uchun 🔐", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

            // Play-time countdown badge
            if (uiState.inGame && !uiState.isLocked) {
                // Yaxshilangan: Recomposition muammosini yo'qotish uchun mustaqil komponentga ajratildi
                PlayTimeCountdownBadge(
                    activeProfile = activeProfile,
                    statsManager = statsManager,
                    modifier = Modifier.align(Alignment.TopEnd),
                    onTimeUp = { mainViewModel.showLock() }
                )
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

            // FAB (Kidzo)
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

    // BackHandler
    BackHandler {
        if (uiState.isLocked) return@BackHandler
        val mgr = webMgrRef.value
        webMgrRef.value?.evaluateJavascript(
            """(function(){
                var sv=document.getElementById('sleep-view');
                if(sv&&!sv.classList.contains('h')){closeSleep();return 'overlay';}
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
        val currentLang = activeProfile?.language ?: prefs.getString("kz_lang", "uz") ?: "uz"
        val childName = activeProfile?.name ?: ""
        KidzoSheet(
            viewModel = kidzoViewModel,
            childName = childName,
            lang = currentLang,
            onContentSelected = { contentId ->
                webMgrRef.value?.evaluateJavascript("if(window.playContent)playContent('$contentId')")
            },
            onDismiss = {
                kidzoViewModel.stopSpeaking()
                kidzoViewModel.stopVoiceInput()
                showKidzoSheet = false
            },
        )
    }
}

