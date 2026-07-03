package uz.kidzone.app.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.kidzone.app.AdsManager
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.data.DailyChallengeRepository
import uz.kidzone.app.data.KidZoneDatabase
import uz.kidzone.app.data.ProfileRepository
import uz.kidzone.app.data.ProfileSyncManager
import uz.kidzone.app.ui.screens.AddEditProfileScreen
import uz.kidzone.app.ui.screens.OnboardingScreen
import uz.kidzone.app.ui.screens.ParentDashboardScreen
import uz.kidzone.app.ui.screens.ProfileSelectScreen
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModel
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModelFactory
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

    val challengeRepository = remember {
        val db = KidZoneDatabase.getInstance(context)
        val firestoreSync = uz.kidzone.app.FirestoreSync.getInstance()
        uz.kidzone.app.data.DailyChallengeRepository(
            challengeDao = db.dailyChallengeDao(),
            streakDao = db.streakDao(),
            firestoreSync = firestoreSync,
        )
    }
    val challengeViewModel: DailyChallengeViewModel = viewModel(
        factory = DailyChallengeViewModelFactory(challengeRepository)
    )

    val profiles by profileViewModel.profiles.collectAsState()

    val startDestination = when {
        !onboardingDone -> "onboarding"
        profiles.size >= 2 -> "profile_select"
        else -> "main"
    }

    NavHost(navController = navController, startDestination = startDestination) {
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
        composable("profile_select") {
            ProfileSelectScreen(
                profiles = profiles,
                onSelect = { profile ->
                    profileViewModel.setActiveProfile(profile)
                    navController.navigate("main") {
                        popUpTo("profile_select") { inclusive = true }
                    }
                },
                onAddNew = { navController.navigate("add_edit_profile/new") },
            )
        }
        composable("main") {
            MainScreen(
                mainViewModel = mainViewModel,
                adsManager = adsManager,
                prefs = prefs,
                statsManager = statsManager,
                profileViewModel = profileViewModel,
                challengeViewModel = challengeViewModel,
                onOpenDashboard = { navController.navigate("dashboard") },
            )
        }
        composable("dashboard") {
            ParentDashboardScreen(
                prefs = prefs,
                onBack = { navController.popBackStack() },
                profileViewModel = profileViewModel,
                challengeViewModel = challengeViewModel,
                onNavigateToAddEdit = { profileId ->
                    navController.navigate("add_edit_profile/${profileId ?: "new"}")
                },
            )
        }
        composable("add_edit_profile/{profileId}") { backStack ->
            val profileId = backStack.arguments?.getString("profileId")
            val profile = if (profileId == "new") null
                          else profiles.firstOrNull { it.id == profileId }
            AddEditProfileScreen(
                profile = profile,
                onSave = { saved ->
                    if (profile == null) profileViewModel.insertProfile(saved)
                    else profileViewModel.updateProfile(saved)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}
