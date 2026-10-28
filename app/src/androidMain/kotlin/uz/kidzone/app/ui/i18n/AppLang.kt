package uz.kidzone.app.ui.i18n

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

private const val KEY_LANG = "kz_lang"

/**
 * The app language ("uz", "ru" or "en") for the native screens: the one the parent picked in
 * onboarding, also changed from the WebView header. Provided once in KidZoneApp.
 */
val LocalAppLang = compositionLocalOf { "uz" }

/** [KEY_LANG] as state, so screens follow a language change made while they are open. */
@Composable
fun rememberAppLang(prefs: SharedPreferences): State<String> {
    val lang = remember { mutableStateOf(prefs.getString(KEY_LANG, null) ?: "uz") }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == KEY_LANG) lang.value = p.getString(KEY_LANG, null) ?: "uz"
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return lang
}

/** Picks the text for [lang]; anything other than ru/en falls back to Uzbek. */
fun tr(lang: String, uz: String, ru: String, en: String): String = when (lang) {
    "ru" -> ru
    "en" -> en
    else -> uz
}

/** [tr] for the current [LocalAppLang]. */
@Composable
fun tr(uz: String, ru: String, en: String): String = tr(LocalAppLang.current, uz, ru, en)
