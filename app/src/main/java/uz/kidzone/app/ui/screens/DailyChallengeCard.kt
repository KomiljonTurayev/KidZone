// app/src/main/java/uz/kidzone/app/ui/screens/DailyChallengeCard.kt
package uz.kidzone.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.kidzone.app.data.DailyChallengeEntity

@Composable
fun DailyChallengeCard(
    streakCount: Int,
    challenge: DailyChallengeEntity?,
    visible: Boolean,
    onPlay: (gameId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible && challenge != null) {
        val c = challenge ?: return@AnimatedVisibility
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            AnimatedContent(
                targetState = c.completed,
                transitionSpec = {
                    (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f))
                        .togetherWith(fadeOut(tween(200)))
                },
                label = "challenge_card",
            ) { completed ->
                if (completed) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "🔥 $streakCount kun streak",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "✅ Bajarildi!",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            "Ertaga yangi vazifa kutilmoqda 🌟",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🔥 $streakCount kun streak",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                "Bugungi vazifa: ${c.gameTitle}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { onPlay(c.gameId) }) {
                            Text("O'ynash →")
                        }
                    }
                }
            }
        }
    }
}
