package uz.kidzone.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.kidzone.app.arch.AppPreferences

// Lockout after repeated wrong PINs: a 4-digit PIN only has 10k combinations, so the real
// defense isn't the hash — it's throttling guesses. Escalating cooldown (30s, 60s, 120s, ...)
// persisted in prefs so it survives leaving/reopening the dashboard or killing the app.
private const val KEY_PIN_FAIL_COUNT = "pin_fail_count"
private const val KEY_PIN_LOCK_UNTIL = "pin_lock_until_ms"
private const val MAX_ATTEMPTS_PER_TIER = 5
private const val BASE_LOCKOUT_SECONDS = 30
private const val MAX_LOCKOUT_SECONDS = 300

private fun AppPreferences.pinFailCount(): Int = getString(KEY_PIN_FAIL_COUNT, "0").toIntOrNull() ?: 0
private fun AppPreferences.setPinFailCount(v: Int) = putString(KEY_PIN_FAIL_COUNT, v.toString())
private fun AppPreferences.pinLockUntil(): Long = getString(KEY_PIN_LOCK_UNTIL, "0").toLongOrNull() ?: 0L
private fun AppPreferences.setPinLockUntil(v: Long) = putString(KEY_PIN_LOCK_UNTIL, v.toString())

@Composable
internal fun PinGate(
    hasPinSet: Boolean,
    onVerifyPin: (String) -> Boolean,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    var entered by remember { mutableStateOf("") }
    var lastAttemptFailed by remember { mutableStateOf(false) }

    if (!hasPinSet) {
        var a by remember { mutableIntStateOf((5..12).random()) }
        var b by remember { mutableIntStateOf((5..12).random()) }
        var answer by remember { mutableStateOf("") }
        var error by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Ota-ona qulfi", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Kattalar ekanligingizni tasdiqlash uchun masalani yeching:", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text("$a × $b = ?", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it.filter { char -> char.isDigit() }.take(3) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(120.dp),
                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 24.sp)
            )

            if (error) {
                Spacer(Modifier.height(8.dp))
                Text("Xato! Qaytadan urinib ko'ring.", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (answer.toIntOrNull() == a * b) {
                        onSuccess()
                    } else {
                        error = true
                        answer = ""
                        a = (5..12).random()
                        b = (5..12).random()
                    }
                },
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Tasdiqlash")
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBack) { Text("Bekor qilish") }
        }
        return
    }

    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var lockUntil by remember { mutableStateOf(prefs.pinLockUntil()) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val remainingLockSeconds = ((lockUntil - nowMs) / 1000L + 1).coerceAtLeast(0L)
    val isLocked = remainingLockSeconds > 0

    // Ticks nowMs once a second while locked so the keypad re-enables itself the moment the
    // cooldown expires, with no action needed from the user.
    LaunchedEffect(lockUntil) {
        while (System.currentTimeMillis() < lockUntil) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
        nowMs = System.currentTimeMillis()
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
        when {
            isLocked -> Text(
                "Juda ko'p xato urinish. $remainingLockSeconds soniyadan keyin qayta urinib ko'ring ⏳",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            lastAttemptFailed -> Text(
                "PIN noto'g'ri ❌",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(16.dp))
        PinKeypad(
            enabled = !isLocked,
            onDigit = { d ->
                if (!isLocked && entered.length < 4) {
                    entered += d
                    lastAttemptFailed = false
                    if (entered.length == 4) {
                        val attempt = entered
                        entered = ""
                        if (onVerifyPin(attempt)) {
                            prefs.setPinFailCount(0)
                            prefs.setPinLockUntil(0L)
                            onSuccess()
                        } else {
                            lastAttemptFailed = true
                            val failCount = prefs.pinFailCount() + 1
                            prefs.setPinFailCount(failCount)
                            if (failCount % MAX_ATTEMPTS_PER_TIER == 0) {
                                val tier = failCount / MAX_ATTEMPTS_PER_TIER
                                val lockoutSeconds = (BASE_LOCKOUT_SECONDS * (1 shl (tier - 1).coerceAtMost(4)))
                                    .coerceAtMost(MAX_LOCKOUT_SECONDS)
                                val until = System.currentTimeMillis() + lockoutSeconds * 1000L
                                prefs.setPinLockUntil(until)
                                lockUntil = until
                            }
                        }
                    }
                }
            },
            onBackspace = {
                if (!isLocked && entered.isNotEmpty()) {
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
private fun PinKeypad(enabled: Boolean = true, onDigit: (String) -> Unit, onBackspace: () -> Unit) {
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
                            enabled = enabled,
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
