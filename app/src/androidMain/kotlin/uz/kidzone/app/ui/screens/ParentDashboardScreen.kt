package uz.kidzone.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.PinUtil
import uz.kidzone.app.arch.AppPreferences
import uz.kidzone.app.auth.AccountFeature
import uz.kidzone.app.auth.AccountState
import uz.kidzone.app.auth.AuthGraph
import uz.kidzone.app.auth.AuthProvider
import uz.kidzone.app.auth.userMessage
import uz.kidzone.app.data.AppClock
import uz.kidzone.app.ui.components.Kid
import uz.kidzone.app.ui.components.KidButton
import uz.kidzone.app.ui.components.KidChip
import uz.kidzone.app.ui.components.KidPage
import uz.kidzone.app.ui.components.KidSection
import uz.kidzone.app.ui.components.KidTextButton
import uz.kidzone.app.ui.components.KidTopBar
import uz.kidzone.app.ui.i18n.LocalAppLang
import kotlinx.coroutines.flow.flowOf
import uz.kidzone.app.data.GameProgress
import uz.kidzone.app.data.GameProgressRepository
import uz.kidzone.app.ui.i18n.tr
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModel
import uz.kidzone.app.ui.viewmodel.DashboardViewModel
import uz.kidzone.app.ui.viewmodel.ProfileViewModel

@Composable
fun ParentDashboardScreen(
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel,
    challengeViewModel: DailyChallengeViewModel,
    gameProgress: GameProgressRepository,
    onNavigateToAddEdit: (String?) -> Unit,
    onOpenPremium: () -> Unit,
) {
    val context = LocalContext.current
    val statsManager = remember { ParentalStatsManager(context) }
    val vm = remember { DashboardViewModel(statsManager, AppPreferences(context), AuthGraph.auth) }
    val state by vm.state.collectAsState()

    // PIN gate
    var pinVerified by remember { mutableStateOf(false) }
    val activeProfile by profileViewModel.activeProfile.collectAsState()
    val savedPinHash = activeProfile?.pinHash
    val profiles by profileViewModel.profiles.collectAsState()
    val allStreaks by challengeViewModel.allStreaks.collectAsState()
    val challengeState by challengeViewModel.state.collectAsState()
    val levels by remember(activeProfile?.id) {
        activeProfile?.id?.let(gameProgress::progress) ?: flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    if (!pinVerified) {
        PinGate(
            hasPinSet = !savedPinHash.isNullOrEmpty(),
            onVerifyPin = { pin -> savedPinHash.isNullOrEmpty() || PinUtil.matches(pin, savedPinHash) },
            onSuccess = { pinVerified = true },
            onBack = onBack,
        )
        return
    }

    // The dashboard is an account-only feature. The PIN gate above already proved a parent is
    // holding the phone, so the sign-in screen is shown directly; state.account flips to
    // SignedIn on success and this recomposes into the dashboard.
    val account = state.account
    if (account !is AccountState.SignedIn) {
        AccountGate(
            feature = AccountFeature.PARENT_DASHBOARD,
            pinHash = savedPinHash,
            lang = LocalAppLang.current,
            parentVerified = true,
            onSignedIn = {},
            onBack = onBack,
        )
        return
    }

    var showSetPin by remember { mutableStateOf(false) }
    var showFamily by remember { mutableStateOf(false) }

    if (showFamily) {
        FamilyScreen(onBack = { showFamily = false })
        return
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showSetPin) {
        SetPinFlow(
            account = account,
            hasPin = !savedPinHash.isNullOrEmpty(),
            onSave = { hash -> profileViewModel.setParentPin(hash); showSetPin = false },
            onCancel = { showSetPin = false },
        )
        return
    }

    KidPage(top = { KidTopBar(tr("Ota-ona paneli", "Панель родителя", "Parent dashboard"), onBack = onBack) }) {
        // 1. Statistika
        KidSection("📊", tr("Bugun", "Сегодня", "Today")) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${state.todayMinutes}", style = MaterialTheme.typography.displaySmall, color = Kid.Orange)
                Spacer(Modifier.width(6.dp))
                Text(tr("daqiqa", "мин", "min"), style = MaterialTheme.typography.titleMedium, color = Kid.InkSoft,
                    modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(12.dp))
            WeeklyChart(state.weeklyMinutes)
        }
        Spacer(Modifier.height(16.dp))

        // 1b. Darajalar (Faza 18)
        KidSection("📈", tr("Darajalar", "Уровни", "Levels")) {
            LevelsCard(levels) { id -> challengeViewModel.gameLabel(id, LocalAppLang.current) ?: id }
        }
        Spacer(Modifier.height(16.dp))

        // 2. Yosh guruhi
        KidSection("🎂", tr("Yosh guruhi", "Возраст", "Age group")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("2-4", "5-7", "8+").forEach { age ->
                    KidChip(age, selected = state.age == age, modifier = Modifier.weight(1f)) { vm.setAge(age) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // 3. Vaqt limiti — activeProfile.timeLimitMinutes ustida ishlaydi, chunki
        // aynan shu maydonni MainScreen kunlik limitni tekshirish uchun o'qiydi.
        KidSection("⏰", tr("Vaqt limiti", "Лимит времени", "Time limit")) {
            val currentLimit = activeProfile?.timeLimitMinutes ?: 0
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RoundButton("−") {
                    activeProfile?.let { p ->
                        profileViewModel.updateProfile(p.copy(timeLimitMinutes = (p.timeLimitMinutes - 15).coerceAtLeast(0)))
                    }
                }
                Text(
                    if (currentLimit == 0) tr("Limit yo'q", "Без лимита", "No limit") else "$currentLimit " + tr("daqiqa", "мин", "min"),
                    style = MaterialTheme.typography.titleLarge,
                    color = Kid.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                RoundButton("+") {
                    activeProfile?.let { p ->
                        profileViewModel.updateProfile(p.copy(timeLimitMinutes = (p.timeLimitMinutes + 15).coerceAtMost(180)))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // 4. Xavfsizlik: PIN va push bildirishnomalar
        KidSection("🔑", tr("Sozlamalar", "Настройки", "Settings")) {
            SettingRow(if (savedPinHash.isNullOrEmpty()) tr("PIN o'rnatish", "Установить PIN", "Set PIN") else tr("PIN o'zgartirish", "Изменить PIN", "Change PIN"), onClick = { showSetPin = true }) {
                Text("›", fontSize = 26.sp, color = Kid.Orange)
            }
            SettingRow(tr("Oila", "Семья", "Family"), onClick = { showFamily = true }) {
                Text("›", fontSize = 26.sp, color = Kid.Orange)
            }
            SettingRow(tr("Bildirishnomalar", "Уведомления", "Notifications")) {
                Switch(
                    checked = state.pushEnabled,
                    onCheckedChange = { vm.setPushEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Kid.Mint,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Kid.Line,
                        uncheckedBorderColor = Kid.Line,
                    ),
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // 5. Hisob va bulutli sinxronlash
        KidSection("☁️", tr("Hisob", "Аккаунт", "Account")) {
            Text(
                "${account.displayName ?: tr("Ota-ona", "Родитель", "Parent")} · ${account.provider.label(LocalAppLang.current)}",
                style = MaterialTheme.typography.titleMedium,
                color = Kid.Ink,
            )
            state.accountError?.userMessage(LocalAppLang.current)?.let { error ->
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (!state.lastSyncTime.isNullOrEmpty()) {
                Text(
                    tr("Oxirgi sinxronlash: ", "Последняя синхронизация: ", "Last sync: ") + state.lastSyncTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = Kid.InkSoft,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KidChip(
                    if (state.isSyncing) tr("⏳ Sinxronlanmoqda", "⏳ Синхронизация", "⏳ Syncing") else tr("🔄 Sinxronlash", "🔄 Обновить", "🔄 Sync now"),
                    selected = false,
                    modifier = Modifier.weight(1f),
                ) { if (!state.isSyncing) vm.syncNow() }
                KidChip(tr("🚪 Chiqish", "🚪 Выйти", "🚪 Sign out"), selected = false, modifier = Modifier.weight(1f)) { vm.logout() }
            }
        }
        Spacer(Modifier.height(20.dp))
        KidButton(tr("⭐ Premium obuna", "⭐ Подписка Premium", "⭐ Premium"), brush = Kid.Royal, onClick = onOpenPremium)
        Spacer(Modifier.height(8.dp))

        // 6. Profillar bo'limi
        Spacer(Modifier.height(20.dp))
        Text(
            tr("👧 Profillar", "👧 Профили", "👧 Profiles"),
            style = MaterialTheme.typography.titleLarge,
            color = Kid.Ink,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))

        profiles.forEach { profile ->
            val streak = allStreaks.firstOrNull { it.profileId == profile.id }
            val todayDate = AppClock.today()
            val isActive = profile.id == activeProfile?.id
            ProfileListItem(
                profile = profile,
                isActive = isActive,
                streakCount = streak?.count ?: 0,
                doneToday = streak?.lastCompletedDate == todayDate,
                // celebrateMilestone tracks whichever profile last closed a game in
                // MainScreen (challengeViewModel's active profile) — only ever the
                // currently active profile, since only it can play games.
                celebrateMilestone = if (isActive) challengeState.celebrateMilestone else null,
                onCelebrationShown = challengeViewModel::onCelebrationShown,
                onEdit = { onNavigateToAddEdit(profile.id) },
                onDelete = {
                    if (profiles.size > 1) {
                        profileViewModel.deleteProfile(profile) {}
                    }
                },
                onSwitch = { profileViewModel.setActiveProfile(profile) },
            )
        }

        Spacer(Modifier.height(12.dp))
        KidButton(tr("➕ Yangi profil qo'shish", "➕ Добавить профиль", "➕ Add profile"), brush = Kid.Meadow, onClick = { onNavigateToAddEdit(null) })

        Spacer(Modifier.height(20.dp))
        KidTextButton(tr("Hisobni butunlay o'chirish", "Удалить аккаунт навсегда", "Delete account permanently"), color = MaterialTheme.colorScheme.error) { showDeleteDialog = true }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            icon = { Text("⚠️", fontSize = 36.sp) },
            title = { Text(tr("Diqqat!", "Внимание!", "Warning!"), color = Kid.Ink) },
            text = {
                Text(
                    tr(
                        "Barcha ma'lumotlaringiz, statistika va profillar serverdan butunlay o'chirib tashlanadi. Buni ortga qaytarib bo'lmaydi. Ishonchingiz komilmi?",
                        "Все ваши данные, статистика и профили будут безвозвратно удалены с сервера. Это нельзя отменить. Вы уверены?",
                        "All your data, stats and profiles will be permanently deleted from the server. This can't be undone. Are you sure?",
                    ),
                    color = Kid.InkSoft,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteAccount { showDeleteDialog = false }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(tr("O'chirish", "Удалить", "Delete"), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(tr("Bekor qilish", "Отмена", "Cancel"), color = Kid.InkSoft)
                }
            }
        )
    }
}

/** One labelled row inside a [KidSection]; the whole row is tappable when [onClick] is set. */
@Composable
private fun SettingRow(label: String, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Kid.Ink, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Best level per game, highest first; "+N" is what the child gained over the last 7 days. */
@Composable
private fun LevelsCard(levels: List<GameProgress>, label: @Composable (String) -> String) {
    if (levels.isEmpty()) {
        Text(
            tr(
                "Bola o'ynagan sari har bir o'yindagi darajasi shu yerda ko'rinadi.",
                "Здесь появятся уровни ребёнка в каждой игре.",
                "Your child's level in each game shows up here as they play.",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Kid.InkSoft,
        )
        return
    }
    val weekTotal = levels.sumOf { it.gainedThisWeek }
    if (weekTotal > 0) {
        Text(
            tr("Bu hafta: +$weekTotal daraja 🚀", "За неделю: +$weekTotal ур. 🚀", "This week: +$weekTotal levels 🚀"),
            style = MaterialTheme.typography.titleMedium,
            color = Kid.Mint,
        )
        Spacer(Modifier.height(8.dp))
    }
    levels.take(8).forEach { g ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Text(label(g.gameId), style = MaterialTheme.typography.bodyLarge, color = Kid.Ink, modifier = Modifier.weight(1f))
            if (g.gainedThisWeek > 0) {
                Text("+${g.gainedThisWeek}", style = MaterialTheme.typography.labelLarge, color = Kid.Mint)
                Spacer(Modifier.width(10.dp))
            }
            Text("⭐ ${g.level}", style = MaterialTheme.typography.titleMedium, color = Kid.Orange)
        }
    }
    if (levels.size > 8) {
        Text(
            tr("va yana ${levels.size - 8} ta o'yin", "и ещё ${levels.size - 8} игр", "and ${levels.size - 8} more games"),
            style = MaterialTheme.typography.bodySmall,
            color = Kid.InkSoft,
        )
    }
}

@Composable
private fun RoundButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .shadow(8.dp, CircleShape, ambientColor = Kid.Orange, spotColor = Kid.Orange)
            .clip(CircleShape)
            .background(Kid.Sunset)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 26.sp, style = MaterialTheme.typography.titleLarge) }
}

@Composable
private fun WeeklyChart(weeklyMinutes: List<Int>) {
    val maxMin = (weeklyMinutes.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
    val lastIndex = weeklyMinutes.lastIndex
    Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
        val slot = size.width / 7
        val barWidth = slot * 0.62f
        val radius = CornerRadius(barWidth / 2, barWidth / 2)
        weeklyMinutes.forEachIndexed { i, minutes ->
            val left = i * slot + (slot - barWidth) / 2
            // Empty track first, so days with no play still show a soft stub.
            drawRoundRect(color = Kid.Peach, topLeft = Offset(left, 0f), size = Size(barWidth, size.height), cornerRadius = radius)
            val barHeight = ((minutes / maxMin) * size.height).coerceAtLeast(if (minutes > 0) barWidth else 0f)
            if (barHeight > 0f) {
                drawRoundRect(
                    brush = if (i == lastIndex) Kid.Sunset else Brush.verticalGradient(listOf(Color(0xFFFFC75F), Kid.Amber)),
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius,
                )
            }
        }
    }
}

private fun AuthProvider.label(lang: String): String = when (this) {
    AuthProvider.GOOGLE -> "Google"
    AuthProvider.TELEGRAM -> "Telegram"
    AuthProvider.EMAIL -> "Email"
    AuthProvider.UNKNOWN -> tr(lang, "Hisob", "Аккаунт", "Account")
}
