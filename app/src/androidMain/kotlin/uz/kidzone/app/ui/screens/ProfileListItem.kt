package uz.kidzone.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.kidzone.app.data.ProfileEntity

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
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfileInitialAvatar(name = profile.name, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${profile.language.uppercase()} | ${if (profile.timeLimitMinutes == 0) "Cheksiz" else "${profile.timeLimitMinutes} daq"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (isActive) Text("✓ Faol", style = MaterialTheme.typography.bodySmall)
                // Streak satri
                val milestoneCopy = celebrateMilestone?.let { MILESTONE_COPY[it] }
                if (milestoneCopy != null) {
                    Text(
                        "${milestoneCopy.first} ${milestoneCopy.second}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Text(
                            "🔥 $streakCount kun streak",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (doneToday) "Bugun: ✅" else "Bugun: ⏳",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            TextButton(onClick = onSwitch) { Text("Tanlash") }
            TextButton(onClick = onEdit) { Text("Tahrir") }
            TextButton(onClick = onDelete) { Text("O'chir") }
        }
    }
}
