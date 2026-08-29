// app/src/main/java/uz/kidzone/app/ui/screens/ProfileSelectScreen.kt
package uz.kidzone.app.ui.screens

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uz.kidzone.app.data.ProfileEntity
import java.io.File
import kotlin.math.absoluteValue

@Composable
fun ProfileSelectScreen(
    profiles: List<ProfileEntity>,
    onSelect: (ProfileEntity) -> Unit,
    onAddNew: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F0)), // Playful warm background
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "👋 Kim o'ynaydi? 🎮",
                fontSize = 28.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                color = Color(0xFFFF6B35), // KidZone Orange
                modifier = Modifier.padding(bottom = 32.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(profiles, key = { it.id }) { profile ->
                    ProfileCard(profile = profile, onClick = { onSelect(profile) })
                }
            }
            Spacer(Modifier.height(32.dp))
            androidx.compose.material3.Button(
                onClick = onAddNew,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50), // Playful Green
                    contentColor = Color.White
                ),
                elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 0.dp),
                modifier = Modifier.height(56.dp).padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "➕ Yangi profil qo'shish",
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProfileCard(profile: ProfileEntity, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = Modifier.size(130.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp), // Super rounded
        color = Color.White,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFFDBC9)) // Thick playful border
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (profile.avatarPath != null && File(profile.avatarPath).exists()) {
                AsyncImage(
                    model = File(profile.avatarPath),
                    contentDescription = profile.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                )
            } else {
                ProfileInitialAvatar(name = profile.name, size = 64.dp)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = profile.name,
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color(0xFF2D2D2D),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ProfileInitialAvatar(name: String, size: Dp) {
    val avatarColors = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3),
        Color(0xFFFF9800), Color(0xFF9C27B0),
        Color(0xFFE91E63), Color(0xFF00BCD4),
    )
    val color = avatarColors[name.hashCode().absoluteValue % avatarColors.size]
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black
        )
    }
}
