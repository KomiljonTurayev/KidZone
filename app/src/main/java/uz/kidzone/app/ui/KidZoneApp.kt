package uz.kidzone.app.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.kidzone.app.AdsManager
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.data.KidZoneDatabase
import uz.kidzone.app.data.ProfileRepository
import uz.kidzone.app.data.ProfileSyncManager
import uz.kidzone.app.ui.screens.OnboardingScreen
import uz.kidzone.app.ui.screens.ParentDashboardScreen
import uz.kidzone.app.ui.viewmodel.MainViewModel
import uz.kidzone.app.ui.viewmodel.ProfileViewModel
import uz.kidzone.app.ui.viewmodel.ProfileViewModelFactory

@Composable
fun KidZoneApp(
    prefs: SharedPreferences,
    mainViewModel: MainViewModel,
    adsManager: AdsManager,
    statsManager: ParentalStatsManager,
) {
    val navController = rememberNavController()
    val onboardingDone = prefs.getBoolean("kz_onboarding_done", false)
    val context = LocalContext.current

    val profileRepository = remember {
        val db = KidZoneDatabase.getInstance(context)
        val syncManager = ProfileSyncManager(
            runCatching { com.google.firebase.firestore.FirebaseFirestore.getInstance() }.getOrNull()
        )
        ProfileRepository(db.profileDao(), db.profileStatsDao(), prefs, syncManager)
    }
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(profileRepository)
    )

    NavHost(
        navController = navController,
        startDestination = if (onboardingDone) "main" else "onboarding",
    ) {
        composable("onboarding") {
            OnboardingScreen(
                prefs = prefs,
                onDone = {
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
                statsManager = statsManager,
                onOpenDashboard = { navController.navigate("dashboard") },
            )
        }
        composable("dashboard") {
            ParentDashboardScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() },
                profileViewModel = profileViewModel,
                onNavigateToAddEdit = { _ ->
                    // TODO(faza11-task11): navigate to profile_add_edit screen
                },
            )
        }
    }
}
