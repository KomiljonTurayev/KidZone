# Faza 7: Firebase Sync — Dizayn Spesifikatsiyasi

**Sana:** 2026-06-23
**Holat:** Approved

---

## Maqsad

Ota-ona Dashboard ga Firebase Email login + optional cloud sync qo'shish. Login ixtiyoriy — "O'tkazib yuborish" tugmasi bilan. Login bo'lgach, kunlik statistika (o'yin vaqti, o'yinlar ro'yxati) va profil ma'lumotlari Firestore ga yuklanadi.

---

## Joriy holat

- `FirebaseManager.kt` — `signInWithEmail()`, `createAccountWithEmail()`, `signOut()`, `getCurrentUser()`, `getUid()` ✅
- `FirestoreSync.kt` — `syncUserProfile()`, `recordSession()` ✅
- `DashboardViewModel.kt` — `firebaseUid`/`firebaseEmail` state maydoni bor lekin hech qachon to'ldirilmaydi ❌
- `ParentDashboardScreen.kt` — Firebase login UI yo'q ❌
- `google-services.json` — mavjud ✅

---

## Flow

```
Ota-ona FAB → PIN gate → [PIN to'g'ri]
  → Firebase UID yo'q? → CloudLoginSection
      "Kirish"  → signInWithEmail → sync → Dashboard
      "Ro'yxat" → createAccount  → sync → Dashboard
      "O'tkazib yuborish" → cloudResolved=true → Dashboard
  → Firebase UID bor? → Dashboard to'g'ridan-to'g'ri
```

**cloudResolved** — `remember` state, `pinVerified=true` bo'lganda init: `firebaseUid != null`.
Login bo'lganda yoki Skip bosilganda `cloudResolved = true`.

---

## Fayl O'zgarishlari

| Fayl | Nima |
|------|------|
| `app/src/main/java/uz/kidzone/app/ui/viewmodel/DashboardViewModel.kt` | `initFirebase()`, `login()`, `register()`, `logout()`, `syncNow()` + state |
| `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt` | `cloudResolved` state, `CloudLoginSection` composable, cloud status item |

**O'zgartirilmaydi:** `FirebaseManager.kt`, `FirestoreSync.kt`, `MainActivity.kt`, `index.html`, `main.js`

---

## DashboardState yangi maydonlar

Mavjud `DashboardState` ga qo'shimcha (hozir `firebaseUid`/`firebaseEmail` bor):

```kotlin
data class DashboardState(
    // ... mavjud maydonlar o'zgarishsiz ...
    val firebaseUid: String? = null,
    val firebaseEmail: String? = null,
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val loginError: String? = null,
)
```

---

## DashboardViewModel yangi metodlar

### `initFirebase()`
```kotlin
fun initFirebase() {
    val user = FirebaseManager.getInstance().getCurrentUser()
    _state.update { it.copy(firebaseUid = user?.uid, firebaseEmail = user?.email) }
}
```
`init {}` blokida chaqiriladi.

### `login(email, password, onDone)`
```kotlin
fun login(email: String, password: String, onDone: (Boolean) -> Unit) {
    _state.update { it.copy(isSyncing = true, loginError = null) }
    FirebaseManager.getInstance().signInWithEmail(email, password, object : FirebaseManager.AuthCallback {
        override fun onSuccess(user: FirebaseUser) {
            _state.update { it.copy(
                firebaseUid = user.uid,
                firebaseEmail = user.email,
                isSyncing = false,
                loginError = null
            ) }
            onDone(true)
        }
        override fun onError(message: String) {
            _state.update { it.copy(loginError = message, isSyncing = false) }
            onDone(false)
        }
    })
}
```

### `register(email, password, onDone)`
`login()` bilan bir xil, lekin `createAccountWithEmail()` chaqiriladi:
```kotlin
fun register(email: String, password: String, onDone: (Boolean) -> Unit) {
    _state.update { it.copy(isSyncing = true, loginError = null) }
    FirebaseManager.getInstance().createAccountWithEmail(email, password, object : FirebaseManager.AuthCallback {
        override fun onSuccess(user: FirebaseUser) {
            _state.update { it.copy(
                firebaseUid = user.uid,
                firebaseEmail = user.email,
                isSyncing = false,
                loginError = null
            ) }
            onDone(true)
        }
        override fun onError(message: String) {
            _state.update { it.copy(loginError = message, isSyncing = false) }
            onDone(false)
        }
    })
}
```

### `logout()`
```kotlin
fun logout() {
    FirebaseManager.getInstance().signOut()
    _state.update { it.copy(
        firebaseUid = null,
        firebaseEmail = null,
        lastSyncTime = null,
        loginError = null
    ) }
}
```

### `syncNow(statsManager, prefs)`
```kotlin
fun syncNow(statsManager: ParentalStatsManager, prefs: SharedPreferences) {
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
```

---

## ParentDashboardScreen o'zgarishi

### `cloudResolved` state (PIN dan keyin)

```kotlin
// PIN gate dan keyin, `pinVerified = true` kontekstida:
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
```

### `CloudLoginSection` (private composable, ParentDashboardScreen.kt ichida)

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
            Text(state.loginError, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onLogin(email, password) },
                enabled = !state.isSyncing && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                if (state.isSyncing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Kirish")
            }
            OutlinedButton(
                onClick = { onRegister(email, password) },
                enabled = !state.isSyncing && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text("Ro'yxat") }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onSkip) {
            Text("O'tkazib yuborish →")
        }
    }
}
```

### Cloud status item (LazyColumn item #6 — faqat `firebaseUid != null` bo'lsa)

```kotlin
if (state.firebaseUid != null) {
    item {
        Text("Cloud sinxronlash", style = MaterialTheme.typography.titleMedium)
        Text("☁️ Ulangan: ${state.firebaseEmail ?: state.firebaseUid}",
            style = MaterialTheme.typography.bodyMedium)
        if (!state.lastSyncTime.isNullOrEmpty()) {
            Text("Oxirgi sync: ${state.lastSyncTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.syncNow(statsManager, prefs) },
                enabled = !state.isSyncing,
            ) {
                if (state.isSyncing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Sinxronlash")
            }
            OutlinedButton(onClick = {
                vm.logout()
                cloudResolved = false
            }) { Text("Chiqish") }
        }
        Spacer(Modifier.height(16.dp))
    }
}
```

---

## Muvaffaqiyat mezonlari

- [ ] Login bo'lmagan holatda: PIN → `CloudLoginSection` ko'rinadi
- [ ] "O'tkazib yuborish" → Dashboard to'g'ridan ochiladi
- [ ] Email/parol bilan "Kirish" → Firebase auth → Dashboard
- [ ] Email/parol bilan "Ro'yxat" → Firebase createAccount → Dashboard
- [ ] Xato (noto'g'ri parol) → qizil xato matni ko'rinadi
- [ ] Firebase UID bor → PIN dan keyin to'g'ridan Dashboard (login ekrani yo'q)
- [ ] Dashboard da: "☁️ Ulangan: email@..." + "Sinxronlash" + "Chiqish" ko'rinadi
- [ ] "Sinxronlash" → `FirestoreSync.syncUserProfile()` + `recordSession()` chaqiriladi
- [ ] "Chiqish" → signOut → login ekrani ko'rsatiladi
- [ ] Build `BUILD SUCCESSFUL`, runtime xato yo'q
