package uz.kidzone.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.kidzone.app.Family
import uz.kidzone.app.FamilyApiClient
import uz.kidzone.app.FamilyError
import uz.kidzone.app.FamilyInvite
import uz.kidzone.app.FamilyResult
import uz.kidzone.app.ui.components.DigitBoxes
import uz.kidzone.app.ui.components.Kid
import uz.kidzone.app.ui.components.KidButton
import uz.kidzone.app.ui.components.KidCard
import uz.kidzone.app.ui.components.KidChip
import uz.kidzone.app.ui.components.KidError
import uz.kidzone.app.ui.components.KidNumberPad
import uz.kidzone.app.ui.components.KidPage
import uz.kidzone.app.ui.components.KidSection
import uz.kidzone.app.ui.components.KidSubtitle
import uz.kidzone.app.ui.components.KidTextButton
import uz.kidzone.app.ui.components.KidTopBar
import uz.kidzone.app.ui.components.MascotBubble
import uz.kidzone.app.ui.i18n.LocalAppLang
import uz.kidzone.app.ui.i18n.tr

private const val CODE_LENGTH = 6

/**
 * Faza 16 family section, reached from the PIN-gated parent dashboard. The owner invites with
 * a 6-digit code; a second parent enters it. After a join or leave the profile sync follows
 * `users/{uid}.familyOwner` by itself (ProfileCloudSyncListener).
 */
@Composable
internal fun FamilyScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var family by remember { mutableStateOf<Family?>(null) }
    var invite by remember { mutableStateOf<FamilyInvite?>(null) }
    var error by remember { mutableStateOf<FamilyError?>(null) }
    var busy by remember { mutableStateOf(true) }
    var joining by remember { mutableStateOf(false) }

    fun <T> run(call: suspend () -> FamilyResult<T>, onOk: (T) -> Unit) {
        busy = true
        error = null
        scope.launch {
            when (val r = call()) {
                is FamilyResult.Ok -> onOk(r.value)
                is FamilyResult.Failed -> error = r.error
            }
            busy = false
        }
    }
    fun reload() = run({ FamilyApiClient.family() }) { family = it }

    LaunchedEffect(Unit) { reload() }
    // The system back button returns to the dashboard (or from the code entry to here),
    // not out of the parent area.
    BackHandler { if (joining) { joining = false; error = null } else onBack() }

    if (joining) {
        JoinFamily(
            busy = busy,
            error = error,
            onSubmit = { code -> run({ FamilyApiClient.join(code) }) { family = it; joining = false } },
            onBack = { joining = false; error = null },
        )
        return
    }

    KidPage(top = { KidTopBar(tr("Oila", "Семья", "Family"), onBack = onBack) }) {
        MascotBubble("👨‍👩‍👧", size = 100.dp)
        Spacer(Modifier.height(16.dp))
        KidSubtitle(
            tr(
                "Ikkinchi ota-ona o'z telefonidan bolalar profillari va PIN'ni boshqara oladi.",
                "Второй родитель сможет управлять профилями детей и PIN со своего телефона.",
                "A second parent can manage the children's profiles and PIN from their own phone.",
            ),
        )
        Spacer(Modifier.height(20.dp))
        val f = family
        when {
            busy && f == null -> CircularProgressIndicator(color = Kid.Orange)
            f == null -> {
                error?.let { KidError(familyErrorText(it)) }
                Spacer(Modifier.height(12.dp))
                KidButton(tr("Qayta urinish", "Повторить", "Try again"), onClick = ::reload)
            }
            !f.isOwner -> {
                KidSection("🏠", tr("Siz oiladasiz", "Вы в семье", "You're in a family")) {
                    Text(
                        tr(
                            "Profillar va PIN oila egasi bilan umumiy.",
                            "Профили и PIN общие с владельцем семьи.",
                            "Profiles and the PIN are shared with the family owner.",
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Kid.InkSoft,
                    )
                }
                Spacer(Modifier.height(16.dp))
                error?.let { KidError(familyErrorText(it)); Spacer(Modifier.height(8.dp)) }
                KidTextButton(tr("Oiladan chiqish", "Выйти из семьи", "Leave family"), color = MaterialTheme.colorScheme.error) {
                    run({ FamilyApiClient.leave() }) { reload() }
                }
            }
            else -> {
                KidSection("👥", tr("Oila a'zolari", "Члены семьи", "Family members")) {
                    if (f.members.isEmpty()) {
                        Text(tr("Hozircha yo'q", "Пока никого", "No one yet"), style = MaterialTheme.typography.bodyLarge, color = Kid.InkSoft)
                    }
                    f.members.forEachIndexed { i, m ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tr("Ota-ona", "Родитель", "Parent") + " ${i + 2} · " + m.joinedAt.take(10),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Kid.Ink,
                                modifier = Modifier.weight(1f),
                            )
                            KidChip(tr("Chiqarish", "Удалить", "Remove"), selected = false) {
                                run({ FamilyApiClient.removeMember(m.uid) }) { reload() }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                invite?.let { inv ->
                    KidCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(16.dp))
                            Text(inv.code.chunked(3).joinToString(" "), fontSize = 40.sp, style = MaterialTheme.typography.displaySmall, color = Kid.Orange)
                            Text(
                                tr("24 soat amal qiladi, bir marta", "Действует 24 часа, один раз", "Valid for 24 hours, once"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Kid.InkSoft,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                error?.let { KidError(familyErrorText(it)); Spacer(Modifier.height(8.dp)) }
                KidButton(tr("➕ Ota-onani taklif qilish", "➕ Пригласить родителя", "➕ Invite a parent"), busy = busy, enabled = !busy) {
                    run({ FamilyApiClient.invite() }) { invite = it }
                }
                if (f.members.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    KidTextButton(tr("Menda kod bor — oilaga qo'shilish", "У меня есть код — вступить в семью", "I have a code — join a family")) {
                        error = null
                        joining = true
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinFamily(busy: Boolean, error: FamilyError?, onSubmit: (String) -> Unit, onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    KidPage(top = { KidTopBar(tr("Oilaga qo'shilish", "Вступить в семью", "Join a family"), onBack = onBack) }) {
        MascotBubble("🔑", size = 100.dp)
        Spacer(Modifier.height(16.dp))
        KidSubtitle(tr("Oila egasi bergan 6 xonali kodni kiriting", "Введите 6-значный код от владельца семьи", "Enter the 6-digit code from the family owner"))
        Spacer(Modifier.height(20.dp))
        DigitBoxes(code, length = CODE_LENGTH, error = error != null)
        error?.let { Spacer(Modifier.height(10.dp)); KidError(familyErrorText(it)) }
        Spacer(Modifier.height(20.dp))
        KidNumberPad(
            enabled = !busy,
            onDigit = { d -> if (code.length < CODE_LENGTH) code += d },
            onDelete = { code = code.dropLast(1) },
        )
        Spacer(Modifier.height(20.dp))
        KidButton(tr("Qo'shilish", "Вступить", "Join"), busy = busy, enabled = code.length == CODE_LENGTH && !busy) { onSubmit(code) }
    }
}

@Composable
private fun familyErrorText(error: FamilyError): String {
    val lang = LocalAppLang.current
    return when (error) {
        FamilyError.NOT_FOUND -> tr(lang, "Kod noto'g'ri yoki muddati o'tgan.", "Код неверный или устарел.", "The code is wrong or has expired.")
        FamilyError.FORBIDDEN -> tr(lang, "Buni qilib bo'lmaydi.", "Это действие недоступно.", "This isn't allowed.")
        FamilyError.TOO_MANY_ATTEMPTS -> tr(lang, "Juda ko'p urinish. Birozdan keyin qayta urinib ko'ring.", "Слишком много попыток. Попробуйте позже.", "Too many tries. Please try again later.")
        FamilyError.NETWORK -> tr(lang, "Internet aloqasini tekshirib, qayta urinib ko'ring.", "Проверьте подключение к интернету и попробуйте снова.", "Check your internet connection and try again.")
    }
}
