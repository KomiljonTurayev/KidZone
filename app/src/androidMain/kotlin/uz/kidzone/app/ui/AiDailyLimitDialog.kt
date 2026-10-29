package uz.kidzone.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import uz.kidzone.app.PinUtil
import uz.kidzone.app.ui.components.Kid
import uz.kidzone.app.ui.components.KidButton
import uz.kidzone.app.ui.components.KidPage
import uz.kidzone.app.ui.components.KidSubtitle
import uz.kidzone.app.ui.components.KidTextButton
import uz.kidzone.app.ui.components.KidTitle
import uz.kidzone.app.ui.components.MascotBubble
import uz.kidzone.app.ui.i18n.tr
import uz.kidzone.app.ui.screens.PinGate

/** Which free AI allowance ran out today. */
enum class AiLimitKind { STORIES, QUESTIONS }

/**
 * Shown to the child when the family's free AI allowance for today is used up (Faza 17). Only
 * a parent, past the PIN, reaches the Premium screen. A dialog, so it also covers Kidzo's sheet.
 */
@Composable
fun AiDailyLimitDialog(
    kind: AiLimitKind,
    parentPinHash: String?,
    onOpenPremium: () -> Unit,
    onDismiss: () -> Unit,
) {
    var askingPin by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        if (askingPin) {
            PinGate(
                hasPinSet = !parentPinHash.isNullOrEmpty(),
                onVerifyPin = { pin -> parentPinHash.isNullOrEmpty() || PinUtil.matches(pin, parentPinHash) },
                onSuccess = {
                    onDismiss()
                    onOpenPremium()
                },
                onBack = { askingPin = false },
            )
        } else {
            KidPage {
                MascotBubble("🌙")
                Spacer(Modifier.height(20.dp))
                KidTitle(
                    when (kind) {
                        AiLimitKind.STORIES -> tr(
                            "Bugungi sehrli ertak tugadi!",
                            "Волшебная сказка на сегодня закончилась!",
                            "That's today's magic story!",
                        )
                        AiLimitKind.QUESTIONS -> tr(
                            "Kidzo bugun charchadi!",
                            "Kidzo сегодня устал!",
                            "Kidzo is tired for today!",
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
                KidSubtitle(tr(
                    "Ertaga yana keling. Ko'proq kerak bo'lsa, ota-onangizga ayting.",
                    "Приходи завтра. Если хочется больше — скажи родителям.",
                    "Come back tomorrow. Want more? Ask a parent.",
                ))
                Spacer(Modifier.height(24.dp))
                KidButton(tr("Ota-ona uchun 🔐", "Для родителей 🔐", "For parents 🔐"), brush = Kid.Royal) {
                    askingPin = true
                }
                Spacer(Modifier.height(8.dp))
                KidTextButton(tr("Yopish", "Закрыть", "Close"), onClick = onDismiss)
            }
        }
    }
}
