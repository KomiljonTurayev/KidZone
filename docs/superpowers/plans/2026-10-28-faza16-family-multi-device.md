# Faza 16 — Multi-device & Family Account Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Spec:** `docs/superpowers/specs/2026-10-28-faza16-family-multi-device-design.md`

**Goal:** Profiles, time limits and the parent PIN stay the same on every device of a family, and a second parent can join with their own Google/Telegram account.

**Architecture:** Phase A fixes the existing per-account sync: profiles get `updatedAt` + `deleted` (Room 3→4), merge becomes last-write-wins on `updatedAt`, deletes become synced tombstones, and a Firestore snapshot listener applies remote changes while the app is open. Phase B keeps family data under the owner's `users/{ownerUid}/profiles`; membership lives in `families/{ownerUid}/members/{memberUid}`, written only by the backend (invite code → join), and the app and the play-time heartbeat resolve an *effective owner uid* (`familyOwner ?: uid`).

**Tech Stack:** Kotlin, Room 2.6.1, Firebase Firestore (client + Admin SDK in the Spring backend), JUnit4, `@firebase/rules-unit-testing` (`firestore-tests/`).

## Global Constraints

- Phase A ships on its own and must not depend on any Phase B code.
- Avatars stay local-only; `avatarPath` is never overwritten by a remote merge.
- Do not change the `stats/`, `playtime/`, `daily_challenges/` or streak sync paths in Phase A.
- Every new Firestore field must be added to `isValidProfileWrite` in `firestore.rules` with a type check, and covered by `firestore-tests/firestore.rules.test.js`.
- Family membership is written **only** by the backend. Client rules must deny all writes under `families/**`.
- Joining a family requires a signed-in account and passes the PIN gate (parent area), never from a child-facing screen.

---

## Phase A — One account, many devices

### Task A1: `updatedAt` and `deleted` on profiles (Room 3→4)

**Files:**
- Modify: `app/src/androidMain/kotlin/uz/kidzone/app/data/ProfileEntity.kt`
- Modify: `app/src/androidMain/kotlin/uz/kidzone/app/data/KidZoneDatabase.kt`
- Modify: `app/src/androidMain/kotlin/uz/kidzone/app/data/ProfileDao.kt`
- Test: `app/src/test/java/uz/kidzone/app/data/ProfileRepositoryTest.kt` (`FakeProfileDao`)

**Interfaces:**
- Produces: `ProfileEntity(..., createdAt: Long, updatedAt: Long = createdAt, deleted: Boolean = false)`
- Produces: `ProfileDao.getAll()`, `count()`, `getById()` ignore rows with `deleted = 1`; `getAllSync()` keeps returning everything (sync needs tombstones).

- [x] **Step 1:** Add the two fields with defaults so existing call sites compile unchanged.
- [x] **Step 2:** `MIGRATION_3_4`: `ALTER TABLE profiles ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0`, `ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0`, then `UPDATE profiles SET updatedAt = createdAt`. Bump `version = 4`, register the migration.
- [x] **Step 3:** Add `WHERE deleted = 0` to the UI-facing queries; update `FakeProfileDao` the same way.
- [x] **Step 4:** Run `./gradlew :app:testDebugUnitTest` — existing profile tests still pass.

### Task A2: Last-write-wins merge

**Files:**
- Modify: `app/src/androidMain/kotlin/uz/kidzone/app/data/ProfileDao.kt` (`mergeProfiles`)
- Test: `ProfileRepositoryTest`

- [x] **Step 1 (test first):**
  - remote with a newer `updatedAt` replaces the local name/limit/PIN;
  - remote with an older `updatedAt` is ignored;
  - the local `avatarPath` survives a remote replace;
  - an unknown remote tombstone is stored.
- [x] **Step 2:** Change the condition from `remote.createdAt > local.createdAt` to `remote.updatedAt > local.updatedAt`.
- [x] **Step 3:** Tests pass.

### Task A3: Stamp every local change; deletes become tombstones

**Files:**
- Modify: `app/src/androidMain/kotlin/uz/kidzone/app/data/ProfileRepository.kt`
- Modify: `app/src/androidMain/kotlin/uz/kidzone/app/data/ProfileSyncManager.kt`
- Modify: `app/src/androidMain/kotlin/uz/kidzone/app/ui/viewmodel/ProfileViewModel.kt` (only if `deleteProfile` reads deleted rows)
- Test: `ProfileRepositoryTest`, `ProfileViewModelTest`

**Interfaces:**
- Produces: `ProfileRepository(..., nowMs: () -> Long = System::currentTimeMillis)`
- Produces: `pushProfile` also writes `updatedAt`, `deleted` and `serverUpdatedAt = FieldValue.serverTimestamp()`; `pullProfiles` reads `updatedAt` (fallback `createdAt`) and `deleted` (fallback `false`).

- [x] **Step 1 (test first):**
  - `insert`/`update` set `updatedAt = nowMs()` and push;
  - `delete` stores `deleted = true` with a fresh `updatedAt`, pushes it, and the profile disappears from `profiles`;
  - `setParentPin` stamps every profile it changes.
- [x] **Step 2:** Implement. `delete` = `update(profile.copy(deleted = true))`.
- [x] **Step 3:** Keep the "last live profile can't be deleted" rule (`ProfileViewModel` / dashboard).
- [x] **Step 4:** Tests pass.

### Task A4: Firestore rules for the new fields

**Files:**
- Modify: `firestore.rules` (`isValidProfileWrite`)
- Test: `firestore-tests/firestore.rules.test.js`

- [x] **Step 1 (test first):** owner may write `updatedAt` (int), `deleted` (bool), `serverUpdatedAt` (timestamp); wrong types are rejected; another user still can't write.
- [x] **Step 2:** Extend `hasOnly([...])` and add the type checks.
- [x] **Step 3:** Run `npm test` in `firestore-tests/` against the emulator — all tests pass (63 existing + new).

### Task A5: Live updates while the app is open

**Files:**
- Modify: `app/src/androidMain/kotlin/uz/kidzone/app/auth/AccountListeners.kt` (`ProfileCloudSyncListener`)
- Modify: `app/src/androidMain/kotlin/uz/kidzone/app/data/ProfileSyncManager.kt` (`listenProfiles(uid, onChange): ListenerRegistration?`)
- Test: unit test for the listener's attach/detach on account changes (fake sync manager)

- [x] **Step 1 (test first):** signing in attaches exactly one listener; switching account detaches the old one first; signing out / guest detaches it; snapshots with `hasPendingWrites` are skipped.
- [x] **Step 2:** Implement with `addSnapshotListener`; each snapshot → `profileDao.mergeProfiles(...)`.
- [x] **Step 3:** Keep the existing one-off pull + push on sign-in (covers the first sync before the listener fires).

### Task A6: Manual verification and release notes

- [ ] Two emulators, one Google account. Change name / time limit / PIN, and delete a profile on one emulator. Each change shows on the other **without restarting**.
- [ ] Upgrade test: install the current `master` build, create profiles, upgrade to this build. Profiles survive (migration 3→4) and sync.
- [ ] Note in the release changelog (uz/ru/en): "Changes now reach your other phones instantly".

---

## Phase B — Family members (second parent)

> Start only after Phase A is released and the spec's open questions are answered.

### Task B1: Backend — family model and invite codes

**Files (Kidzone_Backend):**
- Create: `domain/Family.java`, `domain/FamilyMember.java`, `domain/FamilyInvite.java`
- Create: `repository/FamilyRepository.java` + `repository/firestore/FirestoreFamilyRepository.java`
- Create: `service/FamilyService.java`, `controller/FamilyController.java`
- Test: `service/FamilyServiceTest.java`, `controller/FamilyControllerTest.java`

- [ ] **Step 1 (tests first):**
  - an invite is a 6-digit code that expires in 24h;
  - joining works once;
  - expired or used codes fail;
  - an owner can't join their own family;
  - more than 10 attempts per hour per uid are rejected;
  - leave / remove works;
  - deleting the owner's account clears every member's `familyOwner`.
- [ ] **Step 2:** Endpoints: `POST /family/invite`, `POST /family/join {code}`, `POST /family/leave`, `DELETE /family/members/{uid}`, `GET /family` (members list for the dashboard).
- [ ] **Step 3:** Joining writes `families/{owner}/members/{member}` and `users/{member}.familyOwner`.

### Task B2: Backend — heartbeat uses the effective owner

**Files:** `service/PlaytimeService.java`, `repository/firestore/FirestoreUserRepository.java`
- [ ] Test first: a member's heartbeat for a family profile reads the limit from, and writes play time under, `users/{owner}/profiles/{id}`.
- [ ] Implement: resolve `familyOwner ?: uid` once per request.

### Task B3: Rules — members can use the owner's profiles

**Files:** `firestore.rules`, `firestore-tests/firestore.rules.test.js`
- [ ] Test first:
  - a member can read/write `users/{owner}/profiles/**`;
  - a non-member can't;
  - a member can't read or modify `users/{owner}` itself, e.g. `status`;
  - clients can't write `families/**`.
- [ ] Add `isFamilyMember(owner)` = `exists(/databases/$(database)/documents/families/$(owner)/members/$(request.auth.uid))` to the profile, stats and challenge rules.

### Task B4: App — effective owner everywhere

**Files:** `auth/AccountListeners.kt`, `data/ProfileSyncManager.kt`, `FirestoreSync.kt` (streak / challenge paths), `PlaytimeApiClient.kt` (no change if the backend resolves it)
- [ ] Read `users/{uid}.familyOwner` after sign-in. Expose `effectiveOwnerUid` as state (unit-tested).
- [ ] Every `users/{uid}/profiles` path uses `effectiveOwnerUid`.

### Task B5: App — Family section in the parent dashboard (uz/ru/en)

**Files:** `ui/screens/ParentDashboardScreen.kt`, new `ui/screens/FamilyScreen.kt`, `ui/viewmodel/FamilyViewModel.kt`, `BackendClient.kt`
- [ ] Owner sees: the members list, "Invite a family member" (big 6-digit code with expiry), and remove member.
- [ ] Member sees the owner's name and a "Leave family" action.
- [ ] Not in a family: "Join a family" with a code entry (`KidNumberPad`).
- [ ] All texts go through `tr(uz, ru, en)`. Errors (wrong / expired code, too many attempts) are localized.

### Task B6: End-to-end check

- [ ] Two emulators, two different Google accounts:
  - invite and join;
  - edit a PIN or limit on either side and see it on both;
  - remove the member: their app falls back to their own (empty) profiles.
- [ ] Deploy order: backend (B1–B2), then rules (B3), then app (B4–B5).
