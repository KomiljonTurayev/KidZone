package uz.kidzone.app.ui

import uz.kidzone.app.arch.AppPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.app.data.DailyChallengeRepository
import uz.kidzone.app.data.KidZoneDatabase
import uz.kidzone.app.data.ProfileRepository
import uz.kidzone.app.data.ProfileSyncManager
import uz.kidzone.app.auth.AccountFeature
import uz.kidzone.app.ui.screens.AccountGate
import uz.kidzone.app.ui.screens.AddEditProfileScreen
import uz.kidzone.app.ui.screens.BannedScreen
import uz.kidzone.app.ui.screens.PaywallScreen
import uz.kidzone.app.ui.screens.OnboardingScreen
import uz.kidzone.app.ui.screens.ParentDashboardScreen
import uz.kidzone.app.ui.screens.ProfileSelectScreen
import uz.kidzone.app.ui.screens.SleepScreen
import uz.kidzone.app.ui.i18n.LocalAppLang
import uz.kidzone.app.ui.i18n.rememberAppLang
import uz.kidzone.app.ui.theme.KidZoneTheme
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModel
import android.content.SharedPreferences
import uz.kidzone.app.ui.viewmodel.DailyChallengeViewModelFactory
import uz.kidzone.app.ui.viewmodel.MainViewModel
import uz.kidzone.app.ui.viewmodel.ProfileViewModel
import uz.kidzone.app.ui.viewmodel.ProfileViewModelFactory

private const val UNLOCKED_FEATURE_KEY = "unlocked_feature"

@Composable
fun KidZoneApp(
    prefs: SharedPreferences,
    mainViewModel: MainViewModel,
    statsManager: ParentalStatsManager,
    onOnboardingDone: () -> Unit = {},
) {
    val mainUiState by mainViewModel.state.collectAsState()
    val appLang by rememberAppLang(prefs)
    if (mainUiState.isBanned) {
        KidZoneTheme {
            CompositionLocalProvider(LocalAppLang provides appLang) { BannedScreen() }
        }
        return
    }

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
    val gameProgress = remember {
        uz.kidzone.app.data.GameProgressRepository(
            dao = KidZoneDatabase.getInstance(context).levelUpDao(),
            firestoreSync = uz.kidzone.app.FirestoreSync.getInstance(),
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

    KidZoneTheme {
        CompositionLocalProvider(LocalAppLang provides appLang) {
            NavHost(navController = navController, startDestination = startDestination) {
                composable("onboarding") {
                    OnboardingScreen(
                        prefs = prefs,
                        profiles = profiles,
                        onSaveChild = { name, lang ->
                            // Every install starts with one default profile; the parent names it here.
                            val target = profiles.firstOrNull { it.isDefault } ?: profiles.firstOrNull()
                            if (target != null) {
                                val named = target.copy(name = name, language = lang)
                                profileViewModel.updateProfile(named)
                                profileViewModel.setActiveProfile(named)
                            }
                        },
                        onDone = {
                            onOnboardingDone()
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
                composable("main") { backStack ->
                    // Set by the "account/{feature}" route after a successful sign-in, so the
                    // feature the child asked for opens right away instead of needing a second tap.
                    val unlocked by backStack.savedStateHandle
                        .getStateFlow<String?>(UNLOCKED_FEATURE_KEY, null)
                        .collectAsState()
                    MainScreen(
                        mainViewModel = mainViewModel,
                        prefs = prefs,
                        statsManager = statsManager,
                        profileViewModel = profileViewModel,
                        challengeViewModel = challengeViewModel,
                        gameProgress = gameProgress,
                        onOpenDashboard = { navController.navigate("dashboard") },
                        onOpenSleep = { navController.navigate("sleep") },
                        onOpenPremium = { navController.navigate("premium") },
                        onRequireAccount = { feature -> navController.navigate("account/${feature.name}") },
                        unlockedFeature = unlocked?.let { name -> AccountFeature.entries.firstOrNull { it.name == name } },
                        onUnlockedFeatureHandled = { backStack.savedStateHandle[UNLOCKED_FEATURE_KEY] = null },
                    )
                }
                composable("account/{feature}") { backStack ->
                    val feature = backStack.arguments?.getString("feature")
                        ?.let { name -> AccountFeature.entries.firstOrNull { it.name == name } }
                        ?: AccountFeature.AI_COMPANION
                    val activeProfile by profileViewModel.activeProfile.collectAsState()
                    AccountGate(
                        feature = feature,
                        pinHash = activeProfile?.pinHash,
                        lang = appLang,
                        onSignedIn = {
                            navController.previousBackStackEntry?.savedStateHandle?.set(UNLOCKED_FEATURE_KEY, feature.name)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("premium") {
                    PaywallScreen(
                        onDismiss = { navController.popBackStack() },
                        onSuccess = { navController.popBackStack() },
                    )
                }
                composable("sleep") {
                    val curLang = appLang
                    SleepScreen(
                        lang = curLang,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("dashboard") {
                    ParentDashboardScreen(
                        onBack = { navController.popBackStack() },
                        profileViewModel = profileViewModel,
                        challengeViewModel = challengeViewModel,
                        gameProgress = gameProgress,
                        onNavigateToAddEdit = { profileId ->
                            navController.navigate("add_edit_profile/${profileId ?: "new"}")
                        },
                        // Reached only from the signed-in dashboard, so PREMIUM is already unlocked.
                        onOpenPremium = { navController.navigate("premium") },
                    )
                }
                composable("add_edit_profile/{profileId}") { backStack ->
                    val profileId = backStack.arguments?.getString("profileId")
                    val profile = if (profileId == "new") null
                                  else profiles.firstOrNull { it.id == profileId }
                    AddEditProfileScreen(
                        profile = profile,
                        // New profiles share the parent PIN (see ProfileViewModel.setParentPin).
                        parentPinHash = profiles.firstNotNullOfOrNull { it.pinHash },
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
    }
}
