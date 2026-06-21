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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val statsManager = remember { ParentalStatsManager(context) }
    val vm = remember { DashboardViewModel(statsManager, prefs) }
    val state by vm.state.collectAsState()

    // PIN gate
    var pinVerified by remember { mutableStateOf(false) }
    val savedPinHash = remember { PinUtil.getOrMigrateHash(prefs, "kz_pin") }

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
private fun PinGate(
    hasPinSet: Boolean,
    onPinCorrect: (String) -> Unit,
    onBack: () -> Unit,
) {
    var entered by remember { mutableStateOf("") }

    if (!hasPinSet) {
        onPinCorrect("")
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
        Spacer(Modifier.height(24.dp))
        PinKeypad(
            onDigit = { d ->
                if (entered.length < 4) {
                    entered += d
                    if (entered.length == 4) onPinCorrect(entered)
                }
            },
            onBackspace = {
                if (entered.isNotEmpty()) entered = entered.dropLast(1)
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
