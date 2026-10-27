package uz.kidzone.app.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.kidzone.app.auth.AccountFeature
import uz.kidzone.app.ui.viewmodel.LoginUiState
import uz.kidzone.app.ui.viewmodel.LoginViewModel
import uz.kidzone.app.ui.viewmodel.isBusy

private val TelegramBlue = Color(0xFF229ED9)
private val LoginBackground = Color(0xFFFFF8F3)
private val LoginText = Color(0xFF2B2B2B)
private val LoginAccent = Color(0xFFE8551F)

/**
 * Parent sign-in, shown when a guest opens an account-only [feature]. Always reached through a
 * parental gate (see [AccountGate]), so a child never sees it on their own.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    feature: AccountFeature,
    onSignedIn: () -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state) {
        if (state == LoginUiState.SignedIn) onSignedIn()
    }

    // Own surface: the app theme's primary colour is the same orange as the window background,
    // which would make default-coloured buttons and links invisible here.
    Surface(modifier = Modifier.fillMaxSize(), color = LoginBackground, contentColor = LoginText) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🔐", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            feature.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${feature.pitch}. Bu imkoniyat uchun ota-ona hisobi bilan kiring.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        OutlinedButton(
            onClick = { (context as? Activity)?.let(viewModel::signInWithGoogle) },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = LoginText),
            border = BorderStroke(1.dp, Color(0xFFDADCE0)),
        ) {
            ButtonContent(
                busy = state == LoginUiState.SigningInWithGoogle,
                label = "Google orqali kirish",
                progressColor = LoginText,
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.signInWithTelegram { url -> context.openExternally(url) } },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue),
        ) {
            ButtonContent(busy = state == LoginUiState.StartingTelegram, label = "Telegram orqali kirish")
        }

        when (val s = state) {
            is LoginUiState.WaitingForTelegram -> TelegramWaiting(
                onReopen = { context.openExternally(s.botUrl) },
                onCancel = viewModel::cancel,
            )
            is LoginUiState.Failed -> {
                Spacer(Modifier.height(16.dp))
                Text(s.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            else -> Unit
        }

        Spacer(Modifier.height(24.dp))
        TextButton(
            onClick = { viewModel.cancel(); onContinueAsGuest() },
            colors = ButtonDefaults.textButtonColors(contentColor = LoginAccent),
        ) {
            Text("Mehmon sifatida davom etish")
        }
    }
    }
}

@Composable
private fun ButtonContent(busy: Boolean, label: String, progressColor: Color = Color.White) {
    if (busy) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = progressColor)
    } else {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TelegramWaiting(onReopen: () -> Unit, onCancel: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text("Telegram'da botni oching va «Start» tugmasini bosing…", style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onReopen) { Text("Botni qayta ochish") }
        TextButton(onClick = onCancel) { Text("Bekor qilish") }
    }
}

private fun Context.openExternally(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No Telegram app and no browser: the parent can still use "Botni qayta ochish" later.
    }
}
