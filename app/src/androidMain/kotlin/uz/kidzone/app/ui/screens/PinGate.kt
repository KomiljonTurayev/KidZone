package uz.kidzone.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import uz.kidzone.app.arch.AppPreferences
import uz.kidzone.app.ui.components.DigitBoxes
import uz.kidzone.app.ui.components.Kid
import uz.kidzone.app.ui.components.KidButton
import uz.kidzone.app.ui.components.KidCard
import uz.kidzone.app.ui.components.KidError
import uz.kidzone.app.ui.components.KidNumberPad
import uz.kidzone.app.ui.components.KidPage
import uz.kidzone.app.ui.components.KidSubtitle
import uz.kidzone.app.ui.components.KidTextButton
import uz.kidzone.app.ui.components.KidTitle
import uz.kidzone.app.ui.components.MascotBubble

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

        KidPage {
            MascotBubble("🔐", size = 100.dp)
            Spacer(Modifier.height(18.dp))
            KidTitle("Ota-ona qulfi")
            Spacer(Modifier.height(6.dp))
            KidSubtitle("Kattalar ekanligingizni tasdiqlash uchun masalani yeching:")
            Spacer(Modifier.height(18.dp))
            KidCard {
                Text(
                    "$a × $b = ${answer.ifEmpty { "?" }}",
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (error) MaterialTheme.colorScheme.error else Kid.Ink,
                )
            }
            if (error) {
                Spacer(Modifier.height(10.dp))
                KidError("Xato! Qaytadan urinib ko'ring.")
            }
            Spacer(Modifier.height(18.dp))
            KidNumberPad(
                onDigit = { d -> if (answer.length < 3) { answer += d; error = false } },
                onDelete = { answer = answer.dropLast(1); error = false },
            )
            Spacer(Modifier.height(18.dp))
            KidButton("Tasdiqlash", enabled = answer.isNotEmpty()) {
                if (answer.toIntOrNull() == a * b) {
                    onSuccess()
                } else {
                    error = true
                    answer = ""
                    a = (5..12).random()
                    b = (5..12).random()
                }
            }
            Spacer(Modifier.height(8.dp))
            KidTextButton("Bekor qilish", onClick = onBack)
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

    KidPage {
        MascotBubble(if (isLocked) "⏳" else "🔐", size = 100.dp)
        Spacer(Modifier.height(18.dp))
        KidTitle("PIN kiriting")
        Spacer(Modifier.height(20.dp))
        DigitBoxes(entered, length = 4, error = lastAttemptFailed || isLocked, masked = true)
        when {
            isLocked -> {
                Spacer(Modifier.height(10.dp))
                KidError("Juda ko'p xato urinish. $remainingLockSeconds soniyadan keyin qayta urinib ko'ring ⏳")
            }
            lastAttemptFailed -> {
                Spacer(Modifier.height(10.dp))
                KidError("PIN noto'g'ri ❌")
            }
        }
        Spacer(Modifier.height(20.dp))
        KidNumberPad(
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
            onDelete = {
                if (!isLocked && entered.isNotEmpty()) {
                    entered = entered.dropLast(1)
                    lastAttemptFailed = false
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        KidTextButton("Bekor qilish", onClick = onBack)
    }
}
