package uz.kidzone.app.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.kidzone.app.auth.AccountFeature
import uz.kidzone.app.auth.AuthProvider
import uz.kidzone.app.ui.components.Kid
import uz.kidzone.app.ui.components.KidCard
import uz.kidzone.app.ui.components.KidPage
import uz.kidzone.app.ui.components.KidSubtitle
import uz.kidzone.app.ui.components.KidTextButton
import uz.kidzone.app.ui.components.KidTitle
import uz.kidzone.app.ui.components.MascotBubble
import uz.kidzone.app.ui.theme.Nunito
import uz.kidzone.app.ui.viewmodel.LoginUiState
import uz.kidzone.app.ui.viewmodel.LoginViewModel
import uz.kidzone.app.ui.viewmodel.isBusy

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
 * [benefits] is an optional checklist of what an account gives; [header] sits above the page
 * (the onboarding progress bar). [providers] limits the buttons, e.g. to the signed-in parent's
 * own provider when re-confirming them.
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
    emoji: String = "🔐",
    benefits: List<String> = emptyList(),
    header: (@Composable () -> Unit)? = null,
    providers: Set<AuthProvider> = setOf(AuthProvider.GOOGLE, AuthProvider.TELEGRAM),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val t = loginTexts(lang)

    LaunchedEffect(state) {
        if (state == LoginUiState.SignedIn) onSignedIn()
    }

    KidPage(top = header) {
        MascotBubble(emoji, size = 112.dp)
        Spacer(Modifier.height(20.dp))
        KidTitle(title)
        Spacer(Modifier.height(6.dp))
        KidSubtitle(message)

        if (benefits.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            KidCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    benefits.forEach { b ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFE6F9EE)),
                                contentAlignment = Alignment.Center,
                            ) { Text("✓", color = Kid.Mint, fontSize = 16.sp, fontFamily = Nunito, fontWeight = FontWeight.Black) }
                            Spacer(Modifier.width(12.dp))
                            Text(b, style = MaterialTheme.typography.bodyLarge, color = Kid.Ink)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        if (AuthProvider.GOOGLE in providers) ProviderButton(
            label = t.google,
            background = Brush.horizontalGradient(listOf(Color.White, Color.White)),
            textColor = Kid.Ink,
            bordered = true,
            busy = state == LoginUiState.SigningInWithGoogle,
            enabled = !state.isBusy,
            icon = { GoogleMark() },
            onClick = { (context as? Activity)?.let(viewModel::signInWithGoogle) },
        )
        if (providers.size > 1) Spacer(Modifier.height(14.dp))
        if (AuthProvider.TELEGRAM in providers) ProviderButton(
            label = t.telegram,
            background = Brush.horizontalGradient(listOf(Color(0xFF2AABEE), Kid.Telegram)),
            textColor = Color.White,
            busy = state == LoginUiState.StartingTelegram,
            enabled = !state.isBusy,
            icon = { Text("✈️", fontSize = 20.sp) },
            onClick = { viewModel.signInWithTelegram { url -> context.openExternally(url) } },
        )

        when (val s = state) {
            is LoginUiState.WaitingForTelegram -> TelegramWaiting(
                t = t,
                onReopen = { context.openExternally(s.botUrl) },
                onCancel = viewModel::cancel,
            )
            is LoginUiState.Failed -> {
                Spacer(Modifier.height(16.dp))
                Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center)
            }
            else -> Unit
        }

        Spacer(Modifier.height(16.dp))
        KidTextButton(skipLabel) { viewModel.cancel(); onSkip() }
    }
}

@Composable
private fun ProviderButton(
    label: String,
    background: Brush,
    textColor: Color,
    busy: Boolean,
    enabled: Boolean,
    bordered: Boolean = false,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "provider_scale")
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(scale)
            .alpha(if (enabled || busy) 1f else 0.5f)
            .shadow(10.dp, shape, ambientColor = Color(0x44000000), spotColor = Color(0x44000000))
            .clip(shape)
            .background(background)
            .then(if (bordered) Modifier.border(1.5.dp, Kid.Line, shape) else Modifier)
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(22.dp), color = textColor, strokeWidth = 2.5.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.width(12.dp))
                Text(label, color = textColor, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** A blue "G", drawn as text so no brand asset needs to be bundled. */
@Composable
private fun GoogleMark() {
    Text("G", color = Color(0xFF4285F4), fontSize = 22.sp, fontFamily = Nunito, fontWeight = FontWeight.Black)
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
private fun TelegramWaiting(t: LoginTexts, onReopen: () -> Unit, onCancel: () -> Unit) {
    Spacer(Modifier.height(18.dp))
    KidCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Kid.Telegram)
                Spacer(Modifier.width(10.dp))
                Text(t.telegramWaiting, style = MaterialTheme.typography.bodyMedium, color = Kid.Ink)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                KidTextButton(t.reopenBot, color = Kid.Telegram, onClick = onReopen)
                KidTextButton(t.cancel, onClick = onCancel)
            }
        }
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
