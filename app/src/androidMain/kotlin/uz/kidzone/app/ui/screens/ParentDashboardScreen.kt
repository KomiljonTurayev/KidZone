package uz.kidzone.app.ui.screens

import android.content.SharedPreferences
import uz.kidzone.app.arch.AppPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.PinUtil
import uz.kidzone.app.auth.AccountFeature
import uz.kidzone.app.auth.AccountState
import uz.kidzone.app.auth.AuthGraph
import uz.kidzone.app.auth.AuthProvider
import uz.kidzone.app.ui.viewmodel.DashboardViewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import uz.kidzone.app.data.AppClock
import uz.kidzone.app.ui.viewmodel.ProfileViewModel
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel,
    challengeViewModel: DailyChallengeViewModel,
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
            parentVerified = true,
            onSignedIn = {},
            onBack = onBack,
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ota-ona paneli") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.padding(padding),
        ) {

            // 1. Statistika
            item {
                Text(
                    "Bugun: ${state.todayMinutes} daqiqa",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                WeeklyChart(state.weeklyMinutes)
                Spacer(Modifier.height(16.dp))
            }

            // 2. Yosh guruhi
            item {
                Text("Yosh guruhi", style = MaterialTheme.typography.titleMedium)
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

            // 3. Vaqt limiti — activeProfile.timeLimitMinutes ustida ishlaydi, chunki
            // aynan shu maydonni MainScreen kunlik limitni tekshirish uchun o'qiydi.
            item {
                Text("Vaqt limiti", style = MaterialTheme.typography.titleMedium)
                val currentLimit = activeProfile?.timeLimitMinutes ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        activeProfile?.let { p ->
                            profileViewModel.updateProfile(p.copy(timeLimitMinutes = (p.timeLimitMinutes - 15).coerceAtLeast(0)))
                        }
                    }) { Text("-") }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (currentLimit == 0) "Limit yo'q" else "$currentLimit daqiqa",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = {
                        activeProfile?.let { p ->
                            profileViewModel.updateProfile(p.copy(timeLimitMinutes = (p.timeLimitMinutes + 15).coerceAtMost(180)))
                        }
                    }) { Text("+") }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 4. PIN o'zgartirish
            item {
                var showChangePinDialog by remember { mutableStateOf(false) }
                Button(onClick = { showChangePinDialog = true }) {
                    Text("PIN o'zgartirish")
                }
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
                Text("Push bildirishnomalar", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bildirishnomalar", modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.pushEnabled,
                        onCheckedChange = { vm.setPushEnabled(it) },
                    )
                }
            }

            // 6. Hisob va bulutli sinxronlash
            item {
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    Spacer(Modifier.height(8.dp))
                    Text("Hisob", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "☁️ ${account.displayName ?: "Ota-ona"} · ${account.provider.label()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!state.accountError.isNullOrEmpty()) {
                        Text(
                            state.accountError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (!state.lastSyncTime.isNullOrEmpty()) {
                        Text(
                            "Oxirgi sync: ${state.lastSyncTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.syncNow() },
                            enabled = !state.isSyncing,
                        ) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Sinxronlash")
                            }
                        }
                        OutlinedButton(onClick = { vm.logout() }) {
                            Text("Chiqish")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenPremium, modifier = Modifier.fillMaxWidth()) {
                        Text("⭐ Premium obuna")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hisobni butunlay o'chirish")
                    }
                    Spacer(Modifier.height(16.dp))

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Diqqat!") },
                            text = { Text("Barcha ma'lumotlaringiz, statistika va profillar serverdan butunlay o'chirib tashlanadi. Buni ortga qaytarib bo'lmaydi. Ishonchingiz komilmi?") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        vm.deleteAccount { showDeleteDialog = false }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text("O'chirish", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Bekor qilish")
                                }
                            }
                        )
                    }
            }

            // 7. Profillar bo'limi
            item {
                Spacer(Modifier.height(16.dp))
                Text("Profillar", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }

            items(profiles) { profile ->
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

            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onNavigateToAddEdit(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("+ Yangi profil qo'shish") }
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
                topLeft = Offset(i * barWidth + 4f, size.height - barHeight),
                size = Size(barWidth - 8f, barHeight),
            )
        }
    }
}

private fun AuthProvider.label(): String = when (this) {
    AuthProvider.GOOGLE -> "Google"
    AuthProvider.TELEGRAM -> "Telegram"
    AuthProvider.EMAIL -> "Email"
    AuthProvider.UNKNOWN -> "Hisob"
}
