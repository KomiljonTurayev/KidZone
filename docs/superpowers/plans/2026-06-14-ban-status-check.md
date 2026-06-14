# Ban Status Check Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** App ochilganda Firebase UID bo'yicha Firestore'dan `status` tekshiriladi; `"banned"` bo'lsa onboarding prefs tozalanib `OnboardingActivity`'ga yo'naltiriladi.

**Architecture:** `BanChecker.java` — `FcmTokenManager` pattern'iga mos injectable utility sinf. Production'da Firestore orqali tekshiradi; testlarda `DocProvider` stub ishlatiladi. `MainActivity.onCreate()` da `ensureAuthAsync` callback'iga qo'shiladi.

**Tech Stack:** Firebase Firestore (allaqachon bor), JUnit 4 (allaqachon bor)

---

## File Map

| Harakat | Fayl | Vazifa |
|---------|------|--------|
| Yaratish | `app/src/main/java/uz/kidzone/app/BanChecker.java` | Ban tekshiruv utility, injectable |
| Yaratish | `app/src/test/java/uz/kidzone/app/BanCheckerTest.java` | 7 ta TDD test |
| O'zgartirish | `app/src/main/java/uz/kidzone/app/MainActivity.java` | `checkAsync` + `banUser()` |

---

## Task 1: BanChecker.java (TDD)

**Files:**
- Create: `app/src/test/java/uz/kidzone/app/BanCheckerTest.java`
- Create: `app/src/main/java/uz/kidzone/app/BanChecker.java`

- [ ] **Step 1: Failing testlar yozish**

`app/src/test/java/uz/kidzone/app/BanCheckerTest.java` yaratish:

```java
package uz.kidzone.app;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class BanCheckerTest {

    // --- Null / unavailable guards ---

    @Test
    public void nullUid_returnsError() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        BanChecker.checkAsync(null, (uid, cb) -> cb.onDoc(true, "active"), result::set);
        assertEquals(BanChecker.Status.ERROR, result.get());
    }

    @Test
    public void unavailableSync_returnsError() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        FirestoreSync sync = new FirestoreSync(null); // isAvailable() = false
        BanChecker.checkAsync("uid-123", sync, result::set);
        assertEquals(BanChecker.Status.ERROR, result.get());
    }

    // --- DocProvider stub tests ---

    @Test
    public void docNotExists_returnsActive() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        BanChecker.checkAsync("uid", (uid, cb) -> cb.onDoc(false, null), result::set);
        assertEquals(BanChecker.Status.ACTIVE, result.get());
    }

    @Test
    public void statusActive_returnsActive() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        BanChecker.checkAsync("uid", (uid, cb) -> cb.onDoc(true, "active"), result::set);
        assertEquals(BanChecker.Status.ACTIVE, result.get());
    }

    @Test
    public void statusBanned_returnsBanned() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        BanChecker.checkAsync("uid", (uid, cb) -> cb.onDoc(true, "banned"), result::set);
        assertEquals(BanChecker.Status.BANNED, result.get());
    }

    @Test
    public void statusMissing_returnsActive() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        BanChecker.checkAsync("uid", (uid, cb) -> cb.onDoc(true, null), result::set);
        assertEquals(BanChecker.Status.ACTIVE, result.get());
    }

    @Test
    public void unknownStatus_returnsActive() {
        AtomicReference<BanChecker.Status> result = new AtomicReference<>();
        BanChecker.checkAsync("uid", (uid, cb) -> cb.onDoc(true, "suspended"), result::set);
        assertEquals(BanChecker.Status.ACTIVE, result.get());
    }
}
```

- [ ] **Step 2: Testni ishga tushirish — FAIL bo'lishini tekshirish**

```bash
./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.BanCheckerTest"
```

Expected: Compilation error — `BanChecker` class not found

- [ ] **Step 3: BanChecker.java yaratish**

`app/src/main/java/uz/kidzone/app/BanChecker.java` yaratish:

```java
package uz.kidzone.app;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;

/** Checks if a user is banned by reading users/{uid}.status from Firestore. */
public final class BanChecker {

    private static final String TAG = "BanChecker";

    public enum Status { ACTIVE, BANNED, ERROR }

    public interface Callback {
        void onResult(Status status);
    }

    interface DocProvider {
        void getDoc(String uid, DocCallback cb);
    }

    interface DocCallback {
        void onDoc(boolean exists, String status);
        default void onError() {}
    }

    /** Production entry point — reads from Firestore. */
    public static void checkAsync(String uid, FirestoreSync sync, Callback cb) {
        if (uid == null || !sync.isAvailable()) {
            cb.onResult(Status.ERROR);
            return;
        }
        checkAsync(uid, (u, docCb) ->
            sync.getDb().collection("users").document(u).get()
                .addOnSuccessListener(snap -> {
                    String status = snap.exists() ? snap.getString("status") : null;
                    docCb.onDoc(snap.exists(), status);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "ban check failed: " + e.getMessage());
                    docCb.onError();
                }),
            cb
        );
    }

    /** Package-private — injectable DocProvider for unit testing. */
    static void checkAsync(String uid, DocProvider provider, Callback cb) {
        if (uid == null) {
            cb.onResult(Status.ERROR);
            return;
        }
        provider.getDoc(uid, new DocCallback() {
            @Override
            public void onDoc(boolean exists, String status) {
                if (!exists || status == null) {
                    cb.onResult(Status.ACTIVE);
                } else if ("banned".equals(status)) {
                    cb.onResult(Status.BANNED);
                } else {
                    cb.onResult(Status.ACTIVE);
                }
            }

            @Override
            public void onError() {
                cb.onResult(Status.ERROR);
            }
        });
    }
}
```

**Eslatma:** `sync.getDb()` mavjud emas — `FirestoreSync`'ga package-private `getDb()` metodi qo'shish kerak (keyingi stepda).

- [ ] **Step 4: FirestoreSync.java ga `getDb()` qo'shish**

`app/src/main/java/uz/kidzone/app/FirestoreSync.java` ichida `isAvailable()` dan keyin qo'shish:

```java
/** Package-private for BanChecker — returns underlying Firestore instance. */
FirebaseFirestore getDb() {
    return db;
}
```

- [ ] **Step 5: Testlarni ishga tushirish — PASS bo'lishini tekshirish**

```bash
./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.BanCheckerTest"
```

Expected: 7 ta test PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/BanChecker.java \
        app/src/main/java/uz/kidzone/app/FirestoreSync.java \
        app/src/test/java/uz/kidzone/app/BanCheckerTest.java
git commit -m "feat: add BanChecker for Firestore ban status check (TDD)"
```

---

## Task 2: MainActivity — wire BanChecker

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/MainActivity.java`

Mavjud barcha funksionallik o'zgarishsiz qoladi. Faqat `ensureAuthAsync` callback'i va yangi `banUser()` metodi qo'shiladi.

- [ ] **Step 1: `ensureAuthAsync` callback'ini yangilash**

`app/src/main/java/uz/kidzone/app/MainActivity.java` da quyidagi qatorni:

```java
        FirebaseManager.getInstance().ensureAuthAsync(() -> {
            String uid = FirebaseManager.getInstance().getUid();
            if (uid != null) FcmTokenManager.registerToken(uid, firestoreSync);
        });
```

Quyidagi bilan almashtirish:

```java
        FirebaseManager.getInstance().ensureAuthAsync(() -> {
            String uid = FirebaseManager.getInstance().getUid();
            if (uid != null) {
                FcmTokenManager.registerToken(uid, firestoreSync);
                BanChecker.checkAsync(uid, firestoreSync, status -> {
                    if (status == BanChecker.Status.BANNED) {
                        runOnUiThread(this::banUser);
                    }
                });
            }
        });
```

**Eslatma:** `BanChecker.checkAsync` Firestore callback'i background thread'da kelishi mumkin — `runOnUiThread` bilan UI thread'da chaqiriladi.

- [ ] **Step 2: `banUser()` metodini qo'shish**

`MainActivity.java` da `setupKidzoFab()` metodidan oldin qo'shish:

```java
    private void banUser() {
        if (isFinishing()) return;
        kzPrefs.edit().putBoolean(OnboardingActivity.KEY_DONE, false).apply();
        startActivity(new android.content.Intent(this, OnboardingActivity.class));
        finish();
    }
```

- [ ] **Step 3: Build tekshirish**

```bash
./gradlew.bat :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/MainActivity.java
git commit -m "feat: wire BanChecker into MainActivity — redirect banned users to onboarding"
```

---

## Task 3: To'liq tekshirish

- [ ] **Step 1: Barcha testlarni ishga tushirish**

```bash
./gradlew.bat :app:testDebugUnitTest
```

Expected yangi test soni:
| Suite | Tests |
|-------|-------|
| `BanCheckerTest` | 7 |
| `FirestoreSyncTest` | 14 |
| `KidZoneFirebaseMessagingServiceTest` | 3 |
| `FcmTokenManagerTest` | 3 |
| `FirebaseManagerTest` | 11 |
| `ParentalStatsManagerTest` | 17 |
| `PinUtilTest` | 14 |

Pre-existing failures (ContentFilterTest × 4, KidzoAgentStateTest × 2) — o'zgarishsiz.

- [ ] **Step 2: Push**

```bash
git push
```
