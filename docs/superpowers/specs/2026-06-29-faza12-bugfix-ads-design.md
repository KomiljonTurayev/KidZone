# Faza 12 — Bug Fix Sprint + Ads Optimization: Dizayn Spesifikatsiyasi

**Sana:** 2026-06-29
**Holat:** Approved
**Versiya:** versionCode `12`, versionName `"1.2.1"`
**Bog'liq:** `docs/superpowers/specs/2026-06-28-faza11-multi-profile-design.md`

---

## Maqsad

Faza 11 dan keyin qolgan uchta tasdiqlangan bug ni yopish va interstitial reklama chastotasini nazorat ostiga olish — keyin imzolangan release AAB (`1.2.1`) chiqarish.

---

## Qamrov

**Kiradi:**
- T1: Interstitial chastotasi nazorati (`AdsManager.kt`)
- T2: `statsManager` profil almashinuvi bug fix (`ParentalStatsManager.kt`, `MainScreen.kt`)
- T3: Vaqt limiti darhol tekshirish (`MainScreen.kt`) — T2 bilan birgalikda hal bo'ladi
- T4: Logcat bug hunt + aniqlangan xatolar fix
- T5: Version bump + imzolangan release AAB

**Kirmaydi:**
- TTS fallback — allaqachon `main.js` da mavjud ✅
- Back button overlay fix — allaqachon `MainScreen.kt` da mavjud ✅
- ProGuard AdMob fix — allaqachon annotation-based qoida mavjud ✅
- Banner placement — faqat `MainScreen` da, profil ekranlarida chiqmaydi ✅
- Yangi kontent, UI refactor, yangi feature

---

## Tasdiqlangan buglar

### Bug 1: Interstitial har o'yin ochilishida chiqadi

**Manba:** `main.js` line 587 — `openGame()` metodida:
```javascript
window.AndroidAdMob.showInterstitial();
```
`AdsManager.showInterstitial()` da hech qanday chastota nazorati yo'q. Har o'yin bosishda interstitial ko'rsatiladi.

### Bug 2: `statsManager` profil almashinuvini bilmaydi

**Manba:** `MainActivity.onCreate()` line 41:
```kotlin
statsManager = ParentalStatsManager(this, activeProfileId)
```
`activeProfileId` bir marta o'qiladi. Profil o'zgarganda `statsManager` eski profil ID'si bilan ishlaydi — vaqt limiti noto'g'ri profil uchun tekshiriladi.

### Bug 3: Vaqt limiti 30 soniya kechikib tekshiriladi

**Manba:** `MainScreen.kt` `LaunchedEffect` — `delay(30_000)` birinchi tekshirishdan OLDIN turadi. Limit allaqachon oshgan profil 30s grace period oladi.

---

## Texnik dizayn

### T1: Interstitial chastotasi — `AdsManager.kt`

`showInterstitial()` metodiga counter + cooldown qo'shiladi:

```kotlin
private var gameCount = 0
private var lastInterstitialMs = 0L
private val INTERSTITIAL_EVERY_N_GAMES = 3
private val INTERSTITIAL_COOLDOWN_MS = 5 * 60 * 1000L

override fun showInterstitial() {
    gameCount++
    val now = System.currentTimeMillis()
    val cooldownOk = (now - lastInterstitialMs) >= INTERSTITIAL_COOLDOWN_MS
    val countOk = (gameCount % INTERSTITIAL_EVERY_N_GAMES) == 0
    if (cooldownOk && countOk && interstitialAd != null) {
        interstitialAd!!.show(activity)
        interstitialAd = null
        lastInterstitialMs = now
        loadInterstitial()
    }
}
```

**Mantiq:** har 3ta o'yin VA oxirgi interstitialdan kamida 5 daqiqa o'tgan bo'lsa ko'rsatiladi. Ikkala shart bir vaqtda bajarilishi kerak.

---

### T2 + T3: `ParentalStatsManager` + `MainScreen.kt`

#### `ParentalStatsManager.kt`

`profileId` `val` → `var` ga o'zgaradi. `switchProfile()` metodi qo'shiladi:

```kotlin
private var profileId: String  // val → var

fun switchProfile(newId: String) {
    if (newId == profileId) return
    onSessionEnd()     // eski profil vaqtini SharedPreferences ga saqlaydi
    profileId = newId  // yangi profil
    onSessionStart()   // yangi sessiya boshlaydi
}
```

#### `MainScreen.kt` — `LaunchedEffect` refactor

```kotlin
LaunchedEffect(activeProfile?.id) {
    activeProfile?.id?.let { statsManager.switchProfile(it) }
    while (true) {
        val limit = activeProfile?.timeLimitMinutes ?: 0
        if (limit > 0 && statsManager.getTodayMinutes() >= limit) {
            mainViewModel.showLock()
        }
        delay(30_000)
    }
}
```

O'zgarishlar:
- Key `activeProfile` → `activeProfile?.id` (profil object o'zgarganda, ID o'zgarishini kuzatish)
- `switchProfile()` chaqiruvi loop boshida
- `delay` birinchi tekshirishdan KEYIN (T3 ham shu bilan hal bo'ladi)

---

### T4: Logcat bug hunt

Qurilmaga APK install → 5 ta senariy qo'lda test:

1. O'yin ochish × 5 → interstitial 3-o'yinda chiqishi kerak, 1va 2da yo'q
2. Profil almashtirish → vaqt limiti yangi profil uchun tekshirilishi kerak
3. Vaqt limiti trigger → profil ochilganda darhol lock
4. Back tugmasi: lyrics-viewer → yopilsin, ai-viewer → yopilsin, o'yin → dialog
5. TTS: audio yuklanmasa → qo'shiq matni o'qilsin

Logcat filtri:
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -s "KidZone","AdsManager","chromium" -d | Select-String "Error|Exception|FATAL"
```

Aniqlangan xatolar shu task ostida fix qilinadi.

---

### T5: Version bump + release AAB

`app/build.gradle`:
```groovy
versionCode  ... : 12
versionName  ... : "1.2.1"
```

```powershell
.\gradlew bundleRelease
```

Natija: `app/build/outputs/bundle/release/app-release.aab`

---

## Fayl o'zgarishlari jadvali

| Fayl | O'zgarish | Task |
|------|-----------|------|
| `app/src/main/java/uz/kidzone/app/AdsManager.kt` | `gameCount`, `lastInterstitialMs`, chastota nazorati | T1 |
| `app/src/main/java/uz/kidzone/app/ParentalStatsManager.kt` | `profileId` `var`, `switchProfile()` metodi | T2 |
| `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt` | `LaunchedEffect` key + `switchProfile` chaqiruvi + `delay` oxirga | T2+T3 |
| `app/build.gradle` | versionCode `12`, versionName `"1.2.1"` | T5 |
| TBD (logcat orqali) | Bug hunt topilmalari | T4 |

---

## Muvaffaqiyat mezonlari

- [ ] Interstitial 1va 2-o'yinda chiqmaydi, 3-o'yinda chiqadi
- [ ] 5 daqiqadan kam vaqtda ikkinchi interstitial chiqmaydi
- [ ] Profil almashganda vaqt limiti yangi profil uchun tekshiriladi
- [ ] Profil ochilganda (limit oshgan bo'lsa) 30s kutilmasdan darhol lock chiqadi
- [ ] Logcat da `Error|Exception|FATAL` yo'q
- [ ] `.\gradlew bundleRelease` → `BUILD SUCCESSFUL`
- [ ] `app-release.aab` generatsiya qilinadi
