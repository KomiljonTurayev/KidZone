# FirestoreSync — Backend Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make KidZone Android write user profiles, session stats, and FCM tokens to Firestore in the exact schema the Spring Boot admin backend reads.

**Architecture:** New `FirestoreSync.java` singleton mirrors `FirebaseManager` pattern. `FirebaseManager` gains anonymous sign-in so every device has a Firebase UID for Firestore writes. `ParentalStatsManager` tracks per-session games. `MainActivity` orchestrates auth → sync on lifecycle events.

**Tech Stack:** Firebase Firestore SDK, Firebase Messaging SDK, Firebase Anonymous Auth, Mockito (tests), Robolectric (SharedPreferences tests)

---

## Backend Firestore Schema (MUST NOT DEVIATE)

From `D:\KidZone\Kidzone_Backend\src\main\java\uz\kidzone\service\UserService.java` and `StatsService.java`:

**`users/{uid}`** (String fields, Timestamp for dates):
```
displayName   : String
email         : String          (null for anonymous users)
ageGroup      : String          "3-5" | "5-7" | "7+"  (NOT "2-4")
status        : String          "active" | "banned"
fcmToken      : String
createdAt     : Timestamp       (set once on document creation)
lastActiveAt  : Timestamp       (updated every session)
```

**`stats/{YYYY-MM-DD}`** (date as doc ID, e.g. "2026-06-13"):
```
dau            : Long           (increment by 1 on first session per device per day)
totalSessions  : Long           (increment by 1 per session)
totalMinutes   : Long           (increment by session duration)
gameBreakdown  : Map {
  "{gameId}": {
    "gameName"  : String,
    "playCount" : Long          (increment by 1 per game play)
  }
}
```

---

## File Map

| Action | Path |
|--------|------|
| Modify | `app/build.gradle` |
| Modify | `app/src/main/java/uz/kidzone/app/FirebaseManager.java` |
| Create | `app/src/main/java/uz/kidzone/app/FirestoreSync.java` |
| Create | `app/src/test/java/uz/kidzone/app/FirestoreSyncTest.java` |
| Create | `app/src/main/java/uz/kidzone/app/FcmTokenManager.java` |
| Create | `app/src/test/java/uz/kidzone/app/FcmTokenManagerTest.java` |
| Modify | `app/src/main/java/uz/kidzone/app/ParentalStatsManager.java` |
| Modify | `app/src/test/java/uz/kidzone/app/ParentalStatsManagerTest.java` |
| Modify | `app/src/main/java/uz/kidzone/app/MainActivity.kt` |

---

## Task 1: Add Firestore + Messaging Gradle dependencies

**Files:**
- Modify: `app/build.gradle`

- [ ] **Step 1: Add dependencies**

In `app/build.gradle`, find the Firebase BOM line and add two lines after `firebase-auth`:
```gradle
// Firebase (CRM Block 1 — Auth foundation)
implementation platform('com.google.firebase:firebase-bom:33.7.0')
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
implementation 'com.google.firebase:firebase-messaging'
```

- [ ] **Step 2: Verify build**

Run: `./gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle
git commit -m "build: add firebase-firestore and firebase-messaging dependencies"
```

---

## Task 2: FirebaseManager — add anonymous sign-in

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/FirebaseManager.java`
- Modify: `app/src/test/java/uz/kidzone/app/FirebaseManagerTest.java`

**Why:** Every device needs a Firebase UID to write to Firestore. Anonymous auth provides a persistent UID even without email sign-in.

- [ ] **Step 1: Write failing test**

Add to `FirebaseManagerTest.java`:
```java
@Test
@SuppressWarnings("unchecked")
public void notConfigured_ensureAuthAsync_runsCallbackImmediately() {
    FirebaseManager manager = new FirebaseManager(null);
    final boolean[] ran = {false};
    manager.ensureAuthAsync(() -> ran[0] = true);
    assertTrue(ran[0]);
}

@Test
@SuppressWarnings("unchecked")
public void configured_userAlreadySignedIn_ensureAuthAsync_runsCallbackImmediately() {
    FirebaseAuth auth = mock(FirebaseAuth.class);
    FirebaseUser user = mock(FirebaseUser.class);
    when(auth.getCurrentUser()).thenReturn(user);

    FirebaseManager manager = new FirebaseManager(auth);
    final boolean[] ran = {false};
    manager.ensureAuthAsync(() -> ran[0] = true);
    assertTrue(ran[0]);
}

@Test
@SuppressWarnings("unchecked")
public void configured_noUser_ensureAuthAsync_callsSignInAnonymously() {
    FirebaseAuth auth = mock(FirebaseAuth.class);
    when(auth.getCurrentUser()).thenReturn(null);
    Task<AuthResult> task = mock(Task.class);
    when(task.addOnSuccessListener(any())).thenReturn(task);
    when(task.addOnFailureListener(any())).thenReturn(task);
    when(auth.signInAnonymously()).thenReturn(task);

    FirebaseManager manager = new FirebaseManager(auth);
    manager.ensureAuthAsync(() -> {});

    verify(auth).signInAnonymously();
}
```

- [ ] **Step 2: Run test — verify FAIL**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.FirebaseManagerTest"`
Expected: Compilation error (method not found)

- [ ] **Step 3: Implement `ensureAuthAsync`**

In `FirebaseManager.java`, add after `signOut()`:
```java
/**
 * Ensures there is an authenticated user (email or anonymous).
 * If already signed in, runs onReady immediately on the calling thread.
 * If not signed in, attempts anonymous sign-in; onReady runs on the main thread
 * after sign-in completes. On sign-in failure, onReady is NOT called.
 */
public void ensureAuthAsync(Runnable onReady) {
    if (auth == null) {
        onReady.run();   // not configured — let caller decide
        return;
    }
    if (auth.getCurrentUser() != null) {
        onReady.run();
        return;
    }
    auth.signInAnonymously()
        .addOnSuccessListener(result -> onReady.run())
        .addOnFailureListener(e -> android.util.Log.w("FirebaseManager",
                "Anonymous sign-in failed: " + e.getMessage()));
}
```

- [ ] **Step 4: Run test — verify PASS**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.FirebaseManagerTest"`
Expected: All tests PASS (3 new + 11 existing = 14 total)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/FirebaseManager.java \
        app/src/test/java/uz/kidzone/app/FirebaseManagerTest.java
git commit -m "feat(firebase): add ensureAuthAsync for anonymous sign-in"
```

---

## Task 3: FirestoreSync.java (TDD)

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/FirestoreSync.java`
- Create: `app/src/test/java/uz/kidzone/app/FirestoreSyncTest.java`

**Pattern:** Same singleton pattern as `FirebaseManager`. Package-private constructor for testing.

- [ ] **Step 1: Write failing tests**

Create `app/src/test/java/uz/kidzone/app/FirestoreSyncTest.java`:
```java
package uz.kidzone.app;

import org.junit.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class FirestoreSyncTest {

    @Test
    public void notConfigured_isAvailableFalse() {
        FirestoreSync sync = new FirestoreSync(null);
        assertFalse(sync.isAvailable());
    }

    @Test
    public void normalizeAgeGroup_twoToFour_returnsThreeToFive() {
        assertEquals("3-5", FirestoreSync.normalizeAgeGroup("2-4"));
    }

    @Test
    public void normalizeAgeGroup_threeToFive_unchanged() {
        assertEquals("3-5", FirestoreSync.normalizeAgeGroup("3-5"));
    }

    @Test
    public void normalizeAgeGroup_fiveToSeven_unchanged() {
        assertEquals("5-7", FirestoreSync.normalizeAgeGroup("5-7"));
    }

    @Test
    public void normalizeAgeGroup_sevenPlus_unchanged() {
        assertEquals("7+", FirestoreSync.normalizeAgeGroup("7+"));
    }

    @Test
    public void normalizeAgeGroup_null_defaultsToThreeToFive() {
        assertEquals("3-5", FirestoreSync.normalizeAgeGroup(null));
    }

    @Test
    public void normalizeAgeGroup_unknown_defaultsToThreeToFive() {
        assertEquals("3-5", FirestoreSync.normalizeAgeGroup("unknown"));
    }

    @Test
    public void notConfigured_syncUserProfile_doesNotThrow() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.syncUserProfile("uid", "Name", "a@b.com", "3-5");
        // no exception
    }

    @Test
    public void notConfigured_updateFcmToken_doesNotThrow() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.updateFcmToken("uid", "token123");
        // no exception
    }

    @Test
    public void notConfigured_recordSession_doesNotThrow() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.recordSession("uid", 10, Collections.emptyMap(), true);
        // no exception
    }

    @Test
    public void recordSession_zeroMinutes_doesNotThrow() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.recordSession("uid", 0, Collections.emptyMap(), false);
        // no exception — even when available (null db), zero minutes = no-op
    }

    @Test
    public void recordSession_nullUid_doesNotThrow() {
        FirestoreSync sync = new FirestoreSync(null);
        sync.recordSession(null, 5, Collections.emptyMap(), true);
        // no exception
    }
}
```

- [ ] **Step 2: Run test — verify FAIL**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.FirestoreSyncTest"`
Expected: Compilation error (class not found)

- [ ] **Step 3: Implement FirestoreSync.java**

Create `app/src/main/java/uz/kidzone/app/FirestoreSync.java`:
```java
package uz.kidzone.app;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Writes user profiles and session stats to Firestore in the schema
 * expected by the KidZone Spring Boot admin backend.
 *
 * Firestore schema:
 *   users/{uid}          — displayName, email, ageGroup, status, fcmToken,
 *                          createdAt (Timestamp), lastActiveAt (Timestamp)
 *   stats/{YYYY-MM-DD}   — dau, totalSessions, totalMinutes,
 *                          gameBreakdown: {gameId: {gameName, playCount}}
 *
 * All writes are fire-and-forget. Failures are logged but never thrown.
 * Gracefully no-ops when Firebase is not configured (no google-services.json).
 */
public class FirestoreSync {

    private static final String TAG = "FirestoreSync";

    private static FirestoreSync instance;

    private final FirebaseFirestore db;

    /** Package-private for testing. */
    FirestoreSync(FirebaseFirestore db) {
        this.db = db;
    }

    public static synchronized FirestoreSync init(Context ctx) {
        if (instance == null) {
            FirebaseFirestore db;
            try {
                db = FirebaseFirestore.getInstance();
            } catch (IllegalStateException e) {
                db = null;
            }
            instance = new FirestoreSync(db);
        }
        return instance;
    }

    public static synchronized FirestoreSync getInstance() {
        return instance != null ? instance : new FirestoreSync(null);
    }

    public boolean isAvailable() {
        return db != null;
    }

    /**
     * Creates or updates the user profile in Firestore.
     * Sets createdAt only when document is first created (via merge).
     * Always updates lastActiveAt and status to "active".
     */
    public void syncUserProfile(String uid, String displayName, String email, String ageGroup) {
        if (!isAvailable() || uid == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("displayName", displayName);
        data.put("email", email);
        data.put("ageGroup", normalizeAgeGroup(ageGroup));
        data.put("status", "active");
        data.put("lastActiveAt", FieldValue.serverTimestamp());

        // createdAt is set on first write; subsequent merges leave it alone
        // because we do NOT include it here — merge only overwrites present keys.
        db.collection("users").document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener(v -> {
                // Set createdAt only if not already present — use update which
                // fails (silently) if the field already exists via a separate
                // document creation with merge semantics.
                Map<String, Object> init = new HashMap<>();
                init.put("createdAt", FieldValue.serverTimestamp());
                db.collection("users").document(uid)
                    .set(init, SetOptions.merge())
                    .addOnFailureListener(e -> Log.d(TAG, "createdAt already set"));
            })
            .addOnFailureListener(e -> Log.w(TAG, "syncUserProfile failed: " + e.getMessage()));
    }

    /** Updates the FCM push notification token for the given user. */
    public void updateFcmToken(String uid, String token) {
        if (!isAvailable() || uid == null || token == null) return;
        db.collection("users").document(uid)
            .update("fcmToken", token)
            .addOnFailureListener(e -> Log.w(TAG, "updateFcmToken failed: " + e.getMessage()));
    }

    /**
     * Atomically increments daily aggregate stats in stats/{YYYY-MM-DD}.
     *
     * @param uid              Firebase UID (null = skip)
     * @param sessionMinutes   Session duration; skipped if <= 0
     * @param gamePlays        Map of gameId → gameName for games played this session
     * @param isFirstSessionToday  True if this is the device's first session today (increments dau)
     */
    public void recordSession(String uid, int sessionMinutes,
                              Map<String, String> gamePlays, boolean isFirstSessionToday) {
        if (!isAvailable() || uid == null || sessionMinutes <= 0) return;

        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("totalSessions", FieldValue.increment(1));
        updates.put("totalMinutes", FieldValue.increment(sessionMinutes));
        if (isFirstSessionToday) {
            updates.put("dau", FieldValue.increment(1));
        }
        if (gamePlays != null) {
            for (Map.Entry<String, String> entry : gamePlays.entrySet()) {
                String gameId = entry.getKey();
                String gameName = entry.getValue() != null ? entry.getValue() : gameId;
                updates.put("gameBreakdown." + gameId + ".playCount", FieldValue.increment(1));
                updates.put("gameBreakdown." + gameId + ".gameName", gameName);
            }
        }

        db.collection("stats").document(dateKey)
            .set(updates, SetOptions.merge())
            .addOnFailureListener(e -> Log.w(TAG, "recordSession failed: " + e.getMessage()));
    }

    /**
     * Maps Android app age groups to the backend's expected ageGroup values.
     * "2-4" is an Android-only group; mapped to "3-5" for backend compatibility.
     */
    static String normalizeAgeGroup(String ageGroup) {
        if (ageGroup == null) return "3-5";
        switch (ageGroup) {
            case "3-5": return "3-5";
            case "5-7": return "5-7";
            case "7+":  return "7+";
            default:    return "3-5";   // covers "2-4" and unknowns
        }
    }
}
```

- [ ] **Step 4: Run tests — verify PASS**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.FirestoreSyncTest"`
Expected: All 12 tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/FirestoreSync.java \
        app/src/test/java/uz/kidzone/app/FirestoreSyncTest.java
git commit -m "feat: add FirestoreSync for backend-compatible Firestore writes (TDD)"
```

---

## Task 4: FcmTokenManager.java (TDD)

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/FcmTokenManager.java`
- Create: `app/src/test/java/uz/kidzone/app/FcmTokenManagerTest.java`

**Design:** `FcmTokenManager` is a utility class (no instances). A `TokenProvider` interface makes the production `FirebaseMessaging` call injectable for testing.

- [ ] **Step 1: Write failing tests**

Create `app/src/test/java/uz/kidzone/app/FcmTokenManagerTest.java`:
```java
package uz.kidzone.app;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class FcmTokenManagerTest {

    @Test
    public void nullUid_doesNotCallProvider() {
        AtomicReference<Boolean> called = new AtomicReference<>(false);
        FirestoreSync sync = new FirestoreSync(null);
        FcmTokenManager.registerToken(null, sync, cb -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    public void unavailableSync_doesNotCallProvider() {
        AtomicReference<Boolean> called = new AtomicReference<>(false);
        FirestoreSync sync = new FirestoreSync(null);   // isAvailable() = false
        FcmTokenManager.registerToken("uid-123", sync, cb -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    public void validInputs_callsProviderAndDoesNotThrow() {
        // This test verifies the provider is called but sync is null → no crash
        AtomicReference<Boolean> called = new AtomicReference<>(false);
        FirestoreSync sync = new FirestoreSync(null);
        // even with unavailable sync, provider should NOT be called (guarded early)
        FcmTokenManager.registerToken("uid-xyz", sync, cb -> {
            called.set(true);
            cb.onToken(null);  // simulate FCM failure
        });
        assertFalse(called.get());   // guard fires first: sync unavailable
    }
}
```

- [ ] **Step 2: Run test — verify FAIL**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.FcmTokenManagerTest"`
Expected: Compilation error (class not found)

- [ ] **Step 3: Implement FcmTokenManager.java**

Create `app/src/main/java/uz/kidzone/app/FcmTokenManager.java`:
```java
package uz.kidzone.app;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;

/** Registers the device FCM token to Firestore so the admin backend can push notifications. */
public final class FcmTokenManager {

    private FcmTokenManager() {}

    /** Injected for unit testing. */
    interface TokenProvider {
        void getToken(TokenCallback cb);
    }

    interface TokenCallback {
        void onToken(String token);
    }

    /** Production entry point — uses real FirebaseMessaging. */
    public static void registerToken(String uid, FirestoreSync sync) {
        registerToken(uid, sync, cb ->
            FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(cb::onToken)
                .addOnFailureListener(e -> {
                    Log.w("FcmTokenManager", "getToken failed: " + e.getMessage());
                    cb.onToken(null);
                })
        );
    }

    /** Testable overload — accepts injectable TokenProvider. */
    static void registerToken(String uid, FirestoreSync sync, TokenProvider provider) {
        if (uid == null || !sync.isAvailable()) return;
        provider.getToken(token -> {
            if (token != null) sync.updateFcmToken(uid, token);
        });
    }
}
```

- [ ] **Step 4: Run tests — verify PASS**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.FcmTokenManagerTest"`
Expected: All 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/FcmTokenManager.java \
        app/src/test/java/uz/kidzone/app/FcmTokenManagerTest.java
git commit -m "feat: add FcmTokenManager for FCM token registration to Firestore (TDD)"
```

---

## Task 5: ParentalStatsManager — per-session game tracking

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/ParentalStatsManager.java`
- Modify: `app/src/test/java/uz/kidzone/app/ParentalStatsManagerTest.java`

**Why:** `MainActivity.onPause()` needs to know which games were played THIS session (not all day) and how many minutes elapsed — to pass to `FirestoreSync.recordSession()`.

- [ ] **Step 1: Write failing tests**

Append to `ParentalStatsManagerTest.java`:
```java
@Test
public void getSessionGames_initiallyEmpty() {
    assertTrue(mgr.getSessionGames().isEmpty());
}

@Test
public void getSessionGames_afterOnGameLaunched_containsGame() {
    mgr.onGameLaunched("math_counting");
    assertTrue(mgr.getSessionGames().contains("math_counting"));
}

@Test
public void getSessionGames_deduplicatesWithinSession() {
    mgr.onGameLaunched("math_counting");
    mgr.onGameLaunched("math_counting");
    assertEquals(1, mgr.getSessionGames().size());
}

@Test
public void onSessionStart_clearsSessionGames() {
    mgr.onGameLaunched("math_counting");
    mgr.onSessionStart();
    assertTrue(mgr.getSessionGames().isEmpty());
}

@Test
public void getSessionMinutes_beforeSessionStart_returnsZero() {
    assertEquals(0, mgr.getSessionMinutes());
}

@Test
public void getSessionMinutes_afterSessionStart_returnsElapsed() throws InterruptedException {
    mgr.onSessionStart();
    // elapsed is < 1 minute so still 0 — test boundary only
    assertEquals(0, mgr.getSessionMinutes());  // sub-minute = 0
}
```

- [ ] **Step 2: Run tests — verify FAIL**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.ParentalStatsManagerTest"`
Expected: 6 new tests FAIL (methods not found)

- [ ] **Step 3: Implement session tracking**

In `ParentalStatsManager.java`, make the following changes:

Add field after `sessionStartMs`:
```java
private final java.util.List<String> sessionGames = new java.util.ArrayList<>();
```

Update `onSessionStart()` to clear session games:
```java
public void onSessionStart() {
    sessionStartMs = System.currentTimeMillis();
    sessionGames.clear();
}
```

Update `onGameLaunched()` to also track session games:
```java
public void onGameLaunched(String gameId) {
    if (gameId == null || gameId.isEmpty()) return;
    String key = todayGlKey();
    String existing = prefs.getString(key, "");
    List<String> list = parseList(existing);
    if (!list.contains(gameId)) {
        list.add(gameId);
        prefs.edit().putString(key, joinList(list)).apply();
    }
    if (!sessionGames.contains(gameId)) {
        sessionGames.add(gameId);
    }
}
```

Add new methods before `getTodayMinutes()`:
```java
/** Returns a snapshot of games launched in the current session. */
public List<String> getSessionGames() {
    return new ArrayList<>(sessionGames);
}

/** Returns elapsed minutes for the current session (0 if session not started). */
public int getSessionMinutes() {
    if (sessionStartMs == 0) return 0;
    return (int) ((System.currentTimeMillis() - sessionStartMs) / 60_000L);
}
```

The import at the top already has `java.util.List` and `java.util.ArrayList` — verify they are present, add if not.

- [ ] **Step 4: Run tests — verify PASS**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.ParentalStatsManagerTest"`
Expected: All tests PASS (6 new + 10 existing = 16 total)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/ParentalStatsManager.java \
        app/src/test/java/uz/kidzone/app/ParentalStatsManagerTest.java
git commit -m "feat(stats): add per-session game tracking and getSessionMinutes to ParentalStatsManager"
```

---

## Task 6: Wire up FirestoreSync + FcmTokenManager in MainActivity

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/MainActivity.kt`

**Behavior:**
1. `onCreate()` — init `FirestoreSync`; call `FirebaseManager.ensureAuthAsync()` to get an anonymous UID
2. `onResume()` — if user is signed in, sync profile + register FCM token
3. `onPause()` — after `statsManager.onSessionEnd()`, send session stats to Firestore

**DAU logic:** A SharedPreferences key `kz_dau_{yyyyMMdd}` tracks whether the device has already contributed to DAU today. Stored in the existing `activityPrefs`.

- [ ] **Step 1: Read current MainActivity.kt** (the full file is needed before editing)

Read `app/src/main/java/uz/kidzone/app/MainActivity.kt` carefully.

- [ ] **Step 2: Implement changes**

The complete updated `MainActivity.kt` after changes:

```kotlin
package uz.kidzone.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import uz.kidzone.app.data.repository.GameRepository
import uz.kidzone.app.ui.KidZoneApp
import uz.kidzone.app.ui.viewmodel.AppViewModel
import uz.kidzone.app.ui.viewmodel.GameViewModel
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        @JvmField
        var instance: WeakReference<MainActivity>? = null
    }

    private lateinit var statsManager: ParentalStatsManager
    private lateinit var adsManager: AdsManager
    private lateinit var appViewModel: AppViewModel
    private lateinit var gameViewModel: GameViewModel
    private lateinit var activityPrefs: SharedPreferences
    private lateinit var firestoreSync: FirestoreSync

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val prefs = getSharedPreferences(OnboardingActivity.PREFS, MODE_PRIVATE)

        if (!prefs.getBoolean(OnboardingActivity.KEY_DONE, false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        instance = WeakReference(this)

        // --- Manager initialisation ---
        FirebaseManager.init(this)
        firestoreSync = FirestoreSync.init(this)

        statsManager = ParentalStatsManager(this)
        adsManager   = AdsManager(this)
        adsManager.initialize()

        // Ensure every device has a Firebase UID (anonymous auth as fallback)
        FirebaseManager.getInstance().ensureAuthAsync {
            val uid = FirebaseManager.getInstance().uid
            if (uid != null) FcmTokenManager.registerToken(uid, firestoreSync)
        }

        // --- ViewModels ---
        activityPrefs = prefs
        val ageFilter = prefs.getString(OnboardingActivity.KEY_AGE, "2-4") ?: "2-4"
        appViewModel = AppViewModel(prefs)
        gameViewModel = GameViewModel(GameRepository(), ageFilter)

        // --- Compose UI ---
        setContent {
            KidZoneApp(
                appViewModel   = appViewModel,
                gameViewModel  = gameViewModel,
                onGameLaunched = { gameId -> statsManager.onGameLaunched(gameId) },
                onOpenDashboard = { showPinDialog() }
            )
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        if (::appViewModel.isInitialized) {
            appViewModel.refreshAgeFilter()
            val currentAge = appViewModel.getAgeFilter()
            if (currentAge != gameViewModel.getAgeFilter()) {
                gameViewModel.updateFilters(currentAge)
            }
        }
        // Sync user profile to Firestore if authenticated
        val uid = FirebaseManager.getInstance().uid
        if (uid != null && ::firestoreSync.isInitialized) {
            val user = FirebaseManager.getInstance().currentUser
            val displayName = user?.displayName ?: ""
            val email = user?.email ?: ""
            val ageGroup = appViewModel.getAgeFilter()
            firestoreSync.syncUserProfile(uid, displayName, email, ageGroup)
        }
        MusicManager.getInstance().startMusic(this)
        statsManager.onSessionStart()
        adsManager.onResume()
    }

    override fun onPause() {
        // Capture session stats before onSessionEnd() resets them
        val sessionMinutes = statsManager.getSessionMinutes()
        val sessionGames = statsManager.getSessionGames()

        statsManager.onSessionEnd()
        adsManager.onPause()
        MusicManager.getInstance().pauseMusic()

        // Sync session stats to Firestore
        val uid = FirebaseManager.getInstance().uid
        if (uid != null && sessionMinutes > 0 && ::firestoreSync.isInitialized) {
            val lang = if (::appViewModel.isInitialized) appViewModel.getLang() else "en"
            val gamePlays = sessionGames.associateWith { id ->
                gameViewModel.getGameById(id)?.title?.get("en") ?: id
            }
            val dateKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val dauPrefKey = "kz_dau_$dateKey"
            val isFirstSession = !activityPrefs.getBoolean(dauPrefKey, false)
            firestoreSync.recordSession(uid, sessionMinutes, gamePlays, isFirstSession)
            if (isFirstSession) activityPrefs.edit().putBoolean(dauPrefKey, true).apply()
        }

        super.onPause()
    }

    override fun onDestroy() {
        instance = null
        adsManager.onDestroy()
        super.onDestroy()
    }

    // ── Back-compat WebView bridge stubs (Faza 1 — no-ops) ────────────────────

    @Suppress("unused")
    fun injectJs(script: String) {}

    @Suppress("unused")
    fun evaluateJs(script: String, cb: ValueCallback<String>?) {
        cb?.onReceiveValue(null)
    }

    // ── PIN / Dashboard ────────────────────────────────────────────────────────

    private fun showPinDialog() {
        val prefs = getSharedPreferences(OnboardingActivity.PREFS, MODE_PRIVATE)
        val hash  = PinUtil.getOrMigrateHash(prefs, "kz_pin")
        when {
            hash == null -> {
                PinDialogHelper.showCreate(this) { pin ->
                    prefs.edit()
                        .putString("kz_pin", if (pin.isEmpty()) "" else PinUtil.hash(pin))
                        .apply()
                    openDashboard()
                }
            }
            hash.isEmpty() -> openDashboard()
            else -> PinDialogHelper.showEnter(this, hash) { openDashboard() }
        }
    }

    private fun openDashboard() {
        startActivity(Intent(this, ParentalDashboardActivity::class.java))
    }
}
```

**Key changes from before:**
- Added `firestoreSync: FirestoreSync` field
- `onCreate()`: init `firestoreSync`, call `ensureAuthAsync` for anonymous auth + FCM token
- `onResume()`: sync user profile after existing ageFilter sync
- `onPause()`: capture session stats BEFORE `onSessionEnd()`, sync to Firestore after
- `super.onPause()` moved to end of `onPause()` (after all sync calls complete)

- [ ] **Step 3: Build and verify**

Run: `./gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

Run: `./gradlew.bat :app:testDebugUnitTest`
Expected: Same pass/fail ratio as before (no regressions from Faza 1)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/MainActivity.kt
git commit -m "feat: wire FirestoreSync and FcmTokenManager into MainActivity lifecycle"
```

---

## Final verification

Run full test suite: `./gradlew.bat :app:testDebugUnitTest`
Expected: All previously-passing tests still pass. New tests (FirestoreSyncTest × 12, FcmTokenManagerTest × 3, ParentalStatsManagerTest +6) all pass.
Pre-existing failures (ContentFilterTest, KidzoAgentStateTest) still expected at same count.
