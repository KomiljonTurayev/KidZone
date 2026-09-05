package uz.kidzone.app.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.kidzone.app.PinUtil

// Matches the WebView games' --kt-accent (app/src/main/assets/www/kids-theme.css)
// so native dialogs feel consistent with the rest of the UI.
private val KidZoneOrange = Color(0xFFFF6B35)

@Composable
internal fun ChangePinDialog(
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
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        icon = { Text("🔐", fontSize = 40.sp) },
        title = {
            Text(
                "PIN o'zgartirish",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
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
            Button(
                onClick = {
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
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KidZoneOrange),
            ) { Text("Keyingi") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = KidZoneOrange),
                border = BorderStroke(1.dp, KidZoneOrange),
            ) { Text("Bekor") }
        },
    )
}
