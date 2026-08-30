package uz.kidzone.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.kidzone.app.kidzo.ContentCard
import uz.kidzone.app.ui.viewmodel.KidzoViewModel

// Matches the WebView games' --kt-accent (app/src/main/assets/www/kids-theme.css)
// so native dialogs feel consistent with the rest of the UI.
private val KidZoneOrange = Color(0xFFFF6B35)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidzoSheet(
    viewModel: KidzoViewModel,
    onContentSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
    ) {
        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()) {

            Text(
                "🐥 Kidzo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = KidZoneOrange,
            )
            Spacer(Modifier.height(12.dp))

            // Content cards
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cards) { card ->
                    KidzoCardItem(card = card, onClick = {
                        onContentSelected(card.contentId)
                        onDismiss()
                    })
                }
            }

            Spacer(Modifier.height(12.dp))

            // Chat messages (last 3)
            messages.takeLast(3).forEach { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Chat input
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Savol bering...") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KidZoneOrange,
                        cursorColor = KidZoneOrange,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                            // Oddiy counter o'rniga haqiqiy paywall counter qo'shiladi
                            val prefs = uz.kidzone.app.arch.AppPreferences.getInstance()
                            val limit = prefs.getInt("daily_ai_limit", 0)
                            prefs.putInt("daily_ai_limit", limit + 1)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KidZoneOrange),
                ) {
                    Text("→")
                }
            }
        }
        
        // PAYWALL LOGIC
        val prefs = uz.kidzone.app.arch.AppPreferences.getInstance()
        var dailyLimit by remember { mutableStateOf(prefs.getInt("daily_ai_limit", 0)) }
        var showPaywall by remember { mutableStateOf(false) }
        var showPinGate by remember { mutableStateOf(false) }
        
        // Agar limit 1 dan oshsa va Premium bo'lmasa, oynani yopish
        if (dailyLimit >= 1 && !uz.kidzone.app.RevenueCatManager.isPremium()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("Bugungi ertaklar tugadi! 🌙", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showPinGate = true },
                        colors = ButtonDefaults.buttonColors(containerColor = KidZoneOrange)
                    ) {
                        Text("🔒 Ota-onalar uchun")
                    }
                }
            }
            
            if (showPinGate) {
                uz.kidzone.app.ui.screens.PinGate(
                    hasPinSet = prefs.getString("kz_pin", "")?.isNotEmpty() == true,
                    onPinCorrect = { 
                        showPinGate = false
                        showPaywall = true
                    },
                    onBack = { showPinGate = false }
                )
            }
            
            if (showPaywall) {
                uz.kidzone.app.ui.screens.PaywallScreen(
                    onDismiss = { showPaywall = false },
                    onSuccess = { 
                        showPaywall = false
                        dailyLimit = 0
                    }
                )
            }
        }
    }
}

@Composable
private fun KidzoCardItem(card: ContentCard, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
        border = BorderStroke(1.dp, KidZoneOrange.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(card.emoji, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = card.displayText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
    }
}
