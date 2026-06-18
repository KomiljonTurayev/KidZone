# Faza 2 — Parental Controls: Time-Limit Overlay + Dashboard Compose Migration

**Sana:** 2026-06-18  
**Holat:** Approved  
**Bog'liq:** `docs/superpowers/plans/2026-06-12-hybrid-migration-faza1.md`

---

## Maqsad

Faza 1'da defer qilingan `showLockOverlay()` ni implement qilish va `ParentalDashboardActivity`'ni Jetpack Compose'ga ko'chirish. Admin panel Java'da qoladi (Faza 3).

---

## Qamrov

**Kiradi:**
- `TimeLimitViewModel` — coroutine timer, 4-holatli state machine
- `TimeLimitOverlay` — Compose overlay (Warning, AwaitingPin, Locked)
- `ParentPinGateScreen` — PIN entry gate (NavHost route)
- `ParentDashboardScreen` — to'liq Compose dashboard (NavHost route)
- TDD: `TimeLimitViewModelTest` (fake timer bilan)

**Kirmaydi:**
- Admin panel Compose migratsiyasi (Faza 3)
- `ParentalDashboardActivity.java` o'chirilmaydi — admin panel uchun saqlanadi
- Stories / Songs / KidAI content (Faza 3)

---

## Arxitektura

### Komponentlar

```
ParentalStatsManager (Java, o'zgarishsiz)
    │  getTimeLimitMinutes()
    │  getTodayMinutes()
    │  isTimeLimitReached()
    │  setTimeLimitMinutes(minutes)
    ▼
TimeLimitViewModel
    │  StateFlow<TimeLimitState>
    │  StateFlow<Int>  minutesRemaining
    │  coroutine: har 30 soniyada tick()
    │  fun dismissWarning()
    │  fun requestExtend()
    │  fun verifyAndExtend(pin: String, hash: String)
    │  fun lock()
    ▼
KidZoneApp (setContent bloki)
    ├── TimeLimitOverlay   ← state asosida ko'rsatiladi, NavHost USTIDA
    └── NavHost
          ├── /games, /stories, /songs, /kidai  (mavjud)
          ├── /game/{gameId}                    (mavjud)
          ├── /parent/gate  → ParentPinGateScreen   (yangi)
          └── /parent       → ParentDashboardScreen  (yangi)
```

### TimeLimitState holatlari

```
Normal      — limit yo'q YOKI qolgan vaqt > 5 daqiqa
Warning     — qolgan vaqt ≤ 5 daqiqa → overlay ko'rsatiladi
AwaitingPin — farzand "Yana 15 daqiqa" yoki "Ota-ona uchun 🔐" bosdi
Locked      — vaqt tugadi va PIN kirmadi / bekor qilindi
Extended    — PIN to'g'ri, +15 daqiqa qo'shildi → Normal'ga qaytadi
```

O'tish qoidalari:

| Holat | Trigger | Keyingi holat |
|-------|---------|---------------|
| Normal | minutesRemaining ≤ 5 | Warning |
| Warning | minutesRemaining = 0 | Locked |
| Warning | "Davom etish" bosildi | Normal (dismiss) |
| Warning | "Yana 15 daqiqa" bosildi | AwaitingPin |
| AwaitingPin | To'g'ri PIN | Extended → Normal |
| AwaitingPin | Noto'g'ri PIN | AwaitingPin (shake) |
| AwaitingPin | "Bekor" bosildi | Locked |
| Locked | "Ota-ona uchun 🔐" bosildi | AwaitingPin |
| Extended | — | Normal (`setTimeLimitMinutes(current + 15)`, timer davom etadi) |

---

## UI Dizayni

### TimeLimitOverlay

**Warning state:**
```
┌─────────────────────────────────┐
│         ⏰                       │
│   Vaqting tugayapti!            │
│   5:00 daqiqa qoldi  (countdown)│
│                                 │
│  [Davom etish]  [Yana 15 daqiqa]│
└─────────────────────────────────┘
```
- Yarim shaffof qora background (alpha 0.85)
- Countdown real-time yangilanadi

**AwaitingPin state:**
```
┌─────────────────────────────────┐
│         🔐                       │
│   Ota-ona PIN kiriting           │
│   [● ● ● ●]                     │
│   [1][2][3]                     │
│   [4][5][6]                     │
│   [7][8][9]                     │
│      [0]                        │
│   [Bekor qilish]                │
└─────────────────────────────────┘
```
- To'g'ri PIN → Extended
- Noto'g'ri PIN → silkish (shake) animatsiya, qayta urinish
- "Bekor" → Locked

**Locked state:**
```
┌─────────────────────────────────┐
│         🌙                       │
│   Bugungi vaqting tugadi        │
│   Yaxshi dam ol!                │
│                                 │
│      [Ota-ona uchun 🔐]         │
└─────────────────────────────────┘
```
- To'liq qora background
- `onBackPressedDispatcher` override — Back button ishlamaydi
- "Ota-ona uchun 🔐" → AwaitingPin

### ParentPinGateScreen (`/parent/gate`)

- PIN raqam klaviaturasi (4 xona)
- To'g'ri PIN → `/parent` route'ga navigate
- Noto'g'ri → shake animatsiya
- Back → `/games`'ga qaytadi

### ParentDashboardScreen (`/parent`)

Seksiyalar (LazyColumn):

1. **Statistika** — bugungi daqiqalar + 7 kunlik bar chart (Canvas bilan)
2. **Bugun o'ynalgan o'yinlar** — emoji qatori (`getTodayGames()` dan)
3. **Yosh guruhi** — `FilterChip` row: 2-4 / 5-7 / 8+
4. **Vaqt limiti** — `[-]  45 min  [+]`, 15 daqiqalik qadamlar, 0 = "No limit"
5. **PIN** — "Change PIN" tugmasi → `PinDialogHelper.showCreate()` (Java, mavjud)
6. **Cloud Backup** — Firebase Auth sign-in/out, email ko'rsatish
7. **Back** — TopAppBar'da `←` tugmasi

Admin panel seksiyasi ko'rsatilmaydi (faqat `ParentalDashboardActivity`'da).

---

## Fayl Tuzilmasi (yangi fayllar)

```
app/src/main/java/uz/kidzone/app/
  ui/viewmodel/TimeLimitViewModel.kt
  ui/overlay/TimeLimitOverlay.kt
  ui/screens/ParentPinGateScreen.kt
  ui/screens/ParentDashboardScreen.kt

app/src/test/java/uz/kidzone/app/
  ui/viewmodel/TimeLimitViewModelTest.kt
```

**O'zgaradigan mavjud fayllar:**
- `ui/navigation/NavRoutes.kt` — `PARENT_GATE`, `PARENT` route'lar qo'shiladi
- `ui/navigation/KidZoneNavHost.kt` — ikkita yangi `composable()` qo'shiladi
- `ui/KidZoneApp.kt` — `TimeLimitOverlay` qatlami qo'shiladi
- `MainActivity.java` — `TimeLimitViewModel` wiring (hozirgi codeda Java versiyasi mavjud)

---

## Testing

### TimeLimitViewModelTest (TDD, Kotlin)

```
test_normalState_whenNoLimit()
test_warningState_whenFiveMinutesRemain()
test_lockedState_whenTimeExpires()
test_verifyAndExtend_correctPin_returnsToNormal()
test_verifyAndExtend_wrongPin_staysAwaitingPin()
test_dismissWarning_returnsToNormal()
test_lock_setsLockedState()
```

`TestScope` + `UnconfinedTestDispatcher` ishlatiladi — real 30 soniya kutilmaydi.

### ParentalStatsManager testlari

Mavjud 7 test o'zgarishsiz qoladi.

---

## Mavjud Java bilan integratsiya

| Java klass | Faza 2 roli |
|-----------|-------------|
| `ParentalStatsManager` | O'zgarishsiz — ViewModel to'g'ridan ishlaydi |
| `PinUtil` | `verifyAndExtend()` ichida `PinUtil.verify()` chaqiriladi |
| `PinDialogHelper` | Dashboard PIN o'zgartirish uchun saqlanadi |
| `FirebaseManager` | Cloud Backup seksiyasida ishlatiladi |
| `ParentalDashboardActivity` | Admin panel uchun saqlanadi, 🔒 tugmasi endi `/parent/gate`'ga yo'naltiradi |

---

## Muvaffaqiyat mezonlari

- [ ] Limit 30 daqiqa qo'yilsa, 25-daqiqada Warning overlay chiqadi
- [ ] Warning'da countdown real-time yangilanadi
- [ ] "Yana 15 daqiqa" → PIN to'g'ri → overlay yopiladi, +15 daqiqa qo'shiladi
- [ ] "Yana 15 daqiqa" → PIN noto'g'ri → shake, qayta urinish
- [ ] Vaqt tugasa Locked ekran chiqadi, Back button ishlamaydi
- [ ] `/parent/gate` PIN to'g'ri kiritilsa Dashboard ochiladi
- [ ] Dashboard'da bugungi vaqt va haftalik chart to'g'ri ko'rsatiladi
- [ ] Limit +/- tugmalari `ParentalStatsManager`'ga yozadi
- [ ] Yosh guruhi tanlanganda `kz_age` prefs'ga saqlanadi
- [ ] `TimeLimitViewModelTest` — 7 test yashil
