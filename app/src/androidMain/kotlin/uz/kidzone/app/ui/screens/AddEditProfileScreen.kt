package uz.kidzone.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import uz.kidzone.app.data.ProfileEntity
import uz.kidzone.app.ui.components.Kid
import uz.kidzone.app.ui.components.KidButton
import uz.kidzone.app.ui.components.KidChip
import uz.kidzone.app.ui.components.KidError
import uz.kidzone.app.ui.components.KidPage
import uz.kidzone.app.ui.components.KidSection
import uz.kidzone.app.ui.components.KidTextField
import uz.kidzone.app.ui.components.KidTopBar
import uz.kidzone.app.ui.i18n.tr
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun AddEditProfileScreen(
    profile: ProfileEntity?,
    parentPinHash: String? = null,
    onSave: (ProfileEntity) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val isNew = profile == null
    val profileId = remember { profile?.id ?: UUID.randomUUID().toString() }

    var name by remember { mutableStateOf(profile?.name ?: "") }
    var avatarPath by remember { mutableStateOf(profile?.avatarPath) }
    var language by remember { mutableStateOf(profile?.language ?: "uz") }
    var timeLimitSlider by remember { mutableFloatStateOf((profile?.timeLimitMinutes ?: 0).toFloat()) }
    var nameError by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarPath = saveAvatarFromUri(context, uri, profileId)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            avatarPath = saveBitmapAsAvatar(context, bitmap, profileId)
        }
    }

    KidPage(top = { KidTopBar(
        if (isNew) tr("Yangi profil", "Новый профиль", "New profile") else tr("Profilni tahrirlash", "Изменить профиль", "Edit profile"),
        onBack = onCancel,
    ) }) {
        // Avatar
        Box(
            modifier = Modifier
                .size(116.dp)
                .shadow(14.dp, CircleShape, ambientColor = Kid.Orange, spotColor = Kid.Orange)
                .clip(CircleShape)
                .background(Kid.Sunset)
                .clickable {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (avatarPath != null && File(avatarPath!!).exists()) {
                AsyncImage(
                    model = File(avatarPath!!),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = if (name.isNotEmpty()) name.first().uppercase() else "?",
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            KidChip(tr("🖼 Galereya", "🖼 Галерея", "🖼 Gallery"), selected = false, modifier = Modifier.weight(1f)) {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            KidChip(tr("📸 Kamera", "📸 Камера", "📸 Camera"), selected = false, modifier = Modifier.weight(1f)) { cameraLauncher.launch(null) }
        }
        Spacer(Modifier.height(20.dp))

        // Ism
        KidTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            placeholder = tr("Bola ismi", "Имя ребёнка", "Child's name"),
            isError = nameError,
        )
        if (nameError) {
            Spacer(Modifier.height(6.dp))
            KidError(tr("Ism bo'sh bo'lmasin", "Введите имя", "Please enter a name"))
        }
        Spacer(Modifier.height(16.dp))

        // Til
        KidSection("🌍", tr("Til", "Язык", "Language")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("uz" to "O'zbek", "ru" to "Русский", "en" to "English").forEach { (code, label) ->
                    KidChip(label, selected = language == code, modifier = Modifier.weight(1f)) { language = code }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Vaqt limiti
        val limitMinutes = timeLimitSlider.roundToInt()
        KidSection("⏰", tr("Vaqt limiti", "Лимит времени", "Time limit")) {
            Text(
                if (limitMinutes == 0) tr("Cheksiz", "Без лимита", "No limit") else "$limitMinutes " + tr("daqiqa", "мин", "min"),
                style = MaterialTheme.typography.titleLarge,
                color = Kid.Orange,
            )
            Slider(
                value = timeLimitSlider,
                onValueChange = { timeLimitSlider = it },
                valueRange = 0f..180f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = Kid.Orange,
                    activeTrackColor = Kid.Amber,
                    inactiveTrackColor = Kid.Line,
                    activeTickColor = Color.White,
                    inactiveTickColor = Kid.InkSoft.copy(alpha = 0.3f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))

        Spacer(Modifier.height(28.dp))

        KidButton(tr("✅ Saqlash", "✅ Сохранить", "✅ Save"), brush = Kid.Meadow) {
            if (name.isBlank()) { nameError = true; return@KidButton }
            val saved = (profile ?: ProfileEntity(
                id = profileId,
                name = "",
                avatarPath = null,
                language = "uz",
                timeLimitMinutes = 0,
                pinHash = null,
                isDefault = false,
                createdAt = System.currentTimeMillis(),
            )).copy(
                name = name.trim(),
                avatarPath = avatarPath,
                language = language,
                timeLimitMinutes = limitMinutes,
                // The PIN is set from the parent dashboard only, after confirming the account.
                pinHash = profile?.pinHash ?: parentPinHash,
            )
            onSave(saved)
        }
    }
}

private fun saveAvatarFromUri(context: Context, uri: Uri, profileId: String): String {
    val dir = File(context.filesDir, "profiles").apply { mkdirs() }
    val file = File(dir, "$profileId.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        val bmp = BitmapFactory.decodeStream(input) ?: return@use
        FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 80, out) }
    }
    return file.absolutePath
}

private fun saveBitmapAsAvatar(context: Context, bitmap: Bitmap, profileId: String): String {
    val dir = File(context.filesDir, "profiles").apply { mkdirs() }
    val file = File(dir, "$profileId.jpg")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out) }
    return file.absolutePath
}
