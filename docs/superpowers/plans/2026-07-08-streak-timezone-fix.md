# Streak "Today" Timezone Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Android app compute the streak's "today" in a fixed `Asia/Tashkent` zone instead of the device's local zone, so it matches the backend's `StreakReminderService` and stops silently disagreeing about which day a streak was completed on.

**Architecture:** Add one small object, `AppClock`, as the single source of truth for "today" in the app's business timezone. Point `DailyChallengeRepository`'s default `todayProvider` and `ParentDashboardScreen`'s inline "done today" check at it, replacing their two independent, zone-less `LocalDate.now()` calls.

**Tech Stack:** Kotlin, `java.time` (`LocalDate`, `ZoneId`), JUnit4 + `org.junit.Assert`, Jetpack Compose (no test changes there).

## Global Constraints

- Fixed timezone is `Asia/Tashkent` — matches backend `StreakReminderService.java:13,26` and the existing job schedule (`kidzone` commit `f5e6a37`). Do not make this configurable.
- No backend changes. No Firestore schema changes. No data migration/backfill of existing `lastCompletedDate` values.
- Existing tests inject a fixed `todayProvider` lambda (`DailyChallengeRepositoryTest.kt:30`, `DailyChallengeViewModelTest.kt:96`) and must keep passing unmodified.
- Module namespace is `uz.kidzone.app`; unit tests run via `./gradlew :app:testDebugUnitTest`.

---

### Task 1: `AppClock` helper + test

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/data/AppClock.kt`
- Test: `app/src/test/java/uz/kidzone/app/data/AppClockTest.kt`

**Interfaces:**
- Produces: `object AppClock { val ZONE: java.time.ZoneId; fun today(): String }` — `Task 2` and `Task 3` both call `AppClock.today()`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/uz/kidzone/app/data/AppClockTest.kt`:

```kotlin
// app/src/test/java/uz/kidzone/app/data/AppClockTest.kt
package uz.kidzone.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AppClockTest {

    @Test
    fun `ZONE is Asia-Tashkent`() {
        assertEquals(ZoneId.of("Asia/Tashkent"), AppClock.ZONE)
    }

    @Test
    fun `today returns date in Asia-Tashkent zone`() {
        val expected = LocalDate.now(ZoneId.of("Asia/Tashkent")).toString()
        assertEquals(expected, AppClock.today())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "uz.kidzone.app.data.AppClockTest"`
Expected: FAIL — compilation error, `AppClock` is unresolved.

- [ ] **Step 3: Write minimal implementation**

Create `app/src/main/java/uz/kidzone/app/data/AppClock.kt`:

```kotlin
// app/src/main/java/uz/kidzone/app/data/AppClock.kt
package uz.kidzone.app.data

import java.time.LocalDate
import java.time.ZoneId

object AppClock {
    val ZONE: ZoneId = ZoneId.of("Asia/Tashkent")
    fun today(): String = LocalDate.now(ZONE).toString()
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "uz.kidzone.app.data.AppClockTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/data/AppClock.kt app/src/test/java/uz/kidzone/app/data/AppClockTest.kt
git commit -m "feat: add AppClock — fixed Asia/Tashkent 'today' for streak logic"
```

---

### Task 2: Wire `DailyChallengeRepository`'s default `todayProvider` to `AppClock`

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt:15-17`
- Test: `app/src/test/java/uz/kidzone/app/data/DailyChallengeRepositoryTest.kt`

**Interfaces:**
- Consumes: `AppClock.today(): String` (from Task 1).
- Produces: no change to `DailyChallengeRepository`'s public signatures — `todayProvider` stays a private constructor parameter with the same type `() -> String`, only its default value changes. Existing callers (production code, `DailyChallengeRepositoryTest`, `DailyChallengeViewModelTest`) are unaffected.

- [ ] **Step 1: Write the failing test**

Add to `app/src/test/java/uz/kidzone/app/data/DailyChallengeRepositoryTest.kt`, inside the `DailyChallengeRepositoryTest` class (after the existing `setUp` block, e.g. after line 32):

```kotlin
    @Test
    fun `default todayProvider uses AppClock`() = runTest {
        val repo = DailyChallengeRepository(
            challengeDao = FakeDailyChallengeDao(),
            streakDao = FakeStreakDao(),
            firestoreSync = NoOpFirestoreSync(),
        )
        repo.updateGamesList("""[{"id":"memory","title":"Memory Match"}]""")
        val result = repo.getTodayChallenge("p1")
        assertEquals(AppClock.today(), result!!.date)
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "uz.kidzone.app.data.DailyChallengeRepositoryTest"`
Expected: FAIL on the new test — `result!!.date` is `java.time.LocalDate.now().toString()` (device-local), which only coincidentally equals `AppClock.today()` when the test JVM's default zone happens to already be Asia/Tashkent. On a CI runner (default UTC) or any other non-Tashkent zone, this assertion fails, demonstrating the bug. (If your local JVM's default zone happens to be Asia/Tashkent, this step will pass early — proceed to Step 3 regardless, since the fix is still correct and the CI runner will genuinely fail without it.)

- [ ] **Step 3: Write minimal implementation**

In `app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt`, replace lines 15-17:

```kotlin
    private val todayProvider: () -> String = {
        java.time.LocalDate.now().toString()
    },
```

with:

```kotlin
    private val todayProvider: () -> String = {
        AppClock.today()
    },
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "uz.kidzone.app.data.DailyChallengeRepositoryTest"`
Expected: PASS (all tests in the class, including the new one and the 7 pre-existing ones)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt app/src/test/java/uz/kidzone/app/data/DailyChallengeRepositoryTest.kt
git commit -m "fix: DailyChallengeRepository uses AppClock (Asia/Tashkent) for today, not device-local zone"
```

---

### Task 3: `ParentDashboardScreen`'s "done today" check uses `AppClock`

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt:57` (imports), `:248`

**Interfaces:**
- Consumes: `AppClock.today(): String` (from Task 1).
- Produces: nothing new — this is a leaf UI computation, no other task depends on it.

No unit test seam exists for this file today (confirmed: no `ParentDashboardScreenTest.kt` in the repo, and the "done today" value is computed inline in a Compose `items` lambda, not exposed through any injectable clock or ViewModel). Per the design spec (§4), this task does not add new test infrastructure — verification is a compile/build check plus the manual QA note below, matching current coverage level for this file.

- [ ] **Step 1: Add the import**

In `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt`, after line 57 (`import uz.kidzone.app.data.ProfileEntity`), add:

```kotlin
import uz.kidzone.app.data.AppClock
```

- [ ] **Step 2: Replace the device-local computation**

Replace line 248:

```kotlin
                val todayDate = java.time.LocalDate.now().toString()
```

with:

```kotlin
                val todayDate = AppClock.today()
```

- [ ] **Step 3: Verify the module compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL, no unresolved reference errors.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt
git commit -m "fix: ParentDashboardScreen 'done today' check uses AppClock (Asia/Tashkent)"
```

---

## Manual QA (after Task 3, before considering this done)

Since `ParentDashboardScreen`'s change has no automated test, do a quick manual check per the run skill:
1. Set the emulator/device's system timezone to something other than Asia/Tashkent (e.g. UTC or America/Los_Angeles), in Android Settings → System → Date & time.
2. Complete a daily challenge for a profile.
3. Open the parent dashboard and confirm the profile's "done today" indicator is checked (matches what was just written), and that the streak count incremented as expected relative to the *previous* Tashkent-day completion, not the device's local day.
