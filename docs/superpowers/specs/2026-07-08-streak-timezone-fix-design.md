# KidZone Android — Streak "Today" Timezone Fix Design Spec
**Date:** 2026-07-08
**Status:** Approved
**Type:** Bugfix (not a numbered Faza — cross-repo consistency fix)
**Related:** backend `StreakReminderService` (`D:\KidZone\Kidzone_Backend`), Faza 14 (`2026-07-01-faza14-daily-challenge-streak-design.md`)

---

## 1. Problem

The backend's `StreakReminderService` (Faza 15/16, repo `kidzone`) always computes "today" in a fixed zone:

```java
LocalDate.now(ZoneId.of("Asia/Tashkent"))
```

and compares it against each profile's `streak.lastCompletedDate` field in Firestore to decide whether a user is at-risk of losing their streak.

The Android app writes that same `lastCompletedDate` field, but computes "today" using the **device's local timezone**, with no explicit zone, in two places:

- `data/DailyChallengeRepository.kt:16` — the `todayProvider` default, used when writing `lastCompletedDate`, incrementing the streak count, and syncing to Firestore (`FirestoreSync.syncStreak`).
- `ui/screens/ParentDashboardScreen.kt:248` — an independent `LocalDate.now()` call used only to compute the "done today" checkmark shown in the parent dashboard.

Because these two zone bases can disagree (any device not set to Asia/Tashkent, most sharply right around each side's local midnight), the app can write a `lastCompletedDate` that doesn't match what the backend considers "today" — silently causing the backend job to either skip a reminder for an actually-at-risk streak, or fire one for a user who already completed their challenge by their own device's clock. The parent dashboard's own "done today" indicator can also disagree with what was actually just written, for the same reason.

Field names and Firestore paths (`users/{uid}/profiles/{profileId}`, `streak.count`, `streak.lastCompletedDate`) already match exactly between the two repos — this is purely a timezone-basis bug, not a schema mismatch.

---

## 2. Decision

Fix Android to always compute the streak's "today" in a fixed `Asia/Tashkent` zone, matching the backend. This is a backend-app-wide business decision already made implicitly by the backend (its job schedule is also fixed to `Asia/Tashkent` — see `kidzone` commit `f5e6a37`), so aligning Android to it requires no backend change and no new data model (no per-user timezone field, no migration).

No backfill of existing `lastCompletedDate` values: the field is overwritten on every challenge completion, so any past mismatch self-corrects on the user's next play session. The only historical impact was a possible one-off incorrect at-risk push around a timezone boundary, which isn't worth a migration.

---

## 3. Changes

### 3.1 New helper — `data/AppClock.kt`

Single source of truth for the app's business-day timezone, replacing the two independent `LocalDate.now()` call sites:

```kotlin
package uz.kidzone.app.data

import java.time.LocalDate
import java.time.ZoneId

object AppClock {
    val ZONE: ZoneId = ZoneId.of("Asia/Tashkent")
    fun today(): String = LocalDate.now(ZONE).toString()
}
```

### 3.2 `DailyChallengeRepository.kt`

Default `todayProvider` (line 15-17) changes from:

```kotlin
private val todayProvider: () -> String = {
    java.time.LocalDate.now().toString()
},
```

to:

```kotlin
private val todayProvider: () -> String = {
    AppClock.today()
},
```

No other logic in the file changes — the yesterday/streak-continuation comparison (`updateStreak`, lines 59-67) already operates purely on whatever `today` string it's given, so it's correct regardless of zone as long as the zone is consistent.

### 3.3 `ParentDashboardScreen.kt`

Line 248 changes from:

```kotlin
val todayDate = java.time.LocalDate.now().toString()
```

to:

```kotlin
val todayDate = AppClock.today()
```

so the "done today" UI check uses the same zone basis as what `DailyChallengeRepository` writes.

---

## 4. Testing

- Existing tests (`DailyChallengeRepositoryTest.kt:30`, `DailyChallengeViewModelTest.kt:96`) already inject a fixed `todayProvider` lambda and are unaffected by this change.
- Add a small unit test for `AppClock.today()` asserting it uses `ZoneId.of("Asia/Tashkent")` (e.g. compare against `LocalDate.now(ZoneId.of("Asia/Tashkent"))` directly, not device-local `LocalDate.now()`).
- `ParentDashboardScreen.kt`'s "done today" computation has no existing test seam (it's inline Compose code, not routed through a testable clock injection point) — matches current coverage level; not adding new test infrastructure for it as part of this fix.

---

## 5. Out of Scope

- Per-user or per-device configurable timezones.
- DST handling (Asia/Tashkent does not observe DST).
- Any change to the backend's job schedule, `isAtRisk` predicate, or Firestore schema.
- Backfilling existing `lastCompletedDate` values.

---

## 6. Files Changed / Created

| File | Repo | Action |
|------|------|--------|
| `app/src/main/java/uz/kidzone/app/data/AppClock.kt` | KidZone (Android) | Create |
| `app/src/main/java/uz/kidzone/app/data/DailyChallengeRepository.kt` | KidZone (Android) | Modify — `todayProvider` default uses `AppClock.today()` |
| `app/src/main/java/uz/kidzone/app/ui/screens/ParentDashboardScreen.kt` | KidZone (Android) | Modify — line 248 uses `AppClock.today()` |
| `app/src/test/java/uz/kidzone/app/data/AppClockTest.kt` | KidZone (Android) | Create — asserts fixed-zone behavior |
