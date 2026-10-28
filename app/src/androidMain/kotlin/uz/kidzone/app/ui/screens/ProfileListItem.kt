package uz.kidzone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.ui.components.Kid
import uz.kidzone.app.ui.components.KidCard
import uz.kidzone.app.ui.components.KidChip
import uz.kidzone.app.ui.i18n.LocalAppLang
import uz.kidzone.app.ui.i18n.tr

private fun milestoneCopy(days: Int, lang: String): Pair<String, String>? = when (days) {
    3 -> "🔥" to tr(lang, "3 kun ketma-ket! Ajoyib boshlanish!", "3 дня подряд! Отличное начало!", "3 days in a row! Great start!")
    7 -> "🔥🔥" to tr(lang, "Bir hafta ketma-ket! Zo'r!", "Неделя подряд! Здорово!", "A whole week in a row! Awesome!")
    14 -> "🔥🔥🔥" to tr(lang, "Ikki hafta ketma-ket!", "Две недели подряд!", "Two weeks in a row!")
    30 -> "🔥🔥🔥🔥" to tr(lang, "Bir oy ketma-ket! Sen chempion!", "Месяц подряд! Ты чемпион!", "A month in a row! You're a champion!")
    else -> null
}

@Composable
internal fun ProfileListItem(
    profile: ProfileEntity,
    isActive: Boolean,
    streakCount: Int,
    doneToday: Boolean,
    celebrateMilestone: Int?,
    onCelebrationShown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSwitch: () -> Unit,
) {
    LaunchedEffect(celebrateMilestone) {
        if (celebrateMilestone != null) {
            kotlinx.coroutines.delay(2500)
            onCelebrationShown()
        }
    }
    KidCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), selected = isActive) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(profile, size = 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Kid.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isActive) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                tr("✓ Faol", "✓ Активен", "✓ Active"),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Kid.Mint)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Text(
                        "${profile.language.uppercase()} · " +
                            if (profile.timeLimitMinutes == 0) tr("Cheksiz", "Без лимита", "No limit")
                            else "${profile.timeLimitMinutes} " + tr("daq", "мин", "min"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Kid.InkSoft,
                    )
                }
            }
            Spacer(Modifier.padding(top = 10.dp))
            // Streak satri
            val milestoneCopy = celebrateMilestone?.let { milestoneCopy(it, LocalAppLang.current) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Kid.Peach.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (milestoneCopy != null) {
                    Text(
                        "${milestoneCopy.first} ${milestoneCopy.second}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Kid.Orange,
                    )
                } else {
                    Text("🔥 " + tr("$streakCount kun ketma-ket", "$streakCount дн. подряд", "$streakCount-day streak"), style = MaterialTheme.typography.titleSmall, color = Kid.Ink)
                    Spacer(Modifier.weight(1f))
                    Text(
                        tr("Bugun: ", "Сегодня: ", "Today: ") + if (doneToday) "✅" else "⏳",
                        style = MaterialTheme.typography.titleSmall,
                        color = Kid.InkSoft,
                    )
                }
            }
            Spacer(Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isActive) KidChip(tr("Tanlash", "Выбрать", "Select"), selected = false, modifier = Modifier.weight(1f), onClick = onSwitch)
                KidChip(tr("✏️ Tahrir", "✏️ Изменить", "✏️ Edit"), selected = false, modifier = Modifier.weight(1f), onClick = onEdit)
                KidChip(tr("🗑 O'chir", "🗑 Удалить", "🗑 Delete"), selected = false, modifier = Modifier.weight(1f), onClick = onDelete)
            }
        }
    }
}
