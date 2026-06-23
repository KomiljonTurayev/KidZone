# Faza 9: Vaqt Limiti Auto-Lock — Dizayn Spesifikatsiyasi

**Sana:** 2026-06-23
**Holat:** Approved

---

## Maqsad

`statsManager.isTimeLimitReached()` hech qachon tekshirilmaydi va `mainViewModel.showLock()` hech qachon chaqirilmaydi — lock ekrani mavjud, lekin avtomatik ishlamaydi. Faza 9 buni to'g'irlaydi:

1. `MainScreen` da `LaunchedEffect` — har 60 soniyada vaqt limitini tekshiradi
2. Lock ekranida PIN himoyasi — "Ota-ona uchun 🔐" tugmasi PIN dialog ochadi
3. `PinGate` composable `internal` ga o'zgartiriladi — `MainScreen` dan qayta ishlatiladi

---

## Joriy holat

| Komponent | Holat |
|-----------|-------|
| `ParentalStatsManager.isTimeLimitReached()` | Mavjud, ishlaydi ✅ |
| `MainViewModel.showLock()` / `hideLock()` | Mavjud ✅ |
| Lock ekranining ko'rinishi | Mavjud, `MainScreen.kt` da ✅ |
| Lock avtomatik chaqirish | **Hech qerda yo'q** ❌ |
| Lock ekranida PIN himoyasi | **Yo'q** — to'g'ridan `hideLock()` ❌ |
| `PinGate` composable | `ParentDashboardScreen.kt` da `private` — boshqa fayldan foydalanib bo'lmaydi ❌ |

---

## Muammo tahlili

### 1. Vaqt limiti ishlamasligi

`statsManager.isTimeLimitReached()` mavjud va to'g'ri ishlaydi:
```kotlin
fun isTimeLimitReached(): Boolean = limit > 0 && getTodayMinutes() >= limit
```

Lekin bu metod hech qachon chaqirilmaydi. `showLock()` faqat qo'lda (test uchun) chaqirilishi mumkin edi.

### 2. PIN himoyasi yo'qligi

Lock ekranida:
```kotlin
Button(onClick = { mainViewModel.hideLock() }) {
    Text("Ota-ona uchun 🔐")
}
```
Bola ham bu tugmani bosib lock ni yopa oladi — xavfsizlik muammosi.

---

## Yechim

### LaunchedEffect — vaqt tekshiruvi

`MainScreen` da `LaunchedEffect(Unit)` qo'shiladi:

```kotlin
LaunchedEffect(Unit) {
    while (true) {
        if (statsManager.isTimeLimitReached()) {
            mainViewModel.showLock()
        }
        delay(60_000L)
    }
}
```

**Mantiq:**
- App ochilganda darhol tekshiradi (delay KEYIN — birinchi iteration zudlik bilan)
- Har 60 soniyada qayta tekshiradi
- `isTimeLimitReached()` `limit == 0` bo'lsa `false` qaytaradi — limit o'rnatilmagan holda lock chiqmaydi
- `showLock()` idempotent — ko'p marta chaqirish muammo yaratmaydi

### statsManager uzatish zanjiri

`statsManager: ParentalStatsManager` parametri 3 ta fayldan o'tkaziladi:

```
MainActivity.onCreate()
  └─ statsManager = ParentalStatsManager(this)    ← mavjud
  └─ KidZoneApp(statsManager = statsManager)       ← yangi
       └─ MainScreen(statsManager = statsManager)  ← yangi
            └─ LaunchedEffect { isTimeLimitReached() }
```

### PinGate → internal

`ParentDashboardScreen.kt` da:
```kotlin
// Hozir:
private fun PinGate(...)

// Yangi:
internal fun PinGate(...)
```

Bir so'z o'zgarishi — `MainScreen.kt` va boshqa `app` moduli fayllari uchun ochiladi.

### Lock ekranida PIN dialog

```kotlin
// Lock overlay ichida:
var showUnlockPin by remember { mutableStateOf(false) }

Button(onClick = { showUnlockPin = true }) {
    Text("Ota-ona uchun 🔐")
}

if (showUnlockPin) {
    PinGate(
        prefs = prefs,
        onVerified = {
            mainViewModel.hideLock()
            showUnlockPin = false
        },
        onDismiss = { showUnlockPin = false },
    )
}
```

---

## Fayl O'zgarishlari

| Fayl | Nima |
|------|------|
| `app/src/main/java/uz/kidzone/app/MainActivity.kt` | `KidZoneApp(statsManager = statsManager)` qo'shish |
| `app/src/main/java/uz/kidzone/app/ui/KidZoneApp.kt` | `statsManager: ParentalStatsManager` param + `MainScreen` ga uzatish |
| `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt` | `statsManager: ParentalStatsManager` param + `LaunchedEffect` + `showUnlockPin` PIN gate |
| `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt` | `private fun PinGate` → `internal fun PinGate` |

**O'zgartirilmaydi:** `ParentalStatsManager.kt`, `PinUtil.kt`, `MainViewModel.kt`, `main.js`

---

## PinGate imzosi (mavjud, o'zgarmaydi)

```kotlin
internal fun PinGate(
    prefs: SharedPreferences,
    onVerified: () -> Unit,
    onDismiss: () -> Unit,
)
```

`MainScreen` da `prefs: SharedPreferences` allaqachon parametr sifatida mavjud (line 57).

---

## Muvaffaqiyat mezonlari

- [ ] Vaqt limiti o'rnatilgan holda (masalan 5 daqiqa) — limit tugagach lock ekrani chiqadi
- [ ] Vaqt limiti o'rnatilmagan holda — lock ekrani hech qachon chiqmaydi
- [ ] Lock ekranidagi "Ota-ona uchun 🔐" tugmasi PIN dialog ochadi
- [ ] Noto'g'ri PIN → "PIN noto'g'ri ❌" xabari, lock yopilmaydi
- [ ] To'g'ri PIN → lock yopiladi, bola davom eta oladi
- [ ] Build `BUILD SUCCESSFUL`, runtime xato yo'q
