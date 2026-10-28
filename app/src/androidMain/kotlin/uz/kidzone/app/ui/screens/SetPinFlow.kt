package uz.kidzone.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import uz.kidzone.app.PinUtil
import uz.kidzone.app.auth.AccountState
import uz.kidzone.app.auth.AuthGraph
import uz.kidzone.app.auth.AuthProvider
import uz.kidzone.app.ui.components.DigitBoxes
import uz.kidzone.app.ui.components.KidError
import uz.kidzone.app.ui.components.KidNumberPad
import uz.kidzone.app.ui.components.KidPage
import uz.kidzone.app.ui.components.KidSubtitle
import uz.kidzone.app.ui.components.KidTextButton
import uz.kidzone.app.ui.components.KidTitle
import uz.kidzone.app.ui.components.KidTopBar
import uz.kidzone.app.ui.components.MascotBubble
import uz.kidzone.app.ui.viewmodel.LoginViewModel

private const val PIN_LENGTH = 4

/**
 * Lets the parent choose any PIN. The old PIN is not asked for; instead the parent confirms with
 * the Google/Telegram account the app is signed into. So only the account owner can set it, a
 * child who saw the PIN can't change it, and a forgotten PIN is reset the same way.
 */
@Composable
internal fun SetPinFlow(
    account: AccountState.SignedIn,
    hasPin: Boolean,
    onSave: (pinHash: String) -> Unit,
    onCancel: () -> Unit,
) {
    val title = if (hasPin) "PIN o'zgartirish" else "PIN o'rnatish"
    var confirmed by remember { mutableStateOf(false) }

    if (!confirmed) {
        val confirmViewModel: LoginViewModel = viewModel(
            key = "set_pin_confirm",
            factory = viewModelFactory {
                initializer {
                    LoginViewModel(AuthGraph.auth, AuthGraph.telegramLogin, AuthGraph.googleSignIn, confirmOnly = true)
                }
            },
        )
        LoginScreen(
            viewModel = confirmViewModel,
            title = "Hisobingiz bilan tasdiqlang",
            message = "PIN'ni faqat ota-ona hisobi egasi o'rnata oladi.",
            skipLabel = "Bekor qilish",
            // Reset the (screen-scoped) view model, or reopening this flow would count as confirmed.
            onSignedIn = { confirmViewModel.cancel(); confirmed = true },
            onSkip = onCancel,
            emoji = "🛡️",
            header = { KidTopBar(title, onBack = onCancel) },
            providers = when (account.provider) {
                AuthProvider.GOOGLE, AuthProvider.TELEGRAM -> setOf(account.provider)
                else -> setOf(AuthProvider.GOOGLE, AuthProvider.TELEGRAM)
            },
        )
        return
    }

    var first by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    KidPage(top = { KidTopBar(title, onBack = onCancel) }) {
        MascotBubble(if (first == null) "🔑" else "✅", size = 100.dp)
        Spacer(Modifier.height(18.dp))
        KidTitle(if (first == null) "Yangi PIN'ni kiriting" else "PIN'ni qayta kiriting")
        Spacer(Modifier.height(6.dp))
        KidSubtitle("Istalgan 4 ta raqam. Uni bolangizga aytmang.")
        Spacer(Modifier.height(20.dp))
        DigitBoxes(entered, length = PIN_LENGTH, error = mismatch, masked = true)
        if (mismatch) {
            Spacer(Modifier.height(10.dp))
            KidError("PIN'lar mos kelmadi. Qaytadan kiriting.")
        }
        Spacer(Modifier.height(20.dp))
        KidNumberPad(
            onDigit = { d ->
                if (entered.length < PIN_LENGTH) {
                    entered += d
                    mismatch = false
                }
                if (entered.length == PIN_LENGTH) {
                    val pin = entered
                    entered = ""
                    when (first) {
                        null -> first = pin
                        pin -> onSave(PinUtil.hash(pin))
                        else -> { first = null; mismatch = true }
                    }
                }
            },
            onDelete = { entered = entered.dropLast(1); mismatch = false },
        )
        Spacer(Modifier.height(12.dp))
        KidTextButton("Bekor qilish", onClick = onCancel)
    }
}
