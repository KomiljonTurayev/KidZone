package uz.kidzone.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.kidzone.app.audio.SleepAudioSynthesizer
import uz.kidzone.app.audio.SleepSoundType

private data class SoundOption(
    val type: SleepSoundType,
    val icon: String,
    val nameUz: String,
    val nameRu: String,
    val nameEn: String,
) {
    fun getName(lang: String): String = when (lang) {
        "ru" -> nameRu
        "en" -> nameEn
        else -> nameUz
    }
}

private class SleepStrings(
    val back: String,
    val title: String,
    val calmNight: String,
    val startTitle: String,
    val playingTitle: String,
    val tapHint: String,
    val playBtn: String,
    val pause: String,
    val stopBtn: String,
    val soundsTitle: String,
    val timerTitle: String,
    val autoOff: String,
    val routineTitle: String,
    val routineTeeth: String,
    val routineToys: String,
    val routineMilk: String,
    val routineBed: String,
    val volumeTitle: String,
)

private val TRANSLATIONS = mapOf(
    "uz" to SleepStrings(
        back = "Orqaga",
        title = "Sehrli Uyqu",
        calmNight = "Tinch tun",
        startTitle = "Uyqu Taronasini Boshlash",
        playingTitle = "Yangramoqda",
        tapHint = "Istalgan tovushni bosing — darhol yangraydi!",
        playBtn = "Ovozni Yoqish",
        pause = "Pauza",
        stopBtn = "To'xtatish",
        soundsTitle = "UYQU OVOZLARI",
        timerTitle = "Uyqu Taymeri",
        autoOff = "Avtomatik o'chadi",
        routineTitle = "UYQU ODOBI (TEKSHIRUV)",
        routineTeeth = "Tishlarni tozalash 🪥",
        routineToys = "O'yinchoqlarni yig'ish 🧸",
        routineMilk = "Iliq sut yoki suv ichish 🥛",
        routineBed = "O'ringa yotish 🛏️",
        volumeTitle = "Ovoz Balandligi",
    ),
    "ru" to SleepStrings(
        back = "Назад",
        title = "Спокойной Ночи",
        calmNight = "Спокойного сна",
        startTitle = "Включить Звуки для Сна",
        playingTitle = "Сейчас играет",
        tapHint = "Нажмите на любой звук — включится сразу!",
        playBtn = "Включить Звук",
        pause = "Пауза",
        stopBtn = "Остановить",
        soundsTitle = "ЗВУКИ ДЛЯ СНА",
        timerTitle = "Таймер Сна",
        autoOff = "Выключится сам",
        routineTitle = "ПЕРЕД СНОМ (ЧЕКЛИСТ)",
        routineTeeth = "Почистить зубки 🪥",
        routineToys = "Собрать игрушки 🧸",
        routineMilk = "Тёплое молоко или вода 🥛",
        routineBed = "Лечь в кроватку 🛏️",
        volumeTitle = "Громкость Звука",
    ),
    "en" to SleepStrings(
        back = "Back",
        title = "Magic Sleep",
        calmNight = "Sweet dreams",
        startTitle = "Start Sleep Melody",
        playingTitle = "Now playing",
        tapHint = "Tap any sound — it plays immediately!",
        playBtn = "Play Sound",
        pause = "Pause",
        stopBtn = "Stop",
        soundsTitle = "SLEEP SOUNDS",
        timerTitle = "Sleep Timer",
        autoOff = "Auto shuts off",
        routineTitle = "BEDTIME ROUTINE",
        routineTeeth = "Brush teeth 🪥",
        routineToys = "Put away toys 🧸",
        routineMilk = "Drink warm milk 🥛",
        routineBed = "Get into bed 🛏️",
        volumeTitle = "Sound Volume",
    ),
)

@Composable
fun SleepScreen(
    lang: String = "uz",
    onBack: () -> Unit,
) {
    val t = TRANSLATIONS[lang] ?: TRANSLATIONS["uz"]!!
    val synthesizer = remember { SleepAudioSynthesizer() }

    var isPlaying by remember { mutableStateOf(false) }
    var selectedSound by remember { mutableStateOf(SleepSoundType.LULLABY) }
    var timerMinutes by remember { mutableIntStateOf(30) }
    var secondsLeft by remember { mutableIntStateOf(30 * 60) }
    var volumeLevel by remember { mutableFloatStateOf(0.85f) }

    val routineDone = remember { mutableStateListOf(false, false, false, false) }

    val sounds = remember {
        listOf(
            SoundOption(SleepSoundType.LULLABY, "🎶", "Alla Kuyi", "Колыбельная", "Lullaby"),
            SoundOption(SleepSoundType.RAIN, "🌧️", "Yomg'ir", "Шум Дождя", "Rainfall"),
            SoundOption(SleepSoundType.WAVES, "🌊", "Dengiz", "Морской Прибой", "Ocean Waves"),
            SoundOption(SleepSoundType.FOREST, "🍃", "Tungi O'rmon", "Ночной Лес", "Night Forest"),
            SoundOption(SleepSoundType.WHITE_NOISE, "🤍", "Oq Shovqin", "Белый Шум", "White Noise"),
            SoundOption(SleepSoundType.TWINKLE, "✨", "Yulduzcha", "Звёздная Сказка", "Twinkle Star"),
        )
    }

    // Function to play sound immediately
    fun playSound(type: SleepSoundType) {
        selectedSound = type
        isPlaying = true
        synthesizer.setVolume(volumeLevel)
        synthesizer.play(type)
    }

    fun stopSound() {
        isPlaying = false
        synthesizer.stop()
    }

    // Timer countdown loop
    LaunchedEffect(isPlaying, timerMinutes) {
        if (isPlaying && timerMinutes > 0) {
            secondsLeft = timerMinutes * 60
            while (secondsLeft > 0 && isPlaying) {
                delay(1000)
                secondsLeft--
                if (secondsLeft <= 30) {
                    val fadeVol = (secondsLeft.toFloat() / 30f) * volumeLevel
                    synthesizer.setVolume(fadeVol)
                }
            }
            if (secondsLeft <= 0 && isPlaying) {
                stopSound()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            synthesizer.release()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "moon_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "moon_scale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617)),
                ),
            ),
    ) {
        // Starry Sky Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val starCount = 55
            for (i in 0 until starCount) {
                val x = (i * 97 % 1000) / 1000f * size.width
                val y = (i * 173 % 1000) / 1000f * size.height
                val radius = ((i % 3) + 1.2f).dp.toPx()
                val alpha = 0.25f + ((i % 5) * 0.15f)
                drawCircle(color = Color(0xFFFDE68A).copy(alpha = alpha), radius = radius, center = Offset(x, y))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.clickable { onBack() },
                ) {
                    Text(
                        text = "← ${t.back}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌙 ", fontSize = 18.sp)
                    Text(
                        text = t.title,
                        color = Color(0xFFFDE68A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                Text(
                    text = "💤 ${t.calmNight}",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Moon Orb Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(125.dp)
                    .scale(if (isPlaying) pulseScale else 1.0f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFF59E0B)),
                        ),
                    )
                    .border(4.dp, Color(0xFFFEF3C7).copy(alpha = 0.6f), CircleShape)
                    .clickable {
                        if (isPlaying) stopSound() else playSound(selectedSound)
                    },
            ) {
                Text(text = if (isPlaying) "✨" else "😴", fontSize = 56.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            val currentSoundObj = sounds.find { it.type == selectedSound }
            val currentName = currentSoundObj?.getName(lang) ?: ""

            Text(
                text = if (isPlaying) "${t.playingTitle}: $currentName 🎵" else t.startTitle,
                color = if (isPlaying) Color(0xFFFDE68A) else Color(0xFFE2E8F0),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
            )

            Text(
                text = if (timerMinutes > 0 && isPlaying) {
                    val mins = secondsLeft / 60
                    val secs = secondsLeft % 60
                    "⏳ %02d:%02d".format(mins, secs)
                } else {
                    t.tapHint
                },
                color = Color(0xFF94A3B8),
                fontSize = 12.5.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // EXPLICIT PLAY / PAUSE / STOP BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                Button(
                    onClick = {
                        if (isPlaying) stopSound() else playSound(selectedSound)
                    },
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) Color(0xFFE11D48) else Color(0xFFFF6B35),
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (isPlaying) "⏸️ ${t.pause}" else "▶️ ${t.playBtn}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.White,
                    )
                }

                Button(
                    onClick = { stopSound() },
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "⏹️ ${t.stopBtn}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFCBD5E1),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sound Options Grid
            Text(
                text = "🎵 ${t.soundsTitle}",
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0 until 3) {
                    val sound = sounds[i]
                    val isSelected = selectedSound == sound.type && isPlaying
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                // Tapping any card IMMEDIATELY plays that sound!
                                playSound(sound.type)
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF4338CA) else Color.White.copy(alpha = 0.07f),
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF818CF8)) else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(sound.icon, fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = sound.getName(lang),
                                color = if (isSelected) Color(0xFFFDE68A) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 3 until 6) {
                    val sound = sounds[i]
                    val isSelected = selectedSound == sound.type && isPlaying
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                // Tapping any card IMMEDIATELY plays that sound!
                                playSound(sound.type)
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF4338CA) else Color.White.copy(alpha = 0.07f),
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF818CF8)) else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(sound.icon, fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = sound.getName(lang),
                                color = if (isSelected) Color(0xFFFDE68A) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volume Control Slider
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🔉 ${t.volumeTitle}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${(volumeLevel * 100).toInt()}%", color = Color(0xFFFDE68A), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    Slider(
                        value = volumeLevel,
                        onValueChange = {
                            volumeLevel = it
                            synthesizer.setVolume(it)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFDE68A),
                            activeTrackColor = Color(0xFFFF6B35),
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timer Chips Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏳", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t.timerTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15 to "15m", 30 to "30m", 60 to "60m", 0 to "∞").forEach { (mins, label) ->
                            val isSel = timerMinutes == mins
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSel) Color(0xFF6366F1) else Color.White.copy(alpha = 0.08f),
                                border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5B4FC)) else null,
                                modifier = Modifier.clickable {
                                    timerMinutes = mins
                                    secondsLeft = mins * 60
                                },
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color.White else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Routine Checklist
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🧸 ${t.routineTitle}",
                        color = Color(0xFFFDE68A),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val routines = listOf(t.routineTeeth, t.routineToys, t.routineMilk, t.routineBed)
                    routines.forEachIndexed { index, task ->
                        val checked = routineDone[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (checked) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { routineDone[index] = !checked }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(text = task, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { routineDone[index] = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF10B981),
                                    checkmarkColor = Color.White,
                                    uncheckedColor = Color.White.copy(alpha = 0.4f),
                                ),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
