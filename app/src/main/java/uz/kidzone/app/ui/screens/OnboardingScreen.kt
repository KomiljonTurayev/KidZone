package uz.kidzone.app.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Stub for OnboardingScreen — full implementation in Task 9.
 * Immediately calls onDone() so the app flows to the main screen.
 */
@Composable
fun OnboardingScreen(
    prefs: SharedPreferences,
    onDone: () -> Unit,
) {
    // TODO(Task 9): Replace with full Compose onboarding flow.
    // Immediately proceed: onboarding preference already set by KidZoneApp before navigating.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
    // Trigger navigation on first composition
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onDone()
    }
}
