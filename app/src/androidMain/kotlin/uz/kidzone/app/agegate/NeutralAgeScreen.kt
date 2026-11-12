package uz.kidzone.app.agegate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.kidzone.app.ui.components.DigitBoxes
import uz.kidzone.app.ui.components.Kid
import uz.kidzone.app.ui.components.KidButton
import uz.kidzone.app.ui.components.KidChip
import uz.kidzone.app.ui.components.KidError
import uz.kidzone.app.ui.components.KidNumberPad
import uz.kidzone.app.ui.components.KidPage
import uz.kidzone.app.ui.components.KidSubtitle
import uz.kidzone.app.ui.components.KidTitle
import uz.kidzone.app.ui.components.MascotBubble
import java.time.LocalDate

/**
 * Google Play Families Policy — neutral age screen.
 *
 * - Nothing is preselected: no month, no year.
 * - The texts only ask for the date of birth; they never mention parents, an age limit or which
 *   answer unlocks what.
 * - The only check is age-independent plausibility (not in the future, not older than 120 years),
 *   so the error message gives no hint about the "right" answer.
 * - The answer is saved once by [AgeGate.submit]; the screen is never offered again.
 *
 * [top] is the optional onboarding progress header.
 */
@Composable
fun NeutralAgeScreen(
    lang: String,
    onSubmit: (birthYear: Int, birthMonth: Int) -> Unit,
    top: (@Composable () -> Unit)? = null,
    today: () -> LocalDate = LocalDate::now,
) {
    val t = neutralAgeTexts(lang)
    var month by rememberSaveable { mutableStateOf<Int?>(null) }
    var year by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    KidPage(top = top) {
        MascotBubble("🎈", size = 96.dp)
        Spacer(Modifier.height(16.dp))
        KidTitle(t.title)
        Spacer(Modifier.height(6.dp))
        KidSubtitle(t.subtitle)

        Spacer(Modifier.height(20.dp))
        Text(t.month, style = MaterialTheme.typography.titleMedium, color = Kid.InkSoft)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            t.months.chunked(4).forEachIndexed { row, names ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    names.forEachIndexed { col, name ->
                        val m = row * 4 + col + 1
                        KidChip(label = name, selected = month == m, modifier = Modifier.weight(1f)) {
                            month = m
                            invalid = false
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(t.year, style = MaterialTheme.typography.titleMedium, color = Kid.InkSoft)
        Spacer(Modifier.height(10.dp))
        DigitBoxes(year, length = 4, error = invalid)
        if (invalid) {
            Spacer(Modifier.height(10.dp))
            KidError(t.checkDate)
        }
        Spacer(Modifier.height(16.dp))
        KidNumberPad(
            onDigit = { d -> if (year.length < 4) { year += d; invalid = false } },
            onDelete = { year = year.dropLast(1); invalid = false },
        )
        Spacer(Modifier.height(20.dp))
        KidButton(
            t.next,
            enabled = month != null && year.length == 4 && !submitted,
            busy = submitted,
        ) {
            val m = month ?: return@KidButton
            val y = year.toIntOrNull()
            if (y == null || !isValidBirthDate(y, m, today())) {
                invalid = true
                return@KidButton
            }
            submitted = true
            onSubmit(y, m)
        }
    }
}

/** Age-independent sanity check: a real month/year, not in the future, at most 120 years ago. */
internal fun isValidBirthDate(year: Int, month: Int, today: LocalDate): Boolean {
    if (month !in 1..12 || !AgeGateRepository.isPlausibleYear(year, today)) return false
    return !(year == today.year && month > today.monthValue)
}

internal data class NeutralAgeTexts(
    val title: String,
    val subtitle: String,
    val month: String,
    val year: String,
    val months: List<String>,
    val checkDate: String,
    val next: String,
)

internal fun neutralAgeTexts(lang: String): NeutralAgeTexts = when (lang) {
    "ru" -> NeutralAgeTexts(
        title = "Дата рождения",
        subtitle = "Выберите месяц и год рождения.",
        month = "Месяц",
        year = "Год",
        months = listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"),
        checkDate = "Проверьте месяц и год",
        next = "Далее",
    )
    "en" -> NeutralAgeTexts(
        title = "Date of birth",
        subtitle = "Choose the month and year of birth.",
        month = "Month",
        year = "Year",
        months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
        checkDate = "Please check the month and year",
        next = "Continue",
    )
    else -> NeutralAgeTexts(
        title = "Tug'ilgan sana",
        subtitle = "Tug'ilgan oy va yilni tanlang.",
        month = "Oy",
        year = "Yil",
        months = listOf("Yan", "Fev", "Mar", "Apr", "May", "Iyun", "Iyul", "Avg", "Sen", "Okt", "Noy", "Dek"),
        checkDate = "Oy va yilni tekshiring",
        next = "Davom etish",
    )
}
