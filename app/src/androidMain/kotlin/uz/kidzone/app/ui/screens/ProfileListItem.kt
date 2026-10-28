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

private val MILESTONE_COPY: Map<Int, Pair<String, String>> = mapOf(
    3 to ("🔥" to "3 kun ketma-ket! Ajoyib boshlanish!"),
    7 to ("🔥🔥" to "Bir hafta ketma-ket! Zo'r!"),
    14 to ("🔥🔥🔥" to "Ikki hafta ketma-ket!"),
    30 to ("🔥🔥🔥🔥" to "Bir oy ketma-ket! Sen chempion!"),
)

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
                                "✓ Faol",
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
                        "${profile.language.uppercase()} · ${if (profile.timeLimitMinutes == 0) "Cheksiz" else "${profile.timeLimitMinutes} daq"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Kid.InkSoft,
                    )
                }
            }
            Spacer(Modifier.padding(top = 10.dp))
            // Streak satri
            val milestoneCopy = celebrateMilestone?.let { MILESTONE_COPY[it] }
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
                    Text("🔥 $streakCount kun streak", style = MaterialTheme.typography.titleSmall, color = Kid.Ink)
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (doneToday) "Bugun: ✅" else "Bugun: ⏳",
                        style = MaterialTheme.typography.titleSmall,
                        color = Kid.InkSoft,
                    )
                }
            }
            Spacer(Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isActive) KidChip("Tanlash", selected = false, modifier = Modifier.weight(1f), onClick = onSwitch)
                KidChip("✏️ Tahrir", selected = false, modifier = Modifier.weight(1f), onClick = onEdit)
                KidChip("🗑 O'chir", selected = false, modifier = Modifier.weight(1f), onClick = onDelete)
            }
        }
    }
}
