package uz.kidzone.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Palette shared by the parent-facing native screens; matches the WebView home (kids-theme.css). */
object Kid {
    val Ink = Color(0xFF2D2A4A)
    val InkSoft = Color(0xFF6B6785)
    val Cream = Color(0xFFFFF8F0)
    val Peach = Color(0xFFFFE3CF)
    val Orange = Color(0xFFFF6B35)
    val Amber = Color(0xFFFF9F1C)
    val Sky = Color(0xFF4FACFE)
    val Blue = Color(0xFF2B7FFF)
    val Mint = Color(0xFF2ECC71)
    val Grape = Color(0xFF7B5CFF)
    val Telegram = Color(0xFF229ED9)
    val Line = Color(0xFFEDE3DA)

    val Sunset = Brush.horizontalGradient(listOf(Color(0xFFFF8A3D), Color(0xFFFF5E62)))
    val Ocean = Brush.horizontalGradient(listOf(Sky, Blue))
    val Meadow = Brush.horizontalGradient(listOf(Color(0xFF43E97B), Color(0xFF20C38A)))
}

/** Warm gradient page with a few slow-floating soft bubbles behind the content. */
@Composable
fun KidBackground(content: @Composable BoxScope.() -> Unit) {
    val drift = rememberInfiniteTransition(label = "bubbles")
    val t by drift.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bubble_t",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF4E8), Kid.Cream, Color(0xFFFFEDE0)))),
    ) {
        Bubble(Color(0x33FF9F1C), 220.dp, x = (-60).dp, y = (60 + 20 * t).dp)
        Bubble(Color(0x2A4FACFE), 180.dp, x = 250.dp, y = (180 - 24 * t).dp)
        Bubble(Color(0x267B5CFF), 140.dp, x = (40 + 16 * t).dp, y = 620.dp)
        Bubble(Color(0x222ECC71), 200.dp, x = 260.dp, y = (700 + 18 * t).dp)
        content()
    }
}

@Composable
private fun Bubble(color: Color, size: Dp, x: Dp, y: Dp) {
    Box(Modifier.offset(x, y).size(size).clip(CircleShape).background(color))
}

/** Scrollable, centred page column with system-bar padding. */
@Composable
fun KidPage(
    top: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    KidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            if (top != null) Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) { top() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                content = content,
            )
        }
    }
}

/** Big emoji in a white halo that gently bobs up and down. */
@Composable
fun MascotBubble(emoji: String, size: Dp = 132.dp) {
    val bob = rememberInfiniteTransition(label = "mascot")
    val dy by bob.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "mascot_dy",
    )
    Box(
        modifier = Modifier
            .offset(y = dy.dp)
            .size(size)
            .shadow(18.dp, CircleShape, ambientColor = Kid.Orange, spotColor = Kid.Orange)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color.White, Color(0xFFFFF1E4)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value * 0.48f).sp)
    }
}

@Composable
fun KidTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineSmall,
        color = Kid.Ink,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun KidSubtitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = Kid.InkSoft,
        textAlign = TextAlign.Center,
    )
}

/** White rounded card with a soft warm shadow. */
@Composable
fun KidCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else if (selected) 1.03f else 1f, label = "card_scale")
    val borderColor by animateColorAsState(if (selected) Kid.Orange else Color.Transparent, label = "card_border")
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .scale(scale)
            .shadow(if (selected) 14.dp else 8.dp, shape, ambientColor = Color(0x22FF9F1C), spotColor = Color(0x33FF6B35))
            .clip(shape)
            .background(Color.White)
            .border(3.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier),
        content = content,
    )
}

/** Pill-shaped gradient call-to-action that squishes when pressed. */
@Composable
fun KidButton(
    label: String,
    modifier: Modifier = Modifier,
    brush: Brush = Kid.Sunset,
    enabled: Boolean = true,
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.95f else 1f, label = "btn_scale")
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(if (enabled) 12.dp else 0.dp, shape, ambientColor = Kid.Orange, spotColor = Kid.Orange)
            .clip(shape)
            .background(brush)
            .clickable(interaction, indication = null, enabled = enabled && !busy, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
        } else {
            Text(label, color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Quiet secondary action (back / skip). */
@Composable
fun KidTextButton(label: String, color: Color = Kid.InkSoft, onClick: () -> Unit) {
    Text(
        label,
        color = color,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** Round back chip + progress dots, used as the top bar of a multi-step flow. */
@Composable
fun StepHeader(current: Int, total: Int, onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Text("←", fontSize = 22.sp, color = Kid.Ink) }
            }
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(total) { i ->
                val active = i == current
                val width by animateDpAsState(if (active) 28.dp else 10.dp, label = "dot_w")
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(10.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(if (i <= current) Kid.Orange else Kid.Line),
                )
            }
        }
        Box(Modifier.size(44.dp))
    }
}
