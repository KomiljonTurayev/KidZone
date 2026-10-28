package uz.kidzone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.ui.components.Kid
import uz.kidzone.app.ui.components.KidButton
import uz.kidzone.app.ui.components.KidCard
import uz.kidzone.app.ui.components.KidPage
import uz.kidzone.app.ui.components.KidTitle
import uz.kidzone.app.ui.components.MascotBubble
import java.io.File
import kotlin.math.absoluteValue

@Composable
fun ProfileSelectScreen(
    profiles: List<ProfileEntity>,
    onSelect: (ProfileEntity) -> Unit,
    onAddNew: () -> Unit,
) {
    KidPage {
        MascotBubble("👋", size = 108.dp)
        Spacer(Modifier.height(20.dp))
        KidTitle("Kim o'ynaydi? 🎮")
        Spacer(Modifier.height(24.dp))
        // Plain rows rather than a lazy grid: the page scrolls as a whole and there are only a few profiles.
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            profiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { profile ->
                        ProfileCard(profile, Modifier.weight(1f)) { onSelect(profile) }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        KidButton("➕ Yangi profil qo'shish", brush = Kid.Meadow, onClick = onAddNew)
    }
}

@Composable
fun ProfileCard(profile: ProfileEntity, modifier: Modifier = Modifier, onClick: () -> Unit) {
    KidCard(modifier = modifier.height(156.dp), onClick = onClick) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProfileAvatar(profile, size = 76.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleLarge,
                color = Kid.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The profile's photo if it has one, otherwise its initial on a coloured circle. */
@Composable
fun ProfileAvatar(profile: ProfileEntity, size: Dp) {
    val path = profile.avatarPath
    if (path != null && File(path).exists()) {
        AsyncImage(
            model = File(path),
            contentDescription = profile.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    } else {
        ProfileInitialAvatar(name = profile.name, size = size)
    }
}

private val avatarGradients = listOf(
    listOf(Color(0xFFFF8A3D), Color(0xFFFF5E62)),
    listOf(Kid.Sky, Kid.Blue),
    listOf(Color(0xFF43E97B), Color(0xFF20C38A)),
    listOf(Color(0xFFA18CFF), Kid.Grape),
    listOf(Color(0xFFFFC75F), Kid.Amber),
    listOf(Color(0xFFFF7EB3), Color(0xFFE84393)),
)

@Composable
fun ProfileInitialAvatar(name: String, size: Dp) {
    val colors = avatarGradients[name.hashCode().absoluteValue % avatarGradients.size]
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}
