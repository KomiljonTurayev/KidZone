# Faza 7: Firebase Sync — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ota-ona Dashboard ga optional Firebase Email login + cloud sync qo'shish — login bo'lsa kunlik statistika va profil Firestore ga yuklanadi.

**Architecture:** `DashboardViewModel` ga `login()`, `register()`, `logout()`, `syncNow()` metodlari qo'shiladi; `FirebaseManager` va `FirestoreSync` mavjud — faqat wiring kerak. `ParentDashboardScreen` ga PIN dan keyin `CloudLoginSection` qo'shiladi (Skip tugmasi bilan), login bo'lsa cloud status item ko'rsatiladi.

**Tech Stack:** Kotlin, Jetpack Compose Material3, Firebase Auth, Firestore (already in dependencies).

## Global Constraints

- Faqat 2 fayl o'zgaradi: `DashboardViewModel.kt` va `ParentDashboardScreen.kt`
- `FirebaseManager.kt`, `FirestoreSync.kt`, `MainActivity.kt` o'zgartirilmaydi
- `login()` va `register()` — `FirebaseManager.AuthCallback` interface orqali async
- `syncNow()` — `statsManager` va `prefs` ViewModel da mavjud, parametr kerak emas
- `recordSession(uid, sessionMinutes: Long, gamePlays: Map<String, String>?, isFirstSession: Boolean)`
- `syncUserProfile(uid: String?, displayName: String?, email: String?, ageGroup: String?)`
- `cloudResolved` — `remember { mutableStateOf(state.firebaseUid != null) }` — PIN dan keyin
- Firebase UID bor bo'lsa login ekrani ko'rsatilmaydi (avtomatik o'tadi)

---

## File Map

| Harakat | Fayl | Nima o'zgaradi |
|---------|------|----------------|
| Modify | `app/src/main/java/uz/kidzone/app/ui/viewmodel/DashboardViewModel.kt` | `DashboardState` + `initFirebase()` + `login()` + `register()` + `logout()` + `syncNow()` |
| Modify | `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt` | 3 import + `cloudResolved` state + `CloudLoginSection` composable + cloud status item |

---

## Task 1: DashboardViewModel — Firebase state + auth methods

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/viewmodel/DashboardViewModel.kt` (butun fayl, 71 satr)

**Interfaces:**
- Consumes: `FirebaseManager.getInstance().getCurrentUser(): FirebaseUser?`
- Consumes: `FirebaseManager.getInstance().signInWithEmail(email, password, AuthCallback)`
- Consumes: `FirebaseManager.getInstance().createAccountWithEmail(email, password, AuthCallback)`
- Consumes: `FirebaseManager.getInstance().signOut()`
- Consumes: `FirestoreSync.getInstance().syncUserProfile(uid, null, email, ageGroup)`
- Consumes: `FirestoreSync.getInstance().recordSession(uid, minutes: Long, games: Map<String,String>?, false)`
- Produces: `DashboardState.isSyncing: Boolean`, `DashboardState.lastSyncTime: String?`, `DashboardState.loginError: String?`
- Produces: `vm.login(email, pass, onDone: (Boolean)->Unit)`
- Produces: `vm.register(email, pass, onDone: (Boolean)->Unit)`
- Produces: `vm.logout()`
- Produces: `vm.syncNow()` (no params — uses ViewModel's statsManager + prefs)

---

- [ ] **Step 1: `DashboardViewModel.kt` ni to'liq almashtirish**

Faylni butunlay quyidagi kod bilan almashtiring:

```kotlin
package uz.kidzone.app.ui.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uz.kidzone.app.FirebaseManager
import uz.kidzone.app.FirestoreSync
import uz.kidzone.app.ParentalStatsManager

data class DashboardState(
    val todayMinutes: Int = 0,
    val weeklyMinutes: List<Int> = List(7) { 0 },
    val todayGames: List<String> = emptyList(),
    val timeLimitMinutes: Int = 0,
    val age: String = "2-4",
    val pushEnabled: Boolean = true,
    val notifHistory: List<String> = emptyList(),
    val firebaseUid: String? = null,
    val firebaseEmail: String? = null,
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val loginError: String? = null,
)

class DashboardViewModel(
    private val statsManager: ParentalStatsManager,
    private val prefs: SharedPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        refresh()
        initFirebase()
    }

    fun refresh() {
        _state.update {
            it.copy(
                todayMinutes = statsManager.getTodayMinutes(),
                weeklyMinutes = statsManager.getWeeklyMinutes().toList(),
                todayGames = statsManager.getTodayGames(),
                timeLimitMinutes = statsManager.getTimeLimitMinutes(),
                age = prefs.getString("kz_age", "2-4") ?: "2-4",
                pushEnabled = prefs.getBoolean("kz_push_enabled", true),
            )
        }
    }

    fun initFirebase() {
        val user = FirebaseManager.getInstance().getCurrentUser()
        _state.update { it.copy(firebaseUid = user?.uid, firebaseEmail = user?.email) }
    }

    fun login(email: String, password: String, onDone: (Boolean) -> Unit) {
        _state.update { it.copy(isSyncing = true, loginError = null) }
        FirebaseManager.getInstance().signInWithEmail(email, password, object : FirebaseManager.AuthCallback {
            override fun onSuccess(user: FirebaseUser) {
                _state.update { it.copy(
                    firebaseUid = user.uid,
                    firebaseEmail = user.email,
                    isSyncing = false,
                    loginError = null,
                ) }
                onDone(true)
            }
            override fun onError(message: String) {
                _state.update { it.copy(loginError = message, isSyncing = false) }
                onDone(false)
            }
        })
    }

    fun register(email: String, password: String, onDone: (Boolean) -> Unit) {
        _state.update { it.copy(isSyncing = true, loginError = null) }
        FirebaseManager.getInstance().createAccountWithEmail(email, password, object : FirebaseManager.AuthCallback {
            override fun onSuccess(user: FirebaseUser) {
                _state.update { it.copy(
                    firebaseUid = user.uid,
                    firebaseEmail = user.email,
                    isSyncing = false,
                    loginError = null,
                ) }
                onDone(true)
            }
            override fun onError(message: String) {
                _state.update { it.copy(loginError = message, isSyncing = false) }
                onDone(false)
            }
        })
    }

    fun logout() {
        FirebaseManager.getInstance().signOut()
        _state.update { it.copy(
            firebaseUid = null,
            firebaseEmail = null,
            lastSyncTime = null,
            loginError = null,
        ) }
    }

    fun syncNow() {
        val uid = _state.value.firebaseUid ?: return
        val email = _state.value.firebaseEmail
        val ageGroup = prefs.getString("kz_age", "2-4") ?: "2-4"
        _state.update { it.copy(isSyncing = true) }
        val sync = FirestoreSync.getInstance()
        sync.syncUserProfile(uid, null, email, ageGroup)
        val minutes = statsManager.getTodayMinutes().toLong()
        val games = statsManager.getTodayGames().associateWith { "played" }
        sync.recordSession(uid, minutes, games, false)
        _state.update { it.copy(isSyncing = false, lastSyncTime = "Az vaqt oldin") }
    }

    fun increaseLimit() {
        val current = statsManager.getTimeLimitMinutes()
        val next = if (current >= 180) 180 else current + 15
        statsManager.setTimeLimitMinutes(next)
        _state.update { it.copy(timeLimitMinutes = next) }
    }

    fun decreaseLimit() {
        val current = statsManager.getTimeLimitMinutes()
        val next = if (current <= 0) 0 else current - 15
        statsManager.setTimeLimitMinutes(next)
        _state.update { it.copy(timeLimitMinutes = next) }
    }

    fun setAge(age: String) {
        prefs.edit().putString("kz_age", age).apply()
        _state.update { it.copy(age = age) }
    }

    fun setPushEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("kz_push_enabled", enabled).apply()
        _state.update { it.copy(pushEnabled = enabled) }
    }
}
```

---

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/viewmodel/DashboardViewModel.kt
git commit -m "feat(faza7): DashboardViewModel Firebase auth + sync methods"
```

---

## Task 2: ParentDashboardScreen — CloudLoginSection + cloud status

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt`

**Interfaces:**
- Consumes: Task 1 dan: `vm.login()`, `vm.register()`, `vm.logout()`, `vm.syncNow()`, `state.isSyncing`, `state.loginError`, `state.firebaseUid`, `state.firebaseEmail`, `state.lastSyncTime`
- Produces: `CloudLoginSection` — PIN dan keyin ko'rinadigan login UI
- Produces: Cloud status item (LazyColumn item #6) — faqat `firebaseUid != null` bo'lsa

---

### 2a. Yangi importlar qo'shish

`ParentDashboardScreen.kt` da import blokining oxirini toping. Hozir mavjud importlar (`uz.kidzone.app.*` dan keyin) ga uchta yangi qator qo'shing:

Mavjud oxirgi import qatori:
```kotlin
import uz.kidzone.app.ui.viewmodel.DashboardViewModel
```

Undan KEYIN quyidagilarni qo'shing:
```kotlin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.style.TextAlign
import uz.kidzone.app.ui.viewmodel.DashboardState
```

---

### 2b. `cloudResolved` state va `CloudLoginSection` qo'shish

`ParentDashboardScreen.kt` da PIN gate blokini toping (line 64-78):

```kotlin
    // PIN gate
    var pinVerified by remember { mutableStateOf(false) }
    val savedPinHash = remember { PinUtil.getOrMigrateHash(prefs, "kz_pin") }

    if (!pinVerified) {
        PinGate(
            hasPinSet = !savedPinHash.isNullOrEmpty(),
            onPinCorrect = { pin ->
                if (savedPinHash.isNullOrEmpty() || PinUtil.matches(pin, savedPinHash)) {
                    pinVerified = true
                }
            },
            onBack = onBack,
        )
        return
    }

    Scaffold(
```

- [ ] **Step 1: PIN gate dan keyin, `Scaffold(` dan OLDIN `cloudResolved` blokini qo'shish**

`return` va `Scaffold(` o'rtasiga quyidagini qo'shing:

```kotlin
    return
    }

    // Cloud login gate (optional)
    var cloudResolved by remember { mutableStateOf(state.firebaseUid != null) }
    if (!cloudResolved) {
        CloudLoginSection(
            state = state,
            onLogin = { email, pass ->
                vm.login(email, pass) { success -> if (success) cloudResolved = true }
            },
            onRegister = { email, pass ->
                vm.register(email, pass) { success -> if (success) cloudResolved = true }
            },
            onSkip = { cloudResolved = true },
        )
        return
    }

    Scaffold(
```

Natija (line 77 atrofida):
```kotlin
        return
    }

    // Cloud login gate (optional)
    var cloudResolved by remember { mutableStateOf(state.firebaseUid != null) }
    if (!cloudResolved) {
        CloudLoginSection(
            state = state,
            onLogin = { email, pass ->
                vm.login(email, pass) { success -> if (success) cloudResolved = true }
            },
            onRegister = { email, pass ->
                vm.register(email, pass) { success -> if (success) cloudResolved = true }
            },
            onSkip = { cloudResolved = true },
        )
        return
    }

    Scaffold(
```

---

### 2c. Cloud status item LazyColumn ga qo'shish

`ParentDashboardScreen.kt` da LazyColumn ichidagi item 5 — "Push bildirishnomalar" blokini toping (line 154-164):

```kotlin
            // 5. Push bildirishnomalar
            item {
                Text("Push bildirishnomalar", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bildirishnomalar", modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.pushEnabled,
                        onCheckedChange = { vm.setPushEnabled(it) },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Push item dan keyin (`}` — items yopuvchi, `}` — LazyColumn yopuvchi dan OLDIN) cloud status item qo'shish**

`Switch` blokidan keyin, `}` (items closing) dan OLDIN:

```kotlin
            // 5. Push bildirishnomalar
            item {
                Text("Push bildirishnomalar", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bildirishnomalar", modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.pushEnabled,
                        onCheckedChange = { vm.setPushEnabled(it) },
                    )
                }
            }

            // 6. Cloud status (faqat login bo'lsa)
            if (state.firebaseUid != null) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Cloud sinxronlash", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "☁️ Ulangan: ${state.firebaseEmail ?: state.firebaseUid}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!state.lastSyncTime.isNullOrEmpty()) {
                        Text(
                            "Oxirgi sync: ${state.lastSyncTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.syncNow() },
                            enabled = !state.isSyncing,
                        ) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Sinxronlash")
                            }
                        }
                        OutlinedButton(onClick = {
                            vm.logout()
                            cloudResolved = false
                        }) {
                            Text("Chiqish")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
```

---

### 2d. `CloudLoginSection` private composable qo'shish

`ParentDashboardScreen.kt` da faylning oxirini toping — `ChangePinDialog` composable dan keyin (`}` — fayl oxiri). Faylning OXIRIGA (closing `}` dan keyin) quyidagi composable ni qo'shing:

- [ ] **Step 3: `CloudLoginSection` composable ni fayl oxiriga qo'shish**

```kotlin
@Composable
private fun CloudLoginSection(
    state: DashboardState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onSkip: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("☁️ Cloud Sinxronlash", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Statistika va profil ma'lumotlarini bulutga saqlang",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Parol") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
        )

        if (!state.loginError.isNullOrEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                state.loginError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onLogin(email, password) },
                enabled = !state.isSyncing && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Kirish")
                }
            }
            OutlinedButton(
                onClick = { onRegister(email, password) },
                enabled = !state.isSyncing && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Ro'yxat")
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onSkip) {
            Text("O'tkazib yuborish →")
        }
    }
}
```

---

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt
git commit -m "feat(faza7): CloudLoginSection + cloud status card in ParentDashboardScreen"
```

---

## Task 3: Build, Install va Verify

**Files:** Hech qanday fayl o'zgartirilmaydi

- [ ] **Step 1: Build va install**

```powershell
cd D:\android_projects\KidZone
.\gradlew installDebug
```

Kutilgan natija: `BUILD SUCCESSFUL` + APK qurilmaga o'rnatiladi.

- [ ] **Step 2: Login flow tekshiruvi (Firebase UID yo'q holat)**

Qurilmada SharedPreferences ni tozalash shart emas — agar oldin login bo'lmagan bo'lsa:
1. App ochiladi → ota-ona tugmasi → PIN kiritiladi
2. `CloudLoginSection` ko'rinadi: "☁️ Cloud Sinxronlash" sarlavha, email/parol maydonlar, 3 tugma
3. Noto'g'ri parol kiritilsa → qizil xato matni ko'rinadi ✅
4. "O'tkazib yuborish →" bosilsa → Dashboard to'g'ridan ochiladi (cloud status yo'q) ✅

- [ ] **Step 3: Email login tekshiruvi**

1. CloudLoginSection da to'g'ri email/parol kiritiladi
2. "Kirish" bosiladi → loading spinner ko'rinadi → Dashboard ochiladi ✅
3. Dashboard pastida item #6: "☁️ Ulangan: email@..." + "Sinxronlash" + "Chiqish" ✅

- [ ] **Step 4: Ro'yxatdan o'tish tekshiruvi**

1. Firebase console da mavjud bo'lmagan yangi email bilan "Ro'yxat" bosiladi
2. → Dashboard ochiladi ✅ (yoki Firebase console da yangi user yaratilganligini tekshiring)

- [ ] **Step 5: Sinxronlash tekshiruvi**

1. Dashboard da "Sinxronlash" bosiladi
2. "Oxirgi sync: Az vaqt oldin" matni ko'rinadi ✅
3. Firebase Firestore console da `stats` + `users` kolleksiyalarida yangilangan ma'lumotlar ✅

- [ ] **Step 6: Chiqish + login takroriy tekshiruvi**

1. "Chiqish" bosiladi
2. CloudLoginSection qaytadan ko'rinadi ✅
3. Yana login bosilsa → Dashboard ✅

- [ ] **Step 7: Firebase UID bor holat (ikkinchi ochilish)**

1. App yopiladi va qaytadan ochiladi
2. Ota-ona → PIN → Dashboard (CloudLoginSection ko'rsatilmaydi, to'g'ridan) ✅
3. Firebase session saqlanadi (FirebaseAuth persists by default) ✅

- [ ] **Step 8: Logcat Kotlin xato tekshiruvi**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -s AndroidRuntime -d 2>&1 | Select-String "FATAL|Exception" | Select-Object -Last 10
```

Kutilgan natija: hech qanday FATAL xato yo'q.

- [ ] **Step 9: Commit**

```bash
git commit --allow-empty -m "chore(faza7): build verified — Firebase login + cloud sync working"
```
