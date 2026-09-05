package uz.kidzone.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.PlaytimeApiClient
import uz.kidzone.app.data.ProfileEntity

// How often we ask the backend for the server-authoritative remaining time. Must stay well
// under the backend's MAX_HEARTBEAT_GAP_SECONDS (45s) so normal play is measured accurately.
private const val HEARTBEAT_INTERVAL_SECONDS = 20

// Yaxshilangan: Taymerni alohida komponentga oldik, shu orqali butun ekran (MainScreen)
// har 1 soniyada qaytadan chizilmaydi (No useless Recomposition).
@Composable
fun PlayTimeCountdownBadge(
    activeProfile: ProfileEntity?,
    statsManager: ParentalStatsManager,
    modifier: Modifier = Modifier,
    onTimeUp: () -> Unit
) {
    var remainingSeconds by remember { mutableStateOf<Long?>(null) }

    // Server-authoritative remaining seconds, refreshed every HEARTBEAT_INTERVAL_SECONDS via
    // PlaytimeApiClient and ticked down locally in between so the badge stays smooth. When the
    // backend is unreachable (offline, cold-started dyno, etc.) this stays null and the loop
    // below falls back to the local ParentalStatsManager count, same as before this change.
    LaunchedEffect(activeProfile) {
        var previousRemaining: Long? = null
        var serverRemaining: Long? = null
        var ticksUntilHeartbeat = 0

        while (true) {
            val profile = activeProfile
            val limit = profile?.timeLimitMinutes ?: 0
            if (profile != null && limit > 0) {
                if (ticksUntilHeartbeat <= 0) {
                    ticksUntilHeartbeat = HEARTBEAT_INTERVAL_SECONDS
                    launch {
                        val result = PlaytimeApiClient.heartbeat(profile.id)
                        serverRemaining = if (result != null && !result.noLimit) result.remainingSeconds else null
                    }
                }
                ticksUntilHeartbeat--

                val left = serverRemaining?.also { serverRemaining = (it - 1).coerceAtLeast(0L) }
                    ?: (limit * 60L - statsManager.getTodaySeconds()).coerceAtLeast(0L)
                remainingSeconds = left
                if (left == 0L && previousRemaining != 0L) {
                    onTimeUp()
                }
                previousRemaining = left
            } else {
                remainingSeconds = null
                previousRemaining = null
                serverRemaining = null
                ticksUntilHeartbeat = 0
            }
            delay(1_000)
        }
    }

    val left = remainingSeconds
    if (left != null) {
        val lowTime = left <= 5 * 60L
        Surface(
            modifier = modifier.padding(top = 64.dp, end = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = if (lowTime) Color(0xFFBA1A1A) else Color.Black.copy(alpha = 0.55f),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("⏳ ", fontSize = 14.sp)
                Text(
                    text = "%d:%02d".format(left / 60, left % 60),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
