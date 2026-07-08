# Faza 15 — Daily Challenge Streak Milestone Celebrations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When a child's Daily Challenge streak crosses 3, 7, 14, or 30 consecutive days, show a temporary celebration message on `DailyChallengeCard` instead of the usual streak-count text, auto-dismissing after 2.5 seconds.

**Architecture:** Add a `lastCelebratedMilestone` column to `StreakEntity` (Room migration 2→3) so the repository can detect when a *new* milestone is crossed. `DailyChallengeRepository.markChallengeCompleted()` changes its return type from `Unit` to `Int?` (the milestone just reached, or `null`). `DailyChallengeViewModel` surfaces this as one-shot `celebrateMilestone` state, consumed by `DailyChallengeCard` via a `LaunchedEffect` timer. No new dependencies, no dialog/overlay component, no JS bridge calls — purely additive Kotlin/Compose changes on top of the existing Faza 14 Daily Challenge feature.

**Tech Stack:** Kotlin, Jetpack Compose, Room 2.6.1, kotlinx-coroutines, JUnit4 + Robolectric (existing test stack — unchanged).

## Global Constraints

- Milestone thresholds are exactly **3, 7, 14, 30** days (`app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt`).
- Celebration is **visual only** — no new stars/points economy, no `evaluateJavascript` bridge calls, no push notifications.
- Do **not** touch the JS `BadgeManager` / `streak_3` / `streak_7` badge system in `main.js` — it stays fully independent.
- No new Dialog/overlay Composable — the milestone message replaces the existing streak text inside `DailyChallengeCard`'s existing `AnimatedContent`, matching Faza 14's "the card updates itself" philosophy.
- No confetti/animation library — reuse the existing `AnimatedContent(scaleIn + fadeIn)` transition already in `DailyChallengeCard.kt`.
- Copy is Uzbek, exact strings (see Task 3).
- `Firestore` schema is untouched — `lastCelebratedMilestone` is local-only (Room), never synced.

**Restore edge case (deviation from spec):** the spec describes an `onFirstLaunch()` Firestore-restore path that must re-derive `lastCelebratedMilestone` from a restored `count` (so a restored 10-day streak doesn't wrongly re-fire the 3-day celebration). No such restore-from-Firestore function exists anywhere in this codebase today — `FirestoreSync` only has write methods (`syncStreak`, `syncChallengeCompleted`), never a read-and-restore path for streaks. There is nothing to modify for this edge case; it's a forward-looking note in the spec that applies only if/when a restore feature is built, at which point it must initialize `lastCelebratedMilestone` to `milestones.lastOrNull { it <= restoredCount } ?: 0`, not `0`.

**Testing note (deviation from spec's Testing section):** the spec calls for a dedicated Room migration test "in the same style as the existing `MIGRATION_1_2` test." That test does not actually exist in this codebase — there is no `androidTest` source set and no `room-testing` dependency in `app/build.gradle`, for `MIGRATION_1_2` or anything else. Adding that infrastructure from scratch is out of scope for this plan (it would be new project-wide test tooling, not a Faza 15-sized change). Similarly, there are zero Compose UI tests anywhere in this project (no `createComposeRule` usage), so `DailyChallengeCard`'s UI change is verified by build success + manual run, consistent with how the rest of the Daily Challenge UI shipped in Faza 14. If you want migration/Compose UI test infra added, that should be its own follow-up plan.

---

### Task 1: Streak schema migration + milestone detection in repository

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/data/StreakEntity.kt`
- Modify: `app/src/main/java/uz/kidzone/app/data/KidZoneDatabase.kt`
- Modify: `app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt`
- Test: `app/src/test/java/uz/kidzone/app/data/DailyChallengeRepositoryTest.kt`

**Interfaces:**
- Produces: `StreakEntity(profileId: String, count: Int = 0, lastCompletedDate: String = "", lastCelebratedMilestone: Int = 0)` (new 4th field, used by Task 2's ViewModel tests via `repository.getStreak()`).
- Produces: `DailyChallengeRepository.markChallengeCompleted(profileId: String, gameId: String): Int?` (changed from `Unit` — Task 2's `DailyChallengeViewModel.onGameClosed` consumes this return value).

- [ ] **Step 1: Add the new column to `StreakEntity` and bump the DB version**

Edit `app/src/main/java/uz/kidzone/app/data/StreakEntity.kt`:

```kotlin
package uz.kidzone.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val profileId: String,
    @ColumnInfo(defaultValue = "0") val count: Int = 0,
    @ColumnInfo(defaultValue = "") val lastCompletedDate: String = "",  // "YYYY-MM-DD"
    @ColumnInfo(defaultValue = "0") val lastCelebratedMilestone: Int = 0,
)
```

Edit `app/src/main/java/uz/kidzone/app/data/KidZoneDatabase.kt` — bump `version` and add `MIGRATION_2_3`:

```kotlin
@Database(
    entities = [
        ProfileEntity::class,
        ProfileStatsEntity::class,
        DailyChallengeEntity::class,
        StreakEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class KidZoneDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun profileStatsDao(): ProfileStatsDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun streakDao(): StreakDao

    companion object {
        @Volatile private var instance: KidZoneDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_challenge` " +
                    "(`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                    "`gameId` TEXT NOT NULL, `gameTitle` TEXT NOT NULL, " +
                    "`completed` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `streak` " +
                    "(`profileId` TEXT NOT NULL, `count` INTEGER NOT NULL DEFAULT 0, " +
                    "`lastCompletedDate` TEXT NOT NULL DEFAULT '', PRIMARY KEY(`profileId`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `streak` ADD COLUMN `lastCelebratedMilestone` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): KidZoneDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KidZoneDatabase::class.java,
                    "kidzone.db",
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
            }
    }
}
```

- [ ] **Step 2: Write the failing milestone-detection tests**

Add to `app/src/test/java/uz/kidzone/app/data/DailyChallengeRepositoryTest.kt` (inside the `DailyChallengeRepositoryTest` class, after the existing tests):

```kotlin
    @Test
    fun `markChallengeCompleted returns 3 when streak crosses first milestone`() = runTest {
        streakDao.upsert(StreakEntity("p1", count = 2, lastCompletedDate = yesterday))
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"}]""")
        repo.getTodayChallenge("p1")
        val milestone = repo.markChallengeCompleted("p1", "memory")
        assertEquals(3, milestone)
        assertEquals(3, repo.getStreak("p1").lastCelebratedMilestone)
    }

    @Test
    fun `markChallengeCompleted returns 7 when streak crosses second milestone`() = runTest {
        streakDao.upsert(StreakEntity("p1", count = 6, lastCompletedDate = yesterday, lastCelebratedMilestone = 3))
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"}]""")
        repo.getTodayChallenge("p1")
        val milestone = repo.markChallengeCompleted("p1", "memory")
        assertEquals(7, milestone)
    }

    @Test
    fun `markChallengeCompleted returns null when no milestone is crossed`() = runTest {
        streakDao.upsert(StreakEntity("p1", count = 3, lastCompletedDate = yesterday, lastCelebratedMilestone = 3))
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"}]""")
        repo.getTodayChallenge("p1")
        val milestone = repo.markChallengeCompleted("p1", "memory")
        assertNull(milestone)
    }

    @Test
    fun `markChallengeCompleted resets lastCelebratedMilestone when streak breaks and restarts`() = runTest {
        streakDao.upsert(StreakEntity("p1", count = 10, lastCompletedDate = olderDate, lastCelebratedMilestone = 7))
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"}]""")
        repo.getTodayChallenge("p1")
        repo.markChallengeCompleted("p1", "memory")
        assertEquals(0, repo.getStreak("p1").lastCelebratedMilestone)
    }

    @Test
    fun `markChallengeCompleted returns null on repeat completion same day even after reaching a milestone`() = runTest {
        streakDao.upsert(StreakEntity("p1", count = 2, lastCompletedDate = yesterday))
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"}]""")
        repo.getTodayChallenge("p1")
        val first = repo.markChallengeCompleted("p1", "memory")
        val second = repo.markChallengeCompleted("p1", "memory")
        assertEquals(3, first)
        assertNull(second)
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.data.DailyChallengeRepositoryTest"`
Expected: FAIL — `markChallengeCompleted` still returns `Unit`, `lastCelebratedMilestone` doesn't exist on `StreakEntity.getStreak(...)` calls will not compile until Step 1's entity change is present (it is, from Step 1), but the new assertions (`assertEquals(3, milestone)`, `assertEquals(3, repo.getStreak("p1").lastCelebratedMilestone)`) fail because the repository doesn't compute or return milestones yet — `markChallengeCompleted` returns `Unit`, causing a compile error on `assertEquals(3, milestone)`. This is expected: the compiler error IS the failing state for this statically-typed step.

- [ ] **Step 4: Implement milestone detection in the repository**

Edit `app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt`, replacing `markChallengeCompleted` and `updateStreak`:

```kotlin
    private val milestones = listOf(3, 7, 14, 30)

    open suspend fun markChallengeCompleted(profileId: String, gameId: String): Int? {
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

- [ ] **Step 5: Run the tests to verify they pass**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.data.DailyChallengeRepositoryTest"`
Expected: PASS (all tests, including the pre-existing ones — `StreakEntity`'s new 4th field defaults to `0` so none of the old constructor calls break).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/data/StreakEntity.kt app/src/main/java/uz/kidzone/app/data/KidZoneDatabase.kt app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt app/src/test/java/uz/kidzone/app/data/DailyChallengeRepositoryTest.kt
git commit -m "feat(faza15): streak milestone detection in repository + MIGRATION_2_3"
```

---

### Task 2: ViewModel — one-shot `celebrateMilestone` state

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModel.kt`
- Test: `app/src/test/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModelTest.kt`

**Interfaces:**
- Consumes: `DailyChallengeRepository.markChallengeCompleted(profileId: String, gameId: String): Int?` (Task 1).
- Produces: `ChallengeState.celebrateMilestone: Int?` and `DailyChallengeViewModel.onCelebrationShown()` (Task 3's `DailyChallengeCard` and Task 4's `MainScreen` wiring consume both).

- [ ] **Step 1: Update the fake repository in the test file to match the new return type**

Edit `app/src/test/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModelTest.kt` — in `FakeDailyChallengeRepository`, replace the `markChallengeCompleted` override:

```kotlin
    var markCompletedCallCount = 0
    var milestoneToReturn: Int? = null

    override suspend fun markChallengeCompleted(profileId: String, gameId: String): Int? {
        markCompletedCallCount++
        return milestoneToReturn
    }
```

(This replaces the existing `override suspend fun markChallengeCompleted(profileId: String, gameId: String) { markCompletedCallCount++ }` block and its separate `markCompletedCallCount` declaration.)

- [ ] **Step 2: Write the failing ViewModel tests**

Add to the `DailyChallengeViewModelTest` class:

```kotlin
    @Test
    fun `onGameClosed sets celebrateMilestone when repository returns a milestone`() = runTest {
        fakeRepo.games = listOf(GameItem("memory", "Memory Match"))
        vm.onProfileChanged("p1")
        advanceUntilIdle()
        fakeRepo.milestoneToReturn = 7
        vm.onGameClosed("memory")
        advanceUntilIdle()
        assertEquals(7, vm.state.value.celebrateMilestone)
    }

    @Test
    fun `onGameClosed leaves celebrateMilestone null when repository returns null`() = runTest {
        fakeRepo.games = listOf(GameItem("memory", "Memory Match"))
        vm.onProfileChanged("p1")
        advanceUntilIdle()
        fakeRepo.milestoneToReturn = null
        vm.onGameClosed("memory")
        advanceUntilIdle()
        assertNull(vm.state.value.celebrateMilestone)
    }

    @Test
    fun `onCelebrationShown clears celebrateMilestone`() = runTest {
        fakeRepo.games = listOf(GameItem("memory", "Memory Match"))
        vm.onProfileChanged("p1")
        advanceUntilIdle()
        fakeRepo.milestoneToReturn = 3
        vm.onGameClosed("memory")
        advanceUntilIdle()
        vm.onCelebrationShown()
        assertNull(vm.state.value.celebrateMilestone)
    }

    @Test
    fun `onProfileChanged clears any pending celebration from the previous profile`() = runTest {
        fakeRepo.games = listOf(GameItem("memory", "Memory Match"))
        vm.onProfileChanged("p1")
        advanceUntilIdle()
        fakeRepo.milestoneToReturn = 3
        vm.onGameClosed("memory")
        advanceUntilIdle()
        assertEquals(3, vm.state.value.celebrateMilestone)

        vm.onProfileChanged("p2")
        advanceUntilIdle()
        assertNull(vm.state.value.celebrateMilestone)
    }
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.ui.viewmodel.DailyChallengeViewModelTest"`
Expected: FAIL — `ChallengeState` has no `celebrateMilestone` property and `DailyChallengeViewModel` has no `onCelebrationShown()` method yet (compile error, same static-typing caveat as Task 1).

- [ ] **Step 4: Implement the state and method**

Edit `app/src/main/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModel.kt`:

```kotlin
data class ChallengeState(
    val challenge: DailyChallengeEntity? = null,
    val streakCount: Int = 0,
    val isLoading: Boolean = true,
    val celebrateMilestone: Int? = null,
)
```

Replace `onGameClosed` and add `onCelebrationShown`:

```kotlin
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

(`onProfileChanged` → `loadChallenge` already replaces `_state` with a fresh `ChallengeState(...)` on every profile switch, so `celebrateMilestone` defaults back to `null` automatically — no extra guard needed.)

- [ ] **Step 5: Run the tests to verify they pass**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.ui.viewmodel.DailyChallengeViewModelTest"`
Expected: PASS (all tests, including pre-existing ones).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModel.kt app/src/test/java/uz/kidzone/app/ui/viewmodel/DailyChallengeViewModelTest.kt
git commit -m "feat(faza15): celebrateMilestone state + onCelebrationShown in DailyChallengeViewModel"
```

---

### Task 3: `DailyChallengeCard` — milestone celebration UI

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/screens/DailyChallengeCard.kt`

**Interfaces:**
- Consumes: `celebrateMilestone: Int?` and `onCelebrationShown: () -> Unit` (Task 2).
- Produces: `DailyChallengeCard(streakCount: Int, challenge: DailyChallengeEntity?, visible: Boolean, celebrateMilestone: Int?, onCelebrationShown: () -> Unit, onPlay: (gameId: String) -> Unit, modifier: Modifier = Modifier)` — Task 4's `MainScreen` call site must pass the two new arguments.

There is no Compose UI test harness in this project (see Global Constraints), so this task's "test cycle" is a build check plus a manual run — not an automated unit test.

- [ ] **Step 1: Add the milestone copy table and new parameters**

Edit `app/src/main/java/uz/kidzone/app/ui/screens/DailyChallengeCard.kt` — add imports:

```kotlin
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
```

Add, above the `DailyChallengeCard` function:

```kotlin
private val MILESTONE_COPY: Map<Int, Pair<String, String>> = mapOf(
    3 to ("🔥" to "3 kun ketma-ket! Ajoyib boshlanish!"),
    7 to ("🔥🔥" to "Bir hafta ketma-ket! Zo'r!"),
    14 to ("🔥🔥🔥" to "Ikki hafta ketma-ket!"),
    30 to ("🔥🔥🔥🔥" to "Bir oy ketma-ket! Sen chempion!"),
)
```

Change the function signature:

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
    AnimatedVisibility(visible = visible && challenge != null) {
```

(The rest of the `AnimatedVisibility` block stays as-is except for the `completed` branch below.)

- [ ] **Step 2: Show the milestone message in the `completed` branch**

Replace the `if (completed) { ... }` block's first `Row` with:

```kotlin
                if (completed) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val milestoneCopy = celebrateMilestone?.let { MILESTONE_COPY[it] }
                            if (milestoneCopy != null) {
                                Text(
                                    "${milestoneCopy.first} ${milestoneCopy.second}",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            } else {
                                Text(
                                    "🔥 $streakCount kun streak",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "✅ Bajarildi!",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            "Ertaga yangi vazifa kutilmoqda 🌟",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                } else {
```

(Everything else in the file — the `else` branch, closing braces — is unchanged.)

- [ ] **Step 3: Build to verify it compiles**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/screens/DailyChallengeCard.kt
git commit -m "feat(faza15): milestone celebration text in DailyChallengeCard"
```

---

### Task 4: Wire `celebrateMilestone` through `MainScreen`

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt:100-109`

**Interfaces:**
- Consumes: `challengeState.celebrateMilestone: Int?` (already available on `ChallengeState` from Task 2, and `challengeState` is already collected at `MainScreen.kt:78`) and `challengeViewModel::onCelebrationShown` (Task 2).

- [ ] **Step 1: Pass the new state/callback into `DailyChallengeCard`**

Edit `app/src/main/java/uz/kidzone/app/ui/MainScreen.kt`, the existing call (lines 100-109):

```kotlin
        DailyChallengeCard(
            streakCount = challengeState.streakCount,
            challenge = challengeState.challenge,
            visible = !uiState.inGame && !uiState.isLocked,
            celebrateMilestone = challengeState.celebrateMilestone,
            onCelebrationShown = challengeViewModel::onCelebrationShown,
            onPlay = { gameId ->
                webMgrRef.value?.evaluateJavascript(
                    "if(window.app){app.openGame(app.games.find(function(g){return g.id==='$gameId';}))||null}"
                )
            },
        )
```

- [ ] **Step 2: Build to verify it compiles**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run the full existing unit test suite as a regression check**

Run: `.\gradlew.bat :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (Faza 14 tests + the new Faza 15 tests from Tasks 1-2).

- [ ] **Step 4: Manual verification**

Run the app (see the `run` skill or `.\gradlew.bat :app:installDebug`), pick a profile, complete daily challenges on 3 consecutive simulated days (or adjust the device date) to cross the 3-day milestone, and confirm:
- The card shows "🔥 3 kun ketma-ket! Ajoyib boshlanish!" instead of the usual streak count immediately after completing the challenge.
- After ~2.5 seconds it reverts to the normal "✅ Bajarildi!" / streak-count display.
- The JS `BadgeManager` streak badges (Achievements grid) are unaffected.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/MainScreen.kt
git commit -m "feat(faza15): wire celebrateMilestone through MainScreen"
```
