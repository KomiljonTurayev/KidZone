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

/** Sign-in for an account-only [feature] a guest tried to open (see [AccountGate]). */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    feature: AccountFeature,
    onSignedIn: () -> Unit,
    onContinueAsGuest: () -> Unit,
    lang: String = "uz",
) {
    val t = loginTexts(lang)
    LoginScreen(
        viewModel = viewModel,
        title = feature.title,
        message = "${feature.pitch}. ${t.featureMessage}",
        skipLabel = t.continueAsGuest,
        onSignedIn = onSignedIn,
        onSkip = onContinueAsGuest,
        lang = lang,
    )
}

/**
 * Parent sign-in with Google or Telegram. Always reached through a parental gate (the
 * onboarding birth-year screen or [AccountGate]), so a child never sees it on their own.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    title: String,
    message: String,
    skipLabel: String,
    onSignedIn: () -> Unit,
    onSkip: () -> Unit,
    lang: String = "uz",
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val t = loginTexts(lang)

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
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
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
                label = t.google,
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
            ButtonContent(busy = state == LoginUiState.StartingTelegram, label = t.telegram)
        }

        when (val s = state) {
            is LoginUiState.WaitingForTelegram -> TelegramWaiting(
                t = t,
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
            onClick = { viewModel.cancel(); onSkip() },
            colors = ButtonDefaults.textButtonColors(contentColor = LoginAccent),
        ) {
            Text(skipLabel)
        }
    }
    }
}

internal class LoginTexts(
    val google: String,
    val telegram: String,
    val featureMessage: String,
    val continueAsGuest: String,
    val telegramWaiting: String,
    val reopenBot: String,
    val cancel: String,
)

internal fun loginTexts(lang: String): LoginTexts = when (lang) {
    "ru" -> LoginTexts(
        google = "Войти через Google",
        telegram = "Войти через Telegram",
        featureMessage = "Для этого войдите в аккаунт родителя.",
        continueAsGuest = "Продолжить как гость",
        telegramWaiting = "Откройте бота в Telegram и нажмите «Start»…",
        reopenBot = "Открыть бота снова",
        cancel = "Отмена",
    )
    "en" -> LoginTexts(
        google = "Continue with Google",
        telegram = "Continue with Telegram",
        featureMessage = "Sign in with a parent account to use it.",
        continueAsGuest = "Continue as guest",
        telegramWaiting = "Open the bot in Telegram and tap «Start»…",
        reopenBot = "Open the bot again",
        cancel = "Cancel",
    )
    else -> LoginTexts(
        google = "Google orqali kirish",
        telegram = "Telegram orqali kirish",
        featureMessage = "Bu imkoniyat uchun ota-ona hisobi bilan kiring.",
        continueAsGuest = "Mehmon sifatida davom etish",
        telegramWaiting = "Telegram'da botni oching va «Start» tugmasini bosing…",
        reopenBot = "Botni qayta ochish",
        cancel = "Bekor qilish",
    )
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
private fun TelegramWaiting(t: LoginTexts, onReopen: () -> Unit, onCancel: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(t.telegramWaiting, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onReopen) { Text(t.reopenBot) }
        TextButton(onClick = onCancel) { Text(t.cancel) }
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
