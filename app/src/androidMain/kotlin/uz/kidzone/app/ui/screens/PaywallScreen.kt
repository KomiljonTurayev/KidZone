package uz.kidzone.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.kidzone.app.RevenueCatManager
import uz.kidzone.app.data.ProfileOwner
import uz.kidzone.app.ui.components.Kid
import uz.kidzone.app.ui.components.KidButton
import uz.kidzone.app.ui.components.KidCard
import uz.kidzone.app.ui.components.KidError
import uz.kidzone.app.ui.components.KidPage
import uz.kidzone.app.ui.components.KidSubtitle
import uz.kidzone.app.ui.components.KidTextButton
import uz.kidzone.app.ui.components.KidTitle
import uz.kidzone.app.ui.components.MascotBubble
import uz.kidzone.app.ui.i18n.tr

@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedPlan by remember { mutableStateOf("monthly") }

    KidPage {
        MascotBubble("🌟", size = 120.dp)
        Spacer(Modifier.height(20.dp))
        KidTitle("KidZone Premium")
        Spacer(Modifier.height(6.dp))
        // Matches the backend's daily allowance (Faza 17): per family, Premium shared with members.
        KidSubtitle(tr(
            "Bepul: kuniga 1 ta AI ertak va Kidzo'ga 10 ta savol. Premium bilan — kuniga 30 ta ertak va 200 ta savol, butun oila uchun.",
            "Бесплатно: 1 ИИ-сказка и 10 вопросов Kidzo в день. С Premium — 30 сказок и 200 вопросов в день для всей семьи.",
            "Free: 1 AI story and 10 questions to Kidzo a day. With Premium: 30 stories and 200 questions a day for the whole family.",
        ))
        if (ProfileOwner.familyOwnerUid != null) {
            Spacer(Modifier.height(8.dp))
            KidSubtitle(tr(
                "Siz oila a'zosisiz: oila egasi Premium olgan bo'lsa, u sizda ham ishlaydi.",
                "Вы участник семьи: если у владельца семьи есть Premium, он работает и у вас.",
                "You're a family member: if the family owner has Premium, it works for you too.",
            ))
        }
        Spacer(Modifier.height(24.dp))

        PlanCard(
            title = tr("Oylik", "Ежемесячно", "Monthly"),
            price = "$4.99 / " + tr("oy", "мес", "month"),
            isSelected = selectedPlan == "monthly",
            onClick = { selectedPlan = "monthly" }
        )
        Spacer(Modifier.height(14.dp))
        PlanCard(
            title = tr("Yillik", "Ежегодно", "Yearly"),
            price = "$39.99 / " + tr("yil", "год", "year"),
            badge = tr("Tejamkor", "Выгодно", "Best value"),
            isSelected = selectedPlan == "annual",
            onClick = { selectedPlan = "annual" }
        )

        Spacer(Modifier.height(24.dp))
        errorMessage?.let {
            KidError(it)
            Spacer(Modifier.height(12.dp))
        }

        KidButton(tr("Davom etish", "Продолжить", "Continue"), busy = isLoading, enabled = !isLoading) {
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
        }
        Spacer(Modifier.height(8.dp))
        KidTextButton(tr("Hozir emas", "Не сейчас", "Not now"), onClick = onDismiss)
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    KidCard(Modifier.fillMaxWidth(), selected = isSelected, onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = Kid.Ink)
                    if (badge != null) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            badge,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Kid.Mint)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Text(price, style = MaterialTheme.typography.bodyLarge, color = Kid.InkSoft)
            }
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Kid.Orange else Kid.Line),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) Text("✓", color = Color.White, fontSize = 16.sp, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
