package uz.kidzone.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import uz.kidzone.app.PinUtil
import uz.kidzone.app.auth.AccountFeature
import uz.kidzone.app.auth.AuthGraph
import uz.kidzone.app.ui.viewmodel.LoginViewModel

/**
 * The path from a locked feature to a parent sign-in: first a parental gate (the profile PIN,
 * or a math question when no PIN is set), then [LoginScreen]. Skip the gate with
 * [parentVerified] when the caller already passed one (e.g. the parent dashboard).
 */
@Composable
fun AccountGate(
    feature: AccountFeature,
    pinHash: String?,
    parentVerified: Boolean = false,
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
) {
    var gatePassed by remember { mutableStateOf(parentVerified) }
    if (!gatePassed) {
        PinGate(
            hasPinSet = !pinHash.isNullOrEmpty(),
            onVerifyPin = { pin -> pinHash.isNullOrEmpty() || PinUtil.matches(pin, pinHash) },
            onSuccess = { gatePassed = true },
            onBack = onBack,
        )
        return
    }
    val loginViewModel: LoginViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LoginViewModel(AuthGraph.auth, AuthGraph.telegramLogin, AuthGraph.googleSignIn) }
        },
    )
    LoginScreen(
        viewModel = loginViewModel,
        feature = feature,
        onSignedIn = onSignedIn,
        onContinueAsGuest = onBack,
    )
}
