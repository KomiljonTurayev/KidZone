package uz.kidzone.app.ui.theme

import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Night mode (Faza 18): "system" follows the phone, "light"/"dark" are the parent's or
 * child's pick from the dashboard or the 🌙 button on the home screen. Kept in kz_theme.
 */
class ThemeController(val mode: String, val dark: Boolean, val setMode: (String) -> Unit)

val LocalThemeController = staticCompositionLocalOf { ThemeController("system", false) {} }

val THEME_MODES = setOf("system", "light", "dark")

@Composable
fun rememberThemeController(prefs: SharedPreferences): ThemeController {
    var mode by remember { mutableStateOf(prefs.getString("kz_theme", "system")?.takeIf { it in THEME_MODES } ?: "system") }
    val dark = when (mode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    return ThemeController(mode, dark) { m ->
        if (m in THEME_MODES) {
            prefs.edit().putString("kz_theme", m).apply()
            mode = m
        }
    }
}
