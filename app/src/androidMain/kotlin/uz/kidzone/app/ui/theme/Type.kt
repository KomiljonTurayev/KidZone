package uz.kidzone.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uz.kidzone.app.R

// Nunito (OFL) is the body font of the WebView screens (kids-theme.css) and, unlike Fredoka,
// covers Cyrillic, so headings and body text look the same in uz/ru/en. Static weights cut
// from the variable font: Compose reused one typeface per resource and ignored the weight axis.
val Nunito = FontFamily(
    Font(R.font.nunito_400, FontWeight.Normal),
    Font(R.font.nunito_600, FontWeight.SemiBold),
    Font(R.font.nunito_700, FontWeight.Bold),
    Font(R.font.nunito_800, FontWeight.ExtraBold),
    Font(R.font.nunito_900, FontWeight.Black),
)

private val base = Typography()

private fun TextStyle.heading() = copy(fontFamily = Nunito, fontWeight = FontWeight.Black)

val KidZoneTypography = Typography(
    displayLarge = base.displayLarge.heading(),
    displayMedium = base.displayMedium.heading(),
    displaySmall = base.displaySmall.heading(),
    headlineLarge = base.headlineLarge.heading(),
    headlineMedium = base.headlineMedium.heading(),
    headlineSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = base.titleLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
    titleMedium = base.titleMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    bodyMedium = base.bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    bodySmall = base.bodySmall.copy(fontFamily = Nunito),
    labelLarge = base.labelLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
    labelMedium = base.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    labelSmall = base.labelSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
)
