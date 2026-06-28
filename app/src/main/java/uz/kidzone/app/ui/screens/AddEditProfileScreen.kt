// app/src/main/java/uz/kidzone/app/ui/screens/AddEditProfileScreen.kt
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uz.kidzone.app.PinUtil
import uz.kidzone.app.data.ProfileEntity
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProfileScreen(
    profile: ProfileEntity?,
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
    var pinInput by remember { mutableStateOf("") }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Yangi profil" else "Profilni tahrirlash") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Bekor") } },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Avatar
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3))
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
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Galereya") }
                OutlinedButton(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier.weight(1f),
                ) { Text("Kamera") }
            }

            // Ism
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Bola ismi") },
                isError = nameError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (nameError) {
                Text("Ism bo'sh bo'lmasin", color = Color.Red)
            }

            // Til
            Text("Til:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("uz" to "O'zbek", "ru" to "Русский", "en" to "English").forEach { (code, label) ->
                    FilterChip(
                        selected = language == code,
                        onClick = { language = code },
                        label = { Text(label) },
                    )
                }
            }

            // Vaqt limiti
            val limitMinutes = timeLimitSlider.roundToInt()
            Text("Vaqt limiti: ${if (limitMinutes == 0) "Cheksiz" else "$limitMinutes daqiqa"}")
            Slider(
                value = timeLimitSlider,
                onValueChange = { timeLimitSlider = it },
                valueRange = 0f..180f,
                steps = 11,
                modifier = Modifier.fillMaxWidth(),
            )

            // PIN (ixtiyoriy)
            OutlinedTextField(
                value = pinInput,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                label = { Text("PIN (ixtiyoriy, 4 xona)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    val pinHash = when {
                        pinInput.length == 4 -> PinUtil.hash(pinInput)
                        pinInput.isEmpty() -> profile?.pinHash
                        else -> { nameError = false; return@Button }
                    }
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
                        pinHash = pinHash,
                    )
                    onSave(saved)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Saqlash")
            }
        }
    }
}

private fun saveAvatarFromUri(context: Context, uri: Uri, profileId: String): String {
    val dir = File(context.filesDir, "profiles").apply { mkdirs() }
    val file = File(dir, "$profileId.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        val bmp = BitmapFactory.decodeStream(input)
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
