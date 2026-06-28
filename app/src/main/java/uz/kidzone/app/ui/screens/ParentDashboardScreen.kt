package uz.kidzone.app.ui.screens

import android.content.SharedPreferences
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
import androidx.compose.material3.Card
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel,
    onNavigateToAddEdit: (String?) -> Unit,
) {
    val context = LocalContext.current
    val statsManager = remember { ParentalStatsManager(context) }
    val vm = remember { DashboardViewModel(statsManager, prefs) }
    val state by vm.state.collectAsState()

    // PIN gate
    var pinVerified by remember { mutableStateOf(false) }
    val activeProfile by profileViewModel.activeProfile.collectAsState()
    val savedPinHash = activeProfile?.pinHash
    val profiles by profileViewModel.profiles.collectAsState()

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

            // 3. Vaqt limiti
            item {
                Text("Vaqt limiti", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { vm.decreaseLimit() }) { Text("-") }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (state.timeLimitMinutes == 0) "Limit yo'q"
                        else "${state.timeLimitMinutes} daqiqa",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = { vm.increaseLimit() }) { Text("+") }
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
                    Spacer(Modifier.height(16.dp))
                }
            }

            // 7. Profillar bo'limi
            item {
                Spacer(Modifier.height(16.dp))
                Text("Profillar", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }

            items(profiles) { profile ->
                ProfileListItem(
                    profile = profile,
                    isActive = profile.id == activeProfile?.id,
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
internal fun PinGate(
    hasPinSet: Boolean,
    onPinCorrect: (String) -> Unit,
    onBack: () -> Unit,
) {
    var entered by remember { mutableStateOf("") }
    var lastAttemptFailed by remember { mutableStateOf(false) }

    if (!hasPinSet) {
        LaunchedEffect(Unit) { onPinCorrect("") }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("PIN kiriting", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (i < entered.length) "●" else "○",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (lastAttemptFailed) {
            Text(
                "PIN noto'g'ri ❌",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(16.dp))
        PinKeypad(
            onDigit = { d ->
                if (entered.length < 4) {
                    entered += d
                    lastAttemptFailed = false
                    if (entered.length == 4) {
                        onPinCorrect(entered)
                        entered = ""
                        lastAttemptFailed = true
                    }
                }
            },
            onBackspace = {
                if (entered.isNotEmpty()) {
                    entered = entered.dropLast(1)
                    lastAttemptFailed = false
                }
            },
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text("Bekor qilish") }
    }
}

@Composable
private fun PinKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "←"),
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Box(Modifier.size(64.dp))
                    } else {
                        FilledTonalButton(
                            onClick = { if (key == "←") onBackspace() else onDigit(key) },
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
private fun ChangePinDialog(
    prefs: SharedPreferences,
    currentHash: String?,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN o'zgartirish") },
        text = {
            Column {
                when (step) {
                    0 -> {
                        Text("Joriy PINni kiriting")
                        OutlinedTextField(
                            value = currentPin,
                            onValueChange = {
                                if (it.length <= 4 && it.all(Char::isDigit)) currentPin = it
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                        )
                    }
                    1 -> {
                        Text("Yangi PINni kiriting")
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = {
                                if (it.length <= 4 && it.all(Char::isDigit)) newPin = it
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                        )
                    }
                    2 -> {
                        Text("Yangi PINni tasdiqlang")
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = {
                                if (it.length <= 4 && it.all(Char::isDigit)) {
                                    confirmPin = it
                                    if (it.length == 4) {
                                        if (it == newPin) {
                                            prefs.edit()
                                                .putString("kz_pin", PinUtil.hash(newPin))
                                                .apply()
                                            onDismiss()
                                        } else {
                                            error = "PIN mos kelmadi"
                                            step = 1
                                            newPin = ""
                                            confirmPin = ""
                                        }
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                        )
                    }
                }
                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (step) {
                    0 -> {
                        if (currentHash.isNullOrEmpty() || PinUtil.matches(currentPin, currentHash)) {
                            step = 1
                            error = ""
                        } else {
                            error = "PIN noto'g'ri"
                        }
                    }
                    1 -> {
                        if (newPin.length == 4) {
                            step = 2
                            error = ""
                        }
                    }
                }
            }) { Text("Keyingi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Bekor") }
        },
    )
}

@Composable
private fun ProfileListItem(
    profile: ProfileEntity,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSwitch: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfileInitialAvatar(name = profile.name, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${profile.language.uppercase()} | ${if (profile.timeLimitMinutes == 0) "Cheksiz" else "${profile.timeLimitMinutes} daq"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (isActive) Text("✓ Faol", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onSwitch) { Text("Tanlash") }
            TextButton(onClick = onEdit) { Text("Tahrir") }
            TextButton(onClick = onDelete) { Text("O'chir") }
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
