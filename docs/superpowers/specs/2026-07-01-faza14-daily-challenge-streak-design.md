# Faza 14 — Daily Challenge + Streak: Dizayn Spesifikatsiyasi

**Sana:** 2026-07-01
**Holat:** Approved
**Versiya:** versionCode `14`, versionName `"1.4.0"`
**Bog'liq:** `docs/superpowers/specs/2026-06-30-faza13-play-store-pipeline-design.md`

---

## Maqsad

Har kungi tasodifiy o'yin vazifasi (Daily Challenge) va ketma-ket kunlar hisoblagichi (Streak) orqali foydalanuvchi retention ni oshirish. Har bir profil o'zining streak va challenge ga ega. Ma'lumot Room DB da local saqlanadi, Firestore ga sinxronlanadi.

---

## Qamrov

**Kiradi:**
- T1: `KidZoneDatabase.kt` — 2 ta yangi entity (`DailyChallengeEntity`, `StreakEntity`) + DAO lar
- T2: `DailyChallengeRepository.kt` — challenge generatsiya + streak hisoblash logikasi
- T3: `DailyChallengeViewModel.kt` — MainScreen uchun state boshqaruvi
- T4: `DailyChallengeCard.kt` — Compose composable (streak + o'yin nomi + tugma + completion state)
- T5: `MainScreen.kt` — DailyChallengeCard ni o'yinlar ro'yxati yuqorisiga qo'shish
- T6: `main.js` — `closeGame()` da challenge callback
- T7: `KidWebViewManager.kt` — `@JavascriptInterface onGameCompleted()`
- T8: `FirestoreSync.kt` — streak va challenge sync
- T9: `ParentalDashboardScreen.kt` — profil kartasiga streak satri

**Kirmaydi:**
- Push notifications (FCM reminder) — keyingi fazaga
- Streak badge award (masalan, 7 kunlik badge) — keyingi fazaga
- Backend (Spring Boot) o'zgarish — Firestore yetarli
- Yangi o'yinlar yoki kontent
- Tablet UI optimizatsiyasi

---

## Arxitektura

```
┌─────────────────────────────────────────────────────┐
│  MainScreen (Compose)                               │
│  ┌──────────────────────────────────────┐           │
│  │  🔥 5 kun  │  Bugun: Memory Match   │           │
│  │            │  [O'ynash →]           │           │
│  └──────────────────────────────────────┘           │
│  [O'yinlar ro'yxati...]                             │
└─────────────────────────────────────────────────────┘
         │
         ▼
  DailyChallengeViewModel
         │
  DailyChallengeRepository
         │
   ┌─────┴──────┐
   │            │
Room DB      Firestore
(asosiy)     (zahira/sync)
```

---

## Ma'lumotlar qatlami

### Room DB — yangi entity lar

```kotlin
@Entity(tableName = "daily_challenge")
data class DailyChallengeEntity(
    @PrimaryKey val id: String,        // "$profileId-$date", e.g. "p1-2026-07-01"
    val profileId: String,
    val date: String,                  // "YYYY-MM-DD"
    val gameId: String,                // e.g. "memory-match"
    val gameTitle: String,             // e.g. "Memory Match"
    val completed: Boolean = false
)

@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val profileId: String,
    val count: Int = 0,
    val lastCompletedDate: String = "" // "YYYY-MM-DD"
)
```

Mavjud `KidZoneDatabase.kt` ga qo'shiladi (alohida DB yaratilmaydi). Joriy `version = 1` → `version = 2` ga o'tadi, `MIGRATION_1_2` (empty migration, faqat yangi jadvallar) qo'shiladi.

### Repository logikasi

**`getTodayChallenge(profileId: String)`:**
1. DB dan `id = "$profileId-$today"` qidiradi
2. Topilsa — qaytaradi (kun davomida o'zgarmaydi)
3. Topilmasa — `content.json` dan tasodifiy `gameId` + `title` tanlanadi, DB ga saqlanadi, qaytaradi

**`markChallengeCompleted(profileId: String, gameId: String)`:**
1. `daily_challenge` da `completed = true` qiladi
2. `StreakEntity` yangilaydi:
   - `lastCompletedDate == kecha` → `count + 1`
   - `lastCompletedDate == bugun` → o'zgarmaydi (takroriy chaqiruv)
   - boshqa holat → `count = 1` (streak reset)
3. Firestore ga background coroutine orqali yuboradi

### O'yinlar ro'yxati manbasi

`app/src/main/assets/www/index.html` ichidagi `GAMES` konstant array (line ~1236) — har bir o'yin `{id, em, name:{uz,ru,en}, file, ...}` strukturasida. `content.json` faqat stories/songs saqlaydi, o'yinlar emas.

Kotlin tomoni o'yinlar ro'yxatini JS dan oladi: app init da `window.AndroidChallenge.onGamesLoaded(json)` chaqiriladi. Bu hardcode dan qochadi — yangi o'yin qo'shilsa avtomatik tanlanadi.

---

## UI

### DailyChallengeCard (faol holat)

```
┌────────────────────────────────────────────┐
│  🔥 5 kun streak                           │
│  ────────────────────────────────────────  │
│  Bugungi vazifa                            │
│  🎮 Memory Match                           │
│                          [O'ynash →]       │
└────────────────────────────────────────────┘
```

### DailyChallengeCard (bajarilgan holat)

```
┌────────────────────────────────────────────┐
│  🔥 6 kun streak          ✅ Bajarildi!    │
│  Bugungi vazifa: Memory Match              │
│  Ertaga yangi vazifa kutilmoqda 🌟         │
└────────────────────────────────────────────┘
```

**Animatsiya:** `completed = true` bo'lganda `AnimatedContent` + `scale` animatsiyasi. Alohida dialog yoki overlay yo'q — karta o'zi yangilanadi.

**Joylashuv:** `MainScreen.kt` da o'yinlar ro'yxati `LazyColumn` yuqorisida, fixed header sifatida.

---

## O'yin yakunlanish detection

### main.js (WebView tomoni)

`openGame()` va `closeGame()` ga qo'shimchalar:

```javascript
// openGame(g) ichida, iframe src o'rnatilgandan keyin:
if (window.AndroidChallenge) {
    window.AndroidChallenge.onGameOpened(g.id);
}

// closeGame() ichida, mavjud game topilgandan keyin:
// const game = this.games.find(g => titleText.includes(g.em)); ← mavjud kod
if (game && window.AndroidChallenge) {
    window.AndroidChallenge.onGameClosed(game.id);
}

// GameManager constructor da bir marta:
if (window.AndroidChallenge) {
    const list = JSON.stringify(this.games.map(g => ({
        id: g.id,
        title: typeof g.name === 'object' ? (g.name.uz || g.name.en) : g.name
    })));
    window.AndroidChallenge.onGamesLoaded(list);
}
```

### KidWebViewManager.kt (Kotlin tomoni)

```kotlin
@JavascriptInterface
fun onGamesLoaded(json: String) {
    // games ro'yxatini repository ga saqlaydi (random tanlov uchun)
    repository.updateGamesList(json)
}

@JavascriptInterface
fun onGameOpened(gameId: String) {
    currentOpenGameId = gameId
}

@JavascriptInterface
fun onGameClosed(gameId: String) {
    coroutineScope.launch {
        val challenge = repository.getTodayChallenge(activeProfileId)
        if (challenge?.gameId == gameId && !challenge.completed) {
            repository.markChallengeCompleted(activeProfileId, gameId)
        }
    }
    currentOpenGameId = null
}
```

---

## Firestore sync

**Struktura** (mavjud `FirestoreSync.kt` ga qo'shiladi):

```
users/{userId}/profiles/{profileId}/
  streak:
    count: 6
    lastCompletedDate: "2026-07-01"
  daily_challenges/{date}:
    gameId: "memory-match"
    gameTitle: "Memory Match"
    completed: true
    completedAt: Timestamp
```

**Sync qoidalari:**
- `markChallengeCompleted()` Room DB ni sinxron yangilaydi, Firestore ni background coroutine orqali
- Firestore xatolikda silent fail — local ma'lumot asosiy manba
- App ochilganda Firestore dan streak tortib olinmaydi — faqat yangi qurilmada yoki reinstall da restore qilinadi
- Restore: `onFirstLaunch()` da Firestore dan `streak` document o'qiladi, Room DB ga yoziladi

---

## Parental Dashboard integratsiyasi

`ParentalDashboardScreen.kt` da mavjud profil kartasiga streak satri qo'shiladi:

```
👤 Ali     🔥 6 kun streak    Bugun: ✅
👤 Zara    🔥 1 kun streak    Bugun: ⏳
```

Alohida ekran yoki dialog kerak emas.

---

## Fayl xaritasi

| # | Harakat | Fayl | Nima o'zgaradi |
|---|---------|------|----------------|
| T1 | Modify | `app/.../data/KidZoneDatabase.kt` | `DailyChallengeEntity`, `StreakEntity`, DAOlar qo'shish |
| T2 | Create | `app/.../data/DailyChallengeRepository.kt` | yangi fayl |
| T3 | Create | `app/.../ui/viewmodel/DailyChallengeViewModel.kt` | yangi fayl |
| T4 | Create | `app/.../ui/screens/DailyChallengeCard.kt` | yangi composable |
| T5 | Modify | `app/.../ui/MainScreen.kt` | DailyChallengeCard qo'shish |
| T6 | Modify | `app/src/main/assets/www/main.js` | `closeGame()` callback |
| T7 | Modify | `app/.../KidWebViewManager.kt` | `@JavascriptInterface onGameCompleted()` |
| T8 | Modify | `app/.../FirestoreSync.kt` | streak + challenge sync |
| T9 | Modify | `app/.../ui/screens/ParentalDashboardScreen.kt` | streak satri |

---

## Texnik cheklovlar

- `main.js` da faqat `closeGame()` ga minimal qo'shimcha — boshqa logika o'zgarmaydi
- Streak hisobi qurilma soati bo'yicha (`LocalDate.now()`) — server vaqt kerak emas
- Room DB: `version 1 → 2`, `MIGRATION_1_2` (yangi jadvallar uchun empty migration — `addMigrations` orqali)
- Firestore xarajat: faqat challenge yakunida 1 write + restore da 1 read — minimal
