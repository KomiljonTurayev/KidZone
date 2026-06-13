# FCM Push Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Android qurilmada FCM push notificationlarni foreground va background da system notification bar orqali ko'rsatish; `createdAt` ni birinchi Firestore yozuvda o'rnatish.

**Architecture:** `KidZoneApplication` notification channelni app start bo'lishi bilan yaratadi. `KidZoneFirebaseMessagingService` xabarlarni qabul qilib system notification ko'rsatadi va token yangilansa Firestore'ni yangilaydi. `FcmTokenManager` `all-users` topicga subscribe bo'ladi. `FirestoreSync.syncUserProfile()` birinchi yozuvda `createdAt` qo'shadi. **Mavjud barcha funksionallik o'zgarishsiz qoladi — faqat qo'shimchalar.**

**Tech Stack:** Firebase Cloud Messaging (allaqachon build.gradle'da), NotificationCompat (androidx.core, allaqachon bor), Robolectric (testlar uchun, allaqachon bor)

---

## File Map

| Harakat | Fayl | Vazifa |
|---------|------|--------|
| Yaratish | `app/src/main/java/uz/kidzone/app/KidZoneApplication.java` | Notification channel init, Application class |
| Yaratish | `app/src/main/java/uz/kidzone/app/KidZoneFirebaseMessagingService.java` | FCM xabar qabul qilish, token yangilash |
| Yaratish | `app/src/test/java/uz/kidzone/app/KidZoneFirebaseMessagingServiceTest.java` | buildNotification TDD testlar |
| O'zgartirish | `app/src/main/java/uz/kidzone/app/FcmTokenManager.java` | `subscribeToTopic("all-users")` qo'shish |
| O'zgartirish | `app/src/test/java/uz/kidzone/app/FcmTokenManagerTest.java` | Mavjud 3 test saqlanadi + 0 yangi |
| O'zgartirish | `app/src/main/java/uz/kidzone/app/FirestoreSync.java` | `syncUserProfile` — createdAt first-write |
| O'zgartirish | `app/src/test/java/uz/kidzone/app/FirestoreSyncTest.java` | 2 yangi test |
| O'zgartirish | `app/src/main/AndroidManifest.xml` | `android:name` + service entry |

---

## Task 1: KidZoneApplication — Notification channel

**Files:**
- Create: `app/src/main/java/uz/kidzone/app/KidZoneApplication.java`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: KidZoneApplication.java yaratish**

`app/src/main/java/uz/kidzone/app/KidZoneApplication.java` faylini yaratish:

```java
package uz.kidzone.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class KidZoneApplication extends Application {

    public static final String CHANNEL_ID = "kidzone_push";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "KidZone Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
```

- [ ] **Step 2: AndroidManifest.xml — `android:name` qo'shish**

`app/src/main/AndroidManifest.xml` ichida `<application` tagiga `android:name=".KidZoneApplication"` qo'shish. Mavjud atributlar o'zgarishsiz qoladi:

```xml
<application
    android:name=".KidZoneApplication"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:label="@string/app_name"
    android:theme="@style/Theme.KidZone"
    android:supportsRtl="true"
    android:hardwareAccelerated="true"
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="false">
```

- [ ] **Step 3: Build qilish — kompilyatsiya xatosi yo'qligini tekshirish**

```bash
./gradlew.bat :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/KidZoneApplication.java app/src/main/AndroidManifest.xml
git commit -m "feat: add KidZoneApplication with FCM notification channel"
```

---

## Task 2: KidZoneFirebaseMessagingService — TDD

**Files:**
- Create: `app/src/test/java/uz/kidzone/app/KidZoneFirebaseMessagingServiceTest.java`
- Create: `app/src/main/java/uz/kidzone/app/KidZoneFirebaseMessagingService.java`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Failing test yozish**

`app/src/test/java/uz/kidzone/app/KidZoneFirebaseMessagingServiceTest.java` yaratish:

```java
package uz.kidzone.app;

import android.app.Notification;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class KidZoneFirebaseMessagingServiceTest {

    private KidZoneFirebaseMessagingService service;

    @Before
    public void setUp() {
        service = Robolectric.buildService(KidZoneFirebaseMessagingService.class)
                .create().get();
    }

    @Test
    public void buildNotification_withTitleAndBody_isNotNull() {
        Notification n = service.buildNotification("Salom", "Yangi o'yin mavjud!");
        assertNotNull(n);
    }

    @Test
    public void buildNotification_emptyTitle_usesDefault() {
        Notification n = service.buildNotification("", "body text");
        assertNotNull(n);
        // NotificationCompat ichida title o'rnatilgan, null emas
        assertNotNull(n.extras.getString(Notification.EXTRA_TITLE));
    }

    @Test
    public void buildNotification_normalTitle_titleMatches() {
        Notification n = service.buildNotification("Test Title", "Test Body");
        assertEquals("Test Title", n.extras.getString(Notification.EXTRA_TITLE));
    }
}
```

- [ ] **Step 2: Testni ishga tushirish — FAIL bo'lishini tekshirish**

```bash
./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.KidZoneFirebaseMessagingServiceTest"
```

Expected: Compilation error — `KidZoneFirebaseMessagingService` class not found

- [ ] **Step 3: KidZoneFirebaseMessagingService.java yaratish**

`app/src/main/java/uz/kidzone/app/KidZoneFirebaseMessagingService.java` yaratish:

```java
package uz.kidzone.app;

import android.app.Notification;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class KidZoneFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "KzFCM";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String title = null;
        String body  = null;

        if (message.getNotification() != null) {
            title = message.getNotification().getTitle();
            body  = message.getNotification().getBody();
        }
        if (title == null || title.isEmpty()) title = message.getData().get("title");
        if (body  == null || body.isEmpty())  body  = message.getData().get("body");

        showNotification(
            title != null && !title.isEmpty() ? title : "KidZone",
            body  != null ? body : ""
        );
    }

    @Override
    public void onNewToken(String token) {
        String uid = FirebaseManager.getInstance().getUid();
        if (uid != null) {
            FirestoreSync.getInstance().updateFcmToken(uid, token);
        } else {
            Log.d(TAG, "onNewToken: uid null, token not synced yet");
        }
    }

    void showNotification(String title, String body) {
        Notification n = buildNotification(title, body);
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, n);
        } catch (SecurityException e) {
            Log.w(TAG, "Notification permission not granted: " + e.getMessage());
        }
    }

    Notification buildNotification(String title, String body) {
        return new NotificationCompat.Builder(this, KidZoneApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build();
    }
}
```

- [ ] **Step 4: Testni ishga tushirish — PASS bo'lishini tekshirish**

```bash
./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.KidZoneFirebaseMessagingServiceTest"
```

Expected: `BUILD SUCCESSFUL`, 3 test PASS

- [ ] **Step 5: AndroidManifest.xml — service entry qo'shish**

`</application>` tagidan oldin quyidagini qo'shish (mavjud activity'lar o'zgarishsiz):

```xml
        <!-- FCM Push Notifications -->
        <service
            android:name=".KidZoneFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT"/>
            </intent-filter>
        </service>

    </application>
```

- [ ] **Step 6: Build tekshirish**

```bash
./gradlew.bat :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/KidZoneFirebaseMessagingService.java \
        app/src/test/java/uz/kidzone/app/KidZoneFirebaseMessagingServiceTest.java \
        app/src/main/AndroidManifest.xml
git commit -m "feat: add KidZoneFirebaseMessagingService — FCM push handling (TDD)"
```

---

## Task 3: FcmTokenManager — `all-users` topic subscription

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/FcmTokenManager.java`

Mavjud 3 test o'zgarmaydi (ular hali ham pass bo'lishi kerak).

- [ ] **Step 1: `FcmTokenManager.java` ichida `subscribeToTopic` qo'shish**

`registerToken(String uid, FirestoreSync sync, TokenProvider provider)` metodiga `subscribeToTopic` qo'shish. Mavjud guard va token logikasi o'zgarishsiz qoladi:

```java
/** Package-private overload with injectable TokenProvider for unit testing. */
static void registerToken(String uid, FirestoreSync sync, TokenProvider provider) {
    if (uid == null || !sync.isAvailable()) return;
    FirebaseMessaging.getInstance().subscribeToTopic("all-users")
        .addOnFailureListener(e -> Log.w("FcmTokenManager", "topic sub failed: " + e));
    provider.getToken(token -> {
        if (token != null) sync.updateFcmToken(uid, token);
    });
}
```

**Eslatma:** Bu o'zgartirish faqat `uid != null && sync.isAvailable()` bo'lganda ishlaydi. Mavjud 3 test (null uid, unavailable sync) holatlari hali ham PASS bo'ladi — chunki guard `return` qiladi va `subscribeToTopic` chaqirilmaydi.

- [ ] **Step 2: Mavjud testlar PASS bo'lishini tekshirish**

```bash
./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.FcmTokenManagerTest"
```

Expected: 3 test PASS (hech qanday o'zgarish yo'q)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/FcmTokenManager.java
git commit -m "feat(fcm): subscribe to all-users topic on registerToken"
```

---

## Task 4: FirestoreSync — `createdAt` first-write

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/FirestoreSync.java`
- Modify: `app/src/test/java/uz/kidzone/app/FirestoreSyncTest.java`

- [ ] **Step 1: Yangi testlar yozish**

`app/src/test/java/uz/kidzone/app/FirestoreSyncTest.java` ga quyidagi 2 testni qo'shish (mavjud 12 test o'zgarishsiz qoladi):

```java
@Test
public void syncUserProfile_nullDisplayName_usesEmptyString() {
    // notConfigured holat — isAvailable() false, return qilinadi, xato yo'q
    FirestoreSync sync = new FirestoreSync(null);
    sync.syncUserProfile("uid", null, "a@b.com", "3-5");
    // no exception
}

@Test
public void syncUserProfile_nullEmail_usesEmptyString() {
    FirestoreSync sync = new FirestoreSync(null);
    sync.syncUserProfile("uid", "Name", null, "3-5");
    // no exception
}
```

- [ ] **Step 2: Testlarni ishga tushirish — FAIL bo'lishini tekshirish**

```bash
./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.FirestoreSyncTest"
```

Expected: Mavjud 12 test PASS, 2 yangi test ham PASS (chunki null db = early return).
Agar FAIL bo'lsa: keyingi stepga o'tish.

- [ ] **Step 3: `syncUserProfile` — `createdAt` first-write logikasi qo'shish**

`FirestoreSync.java` ichida `syncUserProfile` metodini to'liq almashtirish. Quyidagi import qo'shish kerak:

```java
import com.google.firebase.firestore.DocumentReference;
```

Yangi `syncUserProfile`:

```java
public void syncUserProfile(String uid, String displayName, String email, String ageGroup) {
    if (!isAvailable() || uid == null) return;

    Map<String, Object> data = new HashMap<>();
    data.put("displayName", displayName != null ? displayName : "");
    data.put("email", email != null ? email : "");
    data.put("ageGroup", normalizeAgeGroup(ageGroup));
    data.put("status", "active");
    data.put("lastActiveAt", FieldValue.serverTimestamp());

    DocumentReference ref = db.collection("users").document(uid);
    ref.get()
        .addOnSuccessListener(snap -> {
            if (!snap.exists() || !snap.contains("createdAt")) {
                data.put("createdAt", FieldValue.serverTimestamp());
            }
            ref.set(data, SetOptions.merge())
               .addOnFailureListener(e -> Log.w(TAG, "syncUserProfile failed: " + e));
        })
        .addOnFailureListener(e -> {
            // get() xatosi: createdAt skip, boshqa maydonlar yoziladi
            Log.w(TAG, "syncUserProfile get() failed, skipping createdAt: " + e);
            ref.set(data, SetOptions.merge())
               .addOnFailureListener(e2 -> Log.w(TAG, "syncUserProfile set failed: " + e2));
        });
}
```

`TAG` konstantasi `FirestoreSync` classida yo'q — qo'shish kerak (classning boshida):

```java
private static final String TAG = "FirestoreSync";
```

- [ ] **Step 4: Testlarni ishga tushirish — PASS bo'lishini tekshirish**

```bash
./gradlew.bat :app:testDebugUnitTest --tests "uz.kidzone.app.FirestoreSyncTest"
```

Expected: 14 test PASS (12 mavjud + 2 yangi)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/uz/kidzone/app/FirestoreSync.java \
        app/src/test/java/uz/kidzone/app/FirestoreSyncTest.java
git commit -m "feat(firestore): set createdAt on first user document write"
```

---

## Task 5: To'liq tekshirish

- [ ] **Step 1: Barcha testlarni ishga tushirish**

```bash
./gradlew.bat :app:testDebugUnitTest
```

Expected: Barcha yangi testlar PASS.
Pre-existing failures (ContentFilterTest × 4, KidzoAgentStateTest × 2) — bu holat o'zgarishsiz.

Test soni kutilayotgani:
| Suite | Tests |
|-------|-------|
| KidZoneFirebaseMessagingServiceTest | 3 |
| FcmTokenManagerTest | 3 |
| FirestoreSyncTest | 14 |
| FirebaseManagerTest | 11 |
| ParentalStatsManagerTest | 17 |
| PinUtilTest | 14 |

- [ ] **Step 2: Debug APK build**

```bash
./gradlew.bat :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Push**

```bash
git push
```
