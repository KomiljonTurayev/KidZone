# Faza 9: Vaqt Limiti Auto-Lock — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `MainScreen` da har 60 soniyada vaqt limitini tekshirib, tugasa ekranni bloklash; lock ekranidagi "Ota-ona uchun 🔐" tugmasini PIN himoyasi bilan ta'minlash.

**Architecture:** `PinGate` composable `private` → `internal` ga o'zgartiriladi. `statsManager: ParentalStatsManager` `MainActivity` → `KidZoneApp` → `MainScreen` zanjiri orqali uzatiladi. `MainScreen` da `LaunchedEffect(Unit)` coroutine har 60 soniyada `isTimeLimitReached()` tekshiradi — `true` bo'lsa `showLock()` chaqiriladi. Lock ekranida `showUnlockPin` state bilan `PinGate` dialog ochiladi — to'g'ri PIN → `hideLock()`.

**Tech Stack:** Kotlin, Jetpack Compose, `kotlinx.coroutines.delay`, `ParentalStatsManager`, `PinUtil`.

## Global Constraints

- `ParentalStatsManager.kt`, `PinUtil.kt`, `MainViewModel.kt`, `main.js` o'zgartirilmaydi
- `LaunchedEffect(Unit)` — check AVVAL, delay KEYIN (app ochilganda darhol tekshiradi)
- `delay(60_000L)` — 60 soniya interval
- `PinGate` imzosi: `hasPinSet: Boolean, onPinCorrect: (String) -> Unit, onBack: () -> Unit`
- `PinUtil.getOrMigrateHash(prefs, "kz_pin")` — PIN hash ni o'qish
- `PinUtil.matches(pin, savedPinHash)` — PIN tekshirish
- Lock ekranida: `showUnlockPin` va `savedPinHash` `if (uiState.isLocked)` bloki ichida `remember` bilan saqlanadi

---

## File Map

| Harakat | Fayl | Nima o'zgaradi |
|---------|------|----------------|
| Modify | `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt:249` | `private fun PinGate` → `internal fun PinGate` |
| Modify | `app/src/main/java/uz/kidzone/app/MainActivity.kt:66-70` | `statsManager = statsManager` parametri qo'shish |
| Modify | `app/src/main/java/uz/kidzone/app/ui/KidZoneApp.kt:16-45` | `statsManager: ParentalStatsManager` param + MainScreen ga uzatish |
| Modify | `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt` | `statsManager` param + 4 import + `LaunchedEffect` + PIN dialog |

---

## Task 1: PinGate → internal

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt:249`

**Interfaces:**
- Produces: `internal fun PinGate(hasPinSet: Boolean, onPinCorrect: (String) -> Unit, onBack: () -> Unit)` — Task 3 da `MainScreen` dan chaqiriladi

---

- [ ] **Step 1: `private` → `internal` o'zgartirish**

`ParentDashboardScreen.kt` line 249 da:

**Hozirgi:**
```kotlin
@Composable
private fun PinGate(
```

**Yangi:**
```kotlin
@Composable
internal fun PinGate(
```

Faqat bitta so'z o'zgaradi. `PinKeypad` (line 318) `private` bo'lib qoladi — `PinGate` u bilan bir faylda, shuning uchun muammo yo'q.

---

- [ ] **Step 2: Compile tekshiruvi**

```powershell
.\gradlew :app:compileDebugKotlin
```

Kutilgan natija: `BUILD SUCCESSFUL`. Hech qanday xato bo'lmasligi kerak — bu faqat visibility o'zgarishi.

---

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt
git commit -m "refactor(faza9): expose PinGate as internal for cross-screen reuse"
```

---

## Task 2: statsManager zanjiri — MainActivity → KidZoneApp → MainScreen imzosi

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/MainActivity.kt:66-70`
- Modify: `app/src/main/java/uz/kidzone/app/ui/KidZoneApp.kt`
- Modify: `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt:54-59` (faqat imzo)

**Interfaces:**
- Consumes: `statsManager: ParentalStatsManager` — `MainActivity` da allaqachon mavjud (line 23, 40)
- Produces: `MainScreen(statsManager: ParentalStatsManager, ...)` — Task 3 da body ishlatadi

---

- [ ] **Step 1: `MainActivity.kt` — `KidZoneApp` chaqiruviga `statsManager` qo'shish**

`MainActivity.kt` line 65-71. Hozirgi:
```kotlin
        setContent {
            KidZoneApp(
                prefs = kzPrefs,
                mainViewModel = mainViewModel,
                adsManager = adsManager,
            )
        }
```

Yangi:
```kotlin
        setContent {
            KidZoneApp(
                prefs = kzPrefs,
                mainViewModel = mainViewModel,
                adsManager = adsManager,
                statsManager = statsManager,
            )
        }
```

---

- [ ] **Step 2: `KidZoneApp.kt` — param va import qo'shish**

`KidZoneApp.kt` ni to'liq quyidagicha almashtiring:

```kotlin
package uz.kidzone.app.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.kidzone.app.AdsManager
import uz.kidzone.app.ParentalStatsManager
import uz.kidzone.shared.AndroidSettingsProvider
import uz.kidzone.shared.ui.screens.OnboardingScreen
import uz.kidzone.app.ui.screens.ParentDashboardScreen
import uz.kidzone.app.ui.viewmodel.MainViewModel

@Composable
fun KidZoneApp(
    prefs: SharedPreferences,
    mainViewModel: MainViewModel,
    adsManager: AdsManager,
    statsManager: ParentalStatsManager,
) {
    val navController = rememberNavController()
    val onboardingDone = prefs.getBoolean("kz_onboarding_done", false)
    val settings = remember(prefs) { AndroidSettingsProvider(prefs) }

    NavHost(
        navController = navController,
        startDestination = if (onboardingDone) "main" else "onboarding",
    ) {
        composable("onboarding") {
            OnboardingScreen(
                settings = settings,
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
            )
        }
    }
}
```

---

- [ ] **Step 3: `MainScreen.kt` — imzoga `statsManager` param qo'shish**

`MainScreen.kt` line 54-59. Hozirgi:
```kotlin
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    adsManager: AdsManager,
    prefs: SharedPreferences,
    onOpenDashboard: () -> Unit,
) {
```

Yangi (faqat `statsManager` parametri va import qo'shiladi — body o'zgarmaydi):
```kotlin
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    adsManager: AdsManager,
    prefs: SharedPreferences,
    statsManager: ParentalStatsManager,
    onOpenDashboard: () -> Unit,
) {
```

Import blokiga (mavjud `uz.kidzone.app.AdsManager` dan keyin) qo'shing:
```kotlin
import uz.kidzone.app.ParentalStatsManager
```

---

- [ ] **Step 4: Compile tekshiruvi**

```powershell
.\gradlew :app:compileDebugKotlin
```

Kutilgan natija: `BUILD SUCCESSFUL`. Agar `None of the following candidates is applicable` xatosi bo'lsa — `MainScreen` chaqiruvidagi parametrlar nomini tekshiring.

---

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/MainActivity.kt
git add app/src/main/java/uz/kidzone/app/ui/KidZoneApp.kt
git add app/src/main/java/uz/kidzone/app/ui/MainScreen.kt
git commit -m "feat(faza9): thread statsManager through MainActivity→KidZoneApp→MainScreen"
```

---

## Task 3: MainScreen — LaunchedEffect + PIN lock dialog

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt`

**Interfaces:**
- Consumes: `statsManager: ParentalStatsManager` — Task 2 da imzoga qo'shildi
- Consumes: `internal fun PinGate(hasPinSet, onPinCorrect, onBack)` — Task 1 da `internal` qilindi
- Consumes: `PinUtil.getOrMigrateHash(prefs, "kz_pin"): String?` — mavjud
- Consumes: `PinUtil.matches(pin: String, storedHash: String?): Boolean` — mavjud
- Consumes: `mainViewModel.showLock()` / `mainViewModel.hideLock()` — mavjud

---

- [ ] **Step 1: 4 ta import qo'shish**

`MainScreen.kt` import blokiga qo'shing:

```kotlin
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import uz.kidzone.app.PinUtil
import uz.kidzone.app.ui.screens.PinGate
```

---

- [ ] **Step 2: Lock overlay — PIN dialog qo'shish**

`MainScreen.kt` da lock overlay blokini toping (line 108-125 atrofida). Hozirgi:

```kotlin
        // Lock overlay
        if (uiState.isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌙", style = MaterialTheme.typography.displayLarge)
                    Text("Bugungi vaqting tugadi", color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { mainViewModel.hideLock() }) {
                        Text("Ota-ona uchun 🔐")
                    }
                }
            }
        }
```

Yangi — `showUnlockPin` state va `savedPinHash` qo'shiladi, tugma `hideLock()` o'rniga dialog ochadi, `PinGate` dialog qo'shiladi:

```kotlin
        // Lock overlay
        if (uiState.isLocked) {
            var showUnlockPin by remember { mutableStateOf(false) }
            val savedPinHash = remember { PinUtil.getOrMigrateHash(prefs, "kz_pin") }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌙", style = MaterialTheme.typography.displayLarge)
                    Text("Bugungi vaqting tugadi", color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showUnlockPin = true }) {
                        Text("Ota-ona uchun 🔐")
                    }
                }
            }
            if (showUnlockPin) {
                PinGate(
                    hasPinSet = !savedPinHash.isNullOrEmpty(),
                    onPinCorrect = { pin ->
                        if (savedPinHash.isNullOrEmpty() || PinUtil.matches(pin, savedPinHash)) {
                            mainViewModel.hideLock()
                            showUnlockPin = false
                        }
                    },
                    onBack = { showUnlockPin = false },
                )
            }
        }
```

---

- [ ] **Step 3: LaunchedEffect — vaqt tekshiruvi qo'shish**

`MainScreen.kt` da `BackHandler` blokini toping (line 177 atrofida). `BackHandler` dan OLDIN (Column/Box yopilgandan keyin) qo'shing:

```kotlin
    // Time limit check — every 60 seconds
    LaunchedEffect(Unit) {
        while (true) {
            if (statsManager.isTimeLimitReached()) {
                mainViewModel.showLock()
            }
            delay(60_000L)
        }
    }

    // BackHandler
    BackHandler {
```

---

- [ ] **Step 4: Compile tekshiruvi**

```powershell
.\gradlew :app:compileDebugKotlin
```

Kutilgan natija: `BUILD SUCCESSFUL`. Agar `Unresolved reference: PinGate` bo'lsa — Task 1 ni (`internal`) amalga oshirilganini tekshiring. Agar `Unresolved reference: delay` bo'lsa — `import kotlinx.coroutines.delay` borligini tekshiring.

---

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/MainScreen.kt
git commit -m "feat(faza9): LaunchedEffect time check + PIN-protected lock unlock"
```

---

## Task 4: Build, Install va Verify

**Files:** Hech qanday fayl o'zgartirilmaydi.

---

- [ ] **Step 1: To'liq build va qurilmaga o'rnatish**

```powershell
.\gradlew installDebug
```

Kutilgan natija: `BUILD SUCCESSFUL` + APK qurilmaga o'rnatiladi.

---

- [ ] **Step 2: Vaqt limiti tekshiruvi (qurilmada)**

1. Dashboard → Parental controls → Vaqt limiti → 1 daqiqa o'rnating
2. 1 daqiqa kutib turing (yoki `statsManager` debug uchun qisqaroq qiling)
3. Lock ekrani avtomatik chiqishi kerak ✅
4. "Bugungi vaqting tugadi" matni ko'rinishi kerak ✅

---

- [ ] **Step 3: PIN himoyasi tekshiruvi (qurilmada)**

1. Lock ekranida "Ota-ona uchun 🔐" tugmasini bosing
2. PIN dialog chiqishi kerak ✅
3. Noto'g'ri PIN kiriting → "PIN noto'g'ri ❌" ko'rinadi, lock yopilmaydi ✅
4. To'g'ri PIN kiriting → lock yopiladi, bola davom eta oladi ✅

---

- [ ] **Step 4: Limit o'rnatilmagan holda tekshiruvi**

1. Dashboard → vaqt limitini 0 ga o'rnating (yoki o'chiring)
2. Lock ekrani chiqmasligi kerak ✅

---

- [ ] **Step 5: Logcat tekshiruvi**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -s KidZone -d 2>&1 | Select-Object -Last 15
```

Kutilgan natija: Runtime crash yo'q.

---

- [ ] **Step 6: Commit**

```bash
git commit --allow-empty -m "chore(faza9): build verified — time limit auto-lock + PIN gate working"
```
