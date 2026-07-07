# Faza 15 — Daily Challenge Streak Milestone Celebrations: Dizayn Spesifikatsiyasi

**Sana:** 2026-07-07
**Holat:** Approved
**Bog'liq:** `docs/superpowers/specs/2026-07-01-faza14-daily-challenge-streak-design.md`

---

## Maqsad

Faza 14 da qo'shilgan Daily Challenge streak (Room DB, `DailyChallengeRepository`) ma'lum kunlar sonига yetganda (3, 7, 14, 30 kun) bolaga vizual tabriklash ko'rsatish — retention ni yanada mustahkamlash uchun.

Faza 14 spec'ida bu ochiq qoldirilgan edi: *"Streak badge award (masalan, 7 kunlik badge) — keyingi fazaga"*.

---

## Muhim kontekst: ikkita mustaqil streak tizimi

Ilovada allaqachon ikkita mustaqil streak hisoblagichi mavjud:

1. **JS `BadgeManager.checkStreak()`** (`main.js`) — har qanday faoliyat (o'yin/hikoya/qo'shiq) o'ynalgan kunni hisoblaydi, `localStorage` da saqlanadi, `streak_3` 🔥 va `streak_7` 🔥🔥 badge larni allaqachon beradi (Achievements grid + 50 ⭐ bonus orqali).
2. **Kotlin/Room DB streak** (Faza 14) — faqat kunlik tayinlangan Daily Challenge bajarilganda oshadi, Firestore ga sinxronlanadi, `DailyChallengeCard` va `ParentalDashboardScreen` da ko'rinadi.

**Qaror:** yangi milestone tabriklash faqat **Daily Challenge streak** (Kotlin/Room DB) ga bog'lanadi — bu Faza 14 ning asosiy retention mexanizmi. Eski JS `streak_3`/`streak_7` badge tizimiga tegilmaydi, hech qanday bridge/reconciliation qilinmaydi — ular mustaqil ravishda ishlashda davom etadi.

**Qaror:** tabriklash **faqat vizual** — yangi ball/badge iqtisodiyoti yo'q, JS bridge (`evaluateJavascript`) chaqirilmaydi. Butunlay Kotlin/Compose tomonida amalga oshiriladi.

---

## Qamrov

**Kiradi:**
- `StreakEntity` ga `lastCelebratedMilestone: Int` maydoni + `MIGRATION_2_3`
- `DailyChallengeRepository.updateStreak()` — milestone kesib o'tilganini aniqlash
- `DailyChallengeViewModel` — `celebrateMilestone` state + `onCelebrationShown()`
- `DailyChallengeCard` — vaqtinchalik tabriklash matni/animatsiyasi

**Kirmaydi:**
- JS `BadgeManager` / `BADGE_DEFS` o'zgarishi
- Ball (stars) bonusi
- Yangi Dialog/overlay komponenti
- Firestore schema o'zgarishi (`lastCelebratedMilestone` faqat local, ota-ona uchun kerak emas)
- Push notification

---

## Milestone chegaralari

**3, 7, 14, 30 kun.**

| Milestone | Emoji | Matn (uz) |
|---|---|---|
| 3 | 🔥 | "3 kun ketma-ket! Ajoyib boshlanish!" |
| 7 | 🔥🔥 | "Bir hafta ketma-ket! Zo'r!" |
| 14 | 🔥🔥🔥 | "Ikki hafta ketma-ket!" |
| 30 | 🔥🔥🔥🔥 | "Bir oy ketma-ket! Sen chempion!" |

---

## Ma'lumotlar qatlami

### `StreakEntity` o'zgarishi

```kotlin
@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val profileId: String,
    val count: Int = 0,
    val lastCompletedDate: String = "",
    val lastCelebratedMilestone: Int = 0,   // yangi
)
```

`KidZoneDatabase.kt`: `version 2 → 3`, `MIGRATION_2_3` (yangi ustun qo'shish, default 0 — Faza 14 dagi `MIGRATION_1_2` bilan bir xil uslub).

### `DailyChallengeRepository`

```kotlin
private val milestones = listOf(3, 7, 14, 30)

suspend fun markChallengeCompleted(profileId: String, gameId: String): Int? {
    val today = todayProvider()
    val id = "$profileId-$today"
    val challenge = challengeDao.getById(id) ?: return null
    if (challenge.gameId != gameId || challenge.completed) return null

    challengeDao.markCompleted(id)
    return updateStreak(profileId, today, gameId, challenge.gameTitle)
}

private suspend fun updateStreak(profileId: String, today: String, gameId: String, gameTitle: String): Int? {
    val current = streakDao.getByProfile(profileId) ?: StreakEntity(profileId)
    val yesterday = java.time.LocalDate.parse(today).minusDays(1).toString()
    val newCount = when (current.lastCompletedDate) {
        today -> current.count
        yesterday -> current.count + 1
        else -> 1
    }
    val resetMilestone = if (newCount == 1 && current.count > 1) 0 else current.lastCelebratedMilestone
    val newlyReached = milestones.lastOrNull { it <= newCount && it > resetMilestone }
    val celebratedMilestone = newlyReached ?: resetMilestone
    streakDao.upsert(StreakEntity(profileId, newCount, today, celebratedMilestone))

    val uid = try {
        FirebaseAuth.getInstance().currentUser?.uid
    } catch (_: Exception) {
        null
    } ?: return newlyReached
    firestoreSync.syncStreak(uid, profileId, newCount, today)
    firestoreSync.syncChallengeCompleted(uid, profileId, today, gameId, gameTitle)
    return newlyReached
}
```

`markChallengeCompleted`'ning qaytish turi `Unit` dan `Int?` ga o'zgaradi — bitta chaqiruvchi (`DailyChallengeViewModel.onGameClosed`) bor, boshqa joyda ishlatilmaydi.

### Restore (Firestore'dan qayta o'rnatish) edge case

`onFirstLaunch()` Firestore'dan `count` ni tiklaganda, `lastCelebratedMilestone` ni **restored count'dan past yoki teng eng katta milestone** ga o'rnatish kerak (0 emas!) — aks holda 10 kunlik tiklangan streak keyingi challenge bajarilganda noto'g'ri "3 kun" tabrigini chiqarib yuboradi.

---

## ViewModel qatlami

```kotlin
data class ChallengeState(
    val challenge: DailyChallengeEntity? = null,
    val streakCount: Int = 0,
    val isLoading: Boolean = true,
    val celebrateMilestone: Int? = null,   // yangi — UI tomonidan bir marta iste'mol qilinadi
)

fun onGameClosed(gameId: String) {
    val profileId = activeProfileId ?: return
    viewModelScope.launch {
        val milestone = repository.markChallengeCompleted(profileId, gameId)
        val challenge = repository.getTodayChallenge(profileId)
        val streak = repository.getStreak(profileId)
        if (activeProfileId != profileId) return@launch
        _state.update { it.copy(challenge = challenge, streakCount = streak.count, celebrateMilestone = milestone) }
    }
}

fun onCelebrationShown() {
    _state.update { it.copy(celebrateMilestone = null) }
}
```

**Profil almashtirish:** `onProfileChanged` → `loadChallenge` yangi `ChallengeState` yaratadi (`celebrateMilestone` default `null`) — shuning uchun tez profil almashtirish avtomatik ravishda kutilayotgan tabriklashni bekor qiladi, qo'shimcha guard shart emas.

---

## UI — `DailyChallengeCard`

Yangi Dialog/overlay komponenti **yo'q** — Faza 14 dagi mavjud falsafaga mos ravishda ("karta o'zi yangilanadi, alohida overlay yo'q"). `completed` holatidagi mavjud `AnimatedContent` ichida, `celebrateMilestone != null` bo'lganda oddiy streak matni o'rniga milestone matni/emoji ko'rsatiladi, 2.5 soniyadan keyin avtomatik odatdagi holatga qaytadi:

```kotlin
@Composable
fun DailyChallengeCard(
    streakCount: Int,
    challenge: DailyChallengeEntity?,
    visible: Boolean,
    celebrateMilestone: Int?,
    onCelebrationShown: () -> Unit,
    onPlay: (gameId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(celebrateMilestone) {
        if (celebrateMilestone != null) {
            delay(2500)
            onCelebrationShown()
        }
    }
    // ...mavjud AnimatedVisibility/Card/AnimatedContent o'zgarmaydi...
    // `completed` branch ichida: celebrateMilestone != null bo'lsa,
    // "🔥 $streakCount kun streak" o'rniga MILESTONE_COPY[celebrateMilestone]
    // (kattaroq emoji + tabrik matni) ko'rsatiladi shu 2.5s oyna davomida.
}
```

**Nega konfetti/animatsiya kutubxonasi yo'q:** loyihada bunday dependency hozircha mavjud emas; mavjud `AnimatedContent(scaleIn + fadeIn)` o'tishi milestone xabarini alohida his qildirish uchun yetarli, qo'shimcha scope kerak emas.

`MainScreen.kt`: `state.celebrateMilestone` va `viewModel::onCelebrationShown` `DailyChallengeCard` ga uzatiladi — `streakCount`/`challenge` uchun ishlatilgan xuddi shu wiring uslubi.

---

## Chetga chiquvchi holatlar (edge cases)

- **Firestore restore:** yuqorida tavsiflangan — `lastCelebratedMilestone` restored count asosida to'g'ri initsializatsiya qilinadi, 0 emas.
- **Streak reset:** `newCount == 1` va oldingi `count > 1` bo'lsa, `lastCelebratedMilestone` ham 0 ga qaytadi — yangi streak boshlanishida milestone lar qayta qo'lga kiritilishi mumkin.
- **Bir xil kunda takroriy bajarish:** `markChallengeCompleted` allaqachon `challenge.completed` tekshiruvi bilan qisqartiriladi — `updateStreak` ga yetib bormaydi, takroriy milestone signal chiqmaydi.
- **Tabriklash oynasida tez profil almashtirish:** yuqorida tavsiflangan — `loadChallenge` orqali avtomatik tozalanadi.

---

## Fayl xaritasi

| # | Harakat | Fayl | Nima o'zgaradi |
|---|---------|------|----------------|
| 1 | Modify | `app/.../data/KidZoneDatabase.kt` | `StreakEntity.lastCelebratedMilestone` + `MIGRATION_2_3` |
| 2 | Modify | `app/.../data/DailyChallengeRepository.kt` | `updateStreak()` milestone aniqlash, `markChallengeCompleted()` `Int?` qaytaradi |
| 3 | Modify | `app/.../ui/viewmodel/DailyChallengeViewModel.kt` | `celebrateMilestone` state + `onCelebrationShown()` |
| 4 | Modify | `app/.../ui/screens/DailyChallengeCard.kt` | milestone matni/animatsiyasi, `LaunchedEffect` timer |
| 5 | Modify | `app/.../ui/MainScreen.kt` | `celebrateMilestone`/`onCelebrationShown` wiring |

---

## Testlash

Faza 14 dagi mavjud TDD uslubiga mos ravishda:

- **`DailyChallengeRepositoryTest`:** har bir milestone (3/7/14/30) kesib o'tilganda `updateStreak`/`markChallengeCompleted` uni qaytaradi; kesib o'tilmasa `null`; streak uzilganda `lastCelebratedMilestone` 0 ga qaytadi; tiklangan yuqori count past milestone ni qayta ishga tushirmaydi.
- **`DailyChallengeViewModelTest`:** `onGameClosed` faqat repository non-null qaytarganda `celebrateMilestone` ni to'ldiradi; `onCelebrationShown` uni tozalaydi; profil almashtirish kutilayotgan tabriklashni bekor qiladi.
- **Migration test:** `MIGRATION_2_3` mavjud `MIGRATION_1_2` testi bilan bir xil qatordagi additive-column tekshiruvi (schema validatsiyasi, mavjud qatorlar yo'qolmasligi).

## Muvaffaqiyat mezonlari

- [ ] Daily Challenge streak 3, 7, 14 yoki 30 kunga yetganda karta vaqtincha tabrik matnini ko'rsatadi
- [ ] Tabrik 2.5 soniyadan keyin avtomatik odatdagi "Bajarildi!" holatiga qaytadi
- [ ] Bir xil milestone qayta ko'rsatilmaydi (kunlik bajarish davomida)
- [ ] Streak uzilib, qayta boshlanganda milestone lar qayta qo'lga kiritiladi
- [ ] Firestore'dan tiklangan streak past milestone ni noto'g'ri qayta ishga tushirmaydi
- [ ] JS `BadgeManager` / `streak_3` / `streak_7` xatti-harakati o'zgarmaydi
- [ ] Build `BUILD SUCCESSFUL`, mavjud testlar (Faza 14) buzilmaydi
