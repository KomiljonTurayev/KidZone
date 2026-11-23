package uz.kidzone.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import uz.kidzone.app.BackendClient
import uz.kidzone.app.DeviceWipe
import uz.kidzone.app.PinUtil
import uz.kidzone.app.auth.AccountState
import uz.kidzone.app.auth.AuthGraph
import uz.kidzone.app.ui.components.Kid
import uz.kidzone.app.ui.components.KidButton
import uz.kidzone.app.ui.components.KidError
import uz.kidzone.app.ui.components.KidPage
import uz.kidzone.app.ui.components.KidSubtitle
import uz.kidzone.app.ui.components.KidTextButton
import uz.kidzone.app.ui.components.KidTitle
import uz.kidzone.app.ui.components.KidTopBar
import uz.kidzone.app.ui.components.MascotBubble
import uz.kidzone.app.ui.i18n.tr
import uz.kidzone.app.ui.viewmodel.DeleteAccountState
import uz.kidzone.app.ui.viewmodel.DeleteAccountViewModel

const val PRIVACY_POLICY_URL = "https://komiljonturayev.github.io/KidZone/kidzone-privacy.html"

/**
 * Privacy policy and data deletion, behind the parental gate. Open to everyone, also on a device
 * where the age screen was answered for a child and the dashboard is hidden.
 */
@Composable
fun PrivacyScreen(pinHash: String?, onBack: () -> Unit) {
    var gatePassed by remember { mutableStateOf(false) }
    if (!gatePassed) {
        PinGate(
            hasPinSet = !pinHash.isNullOrEmpty(),
            onVerifyPin = { pin -> pinHash.isNullOrEmpty() || PinUtil.matches(pin, pinHash) },
            onSuccess = { gatePassed = true },
            onBack = onBack,
        )
        return
    }

    val context = LocalContext.current
    val vm: DeleteAccountViewModel = viewModel(
        factory = viewModelFactory { initializer { DeleteAccountViewModel(AuthGraph.auth) } },
    )
    val state by vm.state.collectAsState()
    val account by AuthGraph.auth.state.collectAsState()
    var confirmAccount by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { BackendClient.warmUp() } // deletion needs it awake
    LaunchedEffect(state) {
        if (state == DeleteAccountState.DELETED) (context as? Activity)?.let(DeviceWipe::wipeAndRestart)
    }

    val title = tr("Maxfiylik va ma'lumotlar", "Конфиденциальность и данные", "Privacy and data")
    val signedIn = account as? AccountState.SignedIn
    if (confirmAccount && signedIn != null) {
        ConfirmAccount(
            account = signedIn,
            key = "delete_account_confirm",
            title = title,
            message = tr(
                "Hisobni faqat uning egasi o'chira oladi.",
                "Удалить аккаунт может только его владелец.",
                "Only the owner can delete the account.",
            ),
            onConfirmed = { confirmAccount = false; confirmDelete = true },
            onCancel = { confirmAccount = false },
        )
        return
    }

    val deleting = state == DeleteAccountState.DELETING
    KidPage(top = { KidTopBar(title, onBack = onBack) }) {
        MascotBubble("🔒", size = 96.dp)
        Spacer(Modifier.height(16.dp))
        KidTitle(title)
        Spacer(Modifier.height(20.dp))
        KidButton(tr("Maxfiylik siyosati", "Политика конфиденциальности", "Privacy Policy"), brush = Kid.Meadow) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) }
        }
        Spacer(Modifier.height(20.dp))
        if (state == DeleteAccountState.FAILED) {
            KidError(tr(
                "O'chirib bo'lmadi. Internetni tekshirib, qayta urinib ko'ring.",
                "Не удалось удалить. Проверьте интернет и попробуйте ещё раз.",
                "Couldn't delete. Check the internet and try again.",
            ))
            Spacer(Modifier.height(10.dp))
        }
        if (deleting) {
            KidSubtitle(tr("O'chirilmoqda… bir-ikki daqiqa ketishi mumkin.", "Удаляем… это может занять пару минут.", "Deleting… this can take a minute or two."))
        } else {
            KidTextButton(
                if (signedIn != null) tr("Hisobni butunlay o'chirish", "Удалить аккаунт навсегда", "Delete account permanently")
                else tr("Barcha ma'lumotlarimni o'chirish", "Удалить все мои данные", "Delete all my data"),
                color = MaterialTheme.colorScheme.error,
            ) { if (signedIn != null) confirmAccount = true else confirmDelete = true }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Kid.Card,
            icon = { Text("⚠️") },
            title = { Text(tr("Diqqat!", "Внимание!", "Warning!"), color = Kid.Ink) },
            text = {
                Text(
                    tr(
                        "Barcha ma'lumotlaringiz, bolalar profillari va statistika serverdan ham, shu qurilmadan ham butunlay o'chiriladi va ilova qayta ishga tushadi. Premium obuna Google Play'da alohida bekor qilinadi. Buni ortga qaytarib bo'lmaydi. Ishonchingiz komilmi?",
                        "Все ваши данные, профили детей и статистика будут удалены с сервера и с этого устройства, приложение перезапустится. Подписка Premium отменяется отдельно в Google Play. Это нельзя отменить. Вы уверены?",
                        "All your data, the children's profiles and stats will be deleted from the server and from this device, and the app will restart. Cancel a Premium subscription separately in Google Play. This can't be undone. Are you sure?",
                    ),
                    color = Kid.InkSoft,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete() }) {
                    Text(tr("O'chirish", "Удалить", "Delete"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(tr("Bekor qilish", "Отмена", "Cancel"), color = Kid.InkSoft) }
            },
        )
    }
}
