package uz.kidzone.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.kidzone.app.kidzo.ContentCard
import uz.kidzone.app.ui.viewmodel.KidzoViewModel

private val KidZoneOrange = Color(0xFFFF6B35)
private val KidZoneYellow = Color(0xFFFDE68A)
private val KidZoneWarmBg = Color(0xFFFFF9F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidzoSheet(
    viewModel: KidzoViewModel,
    childName: String = "",
    lang: String = "uz",
    onContentSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val lastSpokenText by viewModel.lastSpokenText.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }

    // Audio permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput(context, lang, childName)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopVoiceInput()
            viewModel.stopSpeaking()
        }
    }

    // Dynamic translations
    val i18n = remember(lang) {
        when (lang.lowercase()) {
            "ru" -> mapOf(
                "title" to "Умный Кидзо 🐥",
                "subtitle" to "Твой верный друг и голосовой помощник",
                "idle" to "Нажми на микрофон или выбери тему! ✨",
                "listening" to "Слушаю тебя... Говори! 🎙️",
                "thinking" to "Думаю... 💡",
                "speaking" to "Говорю... 🔊",
                "stop" to "Остановить",
                "repeat" to "Повторить",
                "placeholder" to "Спроси что-нибудь...",
                "chips" to listOf(
                    "🧩 Загадай загадку" to "Загадай мне интересную детскую загадку",
                    "🌈 Почему небо синее?" to "Почему небо синее?",
                    "🦖 О динозаврах" to "Расскажи интересный факт о динозаврах",
                    "📖 Короткая сказка" to "Расскажи короткую добрую сказку",
                    "😄 Расскажи шутку" to "Расскажи детскую веселую шутку",
                    "🌟 Похвали меня" to "Скажи мне теплые слова и похвали меня"
                )
            )
            "en" -> mapOf(
                "title" to "Smart Kidzo 🐥",
                "subtitle" to "Your cheerful voice AI companion",
                "idle" to "Tap the mic or pick a topic! ✨",
                "listening" to "Listening to you... Speak! 🎙️",
                "thinking" to "Thinking... 💡",
                "speaking" to "Speaking... 🔊",
                "stop" to "Stop",
                "repeat" to "Replay",
                "placeholder" to "Ask anything...",
                "chips" to listOf(
                    "🧩 Tell a riddle" to "Tell me a fun riddle with an answer",
                    "🌈 Why is the sky blue?" to "Why is the sky blue?",
                    "🦖 About dinosaurs" to "Tell me a fun fact about dinosaurs",
                    "📖 Tell a mini story" to "Tell me a sweet short bedtime story",
                    "😄 Tell a joke" to "Tell me a funny kid joke",
                    "🌟 Compliment me" to "Give me a warm sweet compliment"
                )
            )
            else -> mapOf(
                "title" to "Aqlli Kidzo 🐥",
                "subtitle" to "Sening eng yaqin aqlli do'sting",
                "idle" to "Mikrofonni bosing yoki mavzu tanlang! ✨",
                "listening" to "Tinglayapman... Gapiring! 🎙️",
                "thinking" to "O'ylayapman... 💡",
                "speaking" to "Gapiryapman... 🔊",
                "stop" to "To'xtatish",
                "repeat" to "Qayta eshitish",
                "placeholder" to "Savol yozing...",
                "chips" to listOf(
                    "🧩 Qiziq topishmoq" to "Menga qiziqarli bolalar topishmog'ini ayt",
                    "🌈 Nega osmon ko'k?" to "Nega osmon ko'k rangda?",
                    "🦖 Dinozavrlar haqida" to "Dinozavrlar haqida qiziq ma'lumot ayt",
                    "📖 Kichik ertak ayt" to "Menga qisqa shirin ertak aytib ber",
                    "😄 Kulgili hazil" to "Bolalar uchun kulgili latifa yoki hazil ayt",
                    "🌟 Menga maqtov ayt" to "Menga yoqimli va dalda beruvchi maqtov ayt"
                )
            )
        }
    }

    // Animation transitions
    val infiniteTransition = rememberInfiniteTransition(label = "kidzo_anim")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = KidZoneWarmBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = i18n["title"] as String,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = KidZoneOrange,
                    )
                    Text(
                        text = i18n["subtitle"] as String,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.06f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── ANIMATED MASCOT HERO ──
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .padding(4.dp)
            ) {
                // Listening Wave Aura
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(100.dp * (pulseGlow + rmsLevel * 0.4f))
                            .clip(CircleShape)
                            .background(KidZoneOrange.copy(alpha = 0.25f))
                    )
                }
                // Speaking Wave Aura
                if (isSpeaking) {
                    Box(
                        modifier = Modifier
                            .size(95.dp * breatheScale)
                            .clip(CircleShape)
                            .background(KidZoneYellow.copy(alpha = 0.4f))
                    )
                }

                // Mascot Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(if (isSpeaking) breatheScale else 1f)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFFDE68A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            isListening -> "👂"
                            isSpeaking -> "🐥"
                            isThinking -> "🤔"
                            else -> "🐥"
                        },
                        fontSize = 42.sp
                    )
                }
            }

            // State Pill
            val statusText = when {
                isListening -> i18n["listening"] as String
                isThinking -> i18n["thinking"] as String
                isSpeaking -> i18n["speaking"] as String
                else -> i18n["idle"] as String
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when {
                            isListening -> KidZoneOrange.copy(alpha = 0.15f)
                            isSpeaking -> Color(0xFF10B981).copy(alpha = 0.15f)
                            isThinking -> KidZoneYellow.copy(alpha = 0.35f)
                            else -> Color.Black.copy(alpha = 0.05f)
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isListening -> KidZoneOrange
                        isSpeaking -> Color(0xFF059669)
                        isThinking -> Color(0xFFB45309)
                        else -> Color.Gray
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── RECENT SPEECH BUBBLE & CONTROLS ──
            val currentDisplayText = if (lastSpokenText.isNotBlank()) lastSpokenText else (messages.lastOrNull()?.second ?: "")
            if (currentDisplayText.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFFE8DE))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = currentDisplayText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B),
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSpeaking) {
                                Button(
                                    onClick = { viewModel.stopSpeaking() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("⏹️ " + (i18n["stop"] as String), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.repeatSpeech(context, lang) },
                                    colors = ButtonDefaults.buttonColors(containerColor = KidZoneOrange),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("🔄 " + (i18n["repeat"] as String), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── 1-TAP QUICK TOPIC CHIPS ──
            @Suppress("UNCHECKED_CAST")
            val chips = i18n["chips"] as List<Pair<String, String>>
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(chips) { (label, prompt) ->
                    Box(
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .clickable {
                                viewModel.askKidzo(prompt, lang, childName, context)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── BIG MICROPHONE BUTTON ──
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(if (isListening) pulseGlow else 1f)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        if (isListening) Brush.radialGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
                        else Brush.radialGradient(listOf(Color(0xFFFF8555), KidZoneOrange))
                    )
                    .clickable {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPerm) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.startVoiceInput(context, lang, childName)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isListening) "⏹️" else "🎙️",
                    fontSize = 32.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── TEXT INPUT OPTION FOR PARENTS ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(i18n["placeholder"] as String, fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KidZoneOrange,
                        cursorColor = KidZoneOrange,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.askKidzo(inputText, lang, childName, context)
                            inputText = ""
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KidZoneOrange),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text("→", fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── RECOMMENDATION CONTENT CARDS ──
            if (cards.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cards) { card: ContentCard ->
                        KidzoCardItem(card = card, onClick = {
                            onContentSelected(card.contentId)
                            onDismiss()
                        })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun KidzoCardItem(card: ContentCard, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(110.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
        border = BorderStroke(1.dp, KidZoneOrange.copy(alpha = 0.25f)),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(card.emoji, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = card.displayText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
