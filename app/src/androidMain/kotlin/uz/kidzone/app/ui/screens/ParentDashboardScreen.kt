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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.PinUtil
import uz.kidzone.app.ui.viewmodel.DashboardViewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.style.TextAlign
import uz.kidzone.app.ui.viewmodel.DashboardState
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
) {
    val context = LocalContext.current
    val statsManager = remember { ParentalStatsManager(context) }
    val vm = remember { DashboardViewModel(statsManager, AppPreferences(context)) }
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

    // Cloud login gate (optional)
    var cloudResolved by remember { mutableStateOf(state.firebaseUid != null) }
    if (!cloudResolved) {
        CloudLoginSection(
            state = state,
            onLogin = { email, pass ->
                vm.login(email, pass) { success -> if (success) cloudResolved = true }
            },
            onRegister = { email, pass ->
                vm.register(email, pass) { success -> if (success) cloudResolved = true }
            },
            onSkip = { cloudResolved = true },
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

            // 6. Cloud status (faqat login bo'lsa)
            if (state.firebaseUid != null) {
                item {
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    Spacer(Modifier.height(8.dp))
                    Text("Cloud sinxronlash", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "☁️ Ulangan: ${state.firebaseEmail ?: state.firebaseUid}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
                        OutlinedButton(onClick = {
                            vm.logout()
                            cloudResolved = false
                        }) {
                            Text("Chiqish")
                        }
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
                                        vm.deleteAccount { success ->
                                            if (success) cloudResolved = false
                                            showDeleteDialog = false
                                        }
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

@Composable
private fun CloudLoginSection(
    state: DashboardState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onSkip: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("☁️ Cloud Sinxronlash", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Statistika va profil ma'lumotlarini bulutga saqlang",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Parol") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
        )

        if (!state.loginError.isNullOrEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                state.loginError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onLogin(email, password) },
                enabled = !state.isSyncing && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Kirish")
                }
            }
            OutlinedButton(
                onClick = { onRegister(email, password) },
                enabled = !state.isSyncing && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Ro'yxat")
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onSkip) {
            Text("O'tkazib yuborish →")
        }
    }
}
