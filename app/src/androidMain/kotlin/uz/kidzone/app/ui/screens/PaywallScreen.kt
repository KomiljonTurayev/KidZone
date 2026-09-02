package uz.kidzone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.kidzone.app.RevenueCatManager

@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedPlan by remember { mutableStateOf("monthly") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("KidZone Premium 🌟", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B35))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Cheksiz Sun'iy Intellekt ertaklari va ovozli hikoyalarni oching!",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(24.dp))

                // Oylik Reja
                PlanCard(
                    title = "Oylik",
                    price = "$4.99 / oy",
                    isSelected = selectedPlan == "monthly",
                    onClick = { selectedPlan = "monthly" }
                )
                Spacer(Modifier.height(12.dp))
                // Yillik Reja
                PlanCard(
                    title = "Yillik (Tejamkor)",
                    price = "$39.99 / yil",
                    isSelected = selectedPlan == "annual",
                    onClick = { selectedPlan = "annual" }
                )

                Spacer(Modifier.height(24.dp))

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        if (selectedPlan == "monthly") {
                            RevenueCatManager.purchaseMonthly(
                                onSuccess = { onSuccess() },
                                onError = { errorMessage = it; isLoading = false }
                            )
                        } else {
                            RevenueCatManager.purchaseAnnual(
                                onSuccess = { onSuccess() },
                                onError = { errorMessage = it; isLoading = false }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Davom etish", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("Hozir emas", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFFFF6B35) else Color.LightGray
    val bgColor = if (isSelected) Color(0xFFFFF0E5) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(price, color = Color.Gray, fontSize = 14.sp)
            }
            if (isSelected) {
                Text("✅", fontSize = 24.sp)
            }
        }
    }
}
