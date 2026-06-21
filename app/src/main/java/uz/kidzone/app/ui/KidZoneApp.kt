package uz.kidzone.app.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.kidzone.app.AdsManager
import uz.kidzone.app.ui.screens.OnboardingScreen
import uz.kidzone.app.ui.screens.ParentDashboardScreen
import uz.kidzone.app.ui.viewmodel.MainViewModel

@Composable
fun KidZoneApp(
    prefs: SharedPreferences,
    mainViewModel: MainViewModel,
    adsManager: AdsManager,
) {
    val navController = rememberNavController()
    val onboardingDone = prefs.getBoolean("kz_onboarding_done", false)

    NavHost(
        navController = navController,
        startDestination = if (onboardingDone) "main" else "onboarding",
    ) {
        composable("onboarding") {
            OnboardingScreen(
                prefs = prefs,
                onDone = {
                    prefs.edit().putBoolean("kz_onboarding_done", true).apply()
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(
                mainViewModel = mainViewModel,
                adsManager = adsManager,
                prefs = prefs,
                onOpenDashboard = { navController.navigate("dashboard") },
            )
        }
        composable("dashboard") {
            ParentDashboardScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
