package uz.kidzone.app.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Stub for ParentDashboardScreen — full implementation in Task 10.
 */
@Composable
fun ParentDashboardScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit,
) {
    // TODO(Task 10): Replace with full Compose parental dashboard.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Parental Dashboard\n(Task 10)")
    }
}
