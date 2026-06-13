# FCM Push Notifications — Design Spec

**Date:** 2026-06-13
**Scope:** CRM Block 2 — FCM push notification handling on Android + `createdAt` first-write fix in FirestoreSync.

---

## Maqsad

Admin panel (`POST /admin/push/send`) orqali yuborilgan push notificationlarni Android qurilmada ko'rsatish. Foreground va background da system notification bar orqali ko'rsatiladi. Qo'shimcha: `FirestoreSync.syncUserProfile()` da `createdAt` birinchi yozuvda o'rnatiladi.

---

## Arxitektura

```
App Start
    └── KidZoneApplication.onCreate()
            └── Notification channel yaratiladi ("kidzone_push")

MainActivity.onCreate()
    └── FirebaseManager.ensureAuthAsync()
            └── FcmTokenManager.registerToken(uid, sync)
                    ├── FirebaseMessaging.subscribeToTopic("all-users")
                    └── FirebaseMessaging.getToken() → Firestore.updateFcmToken()

FCM xabar kelganda
    └── KidZoneFirebaseMessagingService.onMessageReceived()
            └── buildNotification(title, body) → NotificationManager.notify()

Token yangilanganda
    └── KidZoneFirebaseMessagingService.onNewToken()
            └── FirestoreSync.getInstance().updateFcmToken(uid, newToken)

FirestoreSync.syncUserProfile() — createdAt fix
    └── docRef.get()
            ├── doc yo'q → data.put("createdAt", serverTimestamp())
            └── doc bor → createdAt skip
            └── docRef.set(data, SetOptions.merge())
```

---

## Yangi va o'zgartirilgan fayllar

| Harakat | Fayl | Nima |
|---------|------|------|
| Yaratish | `app/src/main/java/uz/kidzone/app/KidZoneApplication.java` | Notification channel init |
| Yaratish | `app/src/main/java/uz/kidzone/app/KidZoneFirebaseMessagingService.java` | FCM xabar qabul qilish |
| Yaratish | `app/src/test/java/uz/kidzone/app/KidZoneFirebaseMessagingServiceTest.java` | TDD testlar |
| O'zgartirish | `app/src/main/java/uz/kidzone/app/FcmTokenManager.java` | `subscribeToTopic` qo'shish |
| O'zgartirish | `app/src/test/java/uz/kidzone/app/FcmTokenManagerTest.java` | Topic subscription testi |
| O'zgartirish | `app/src/main/java/uz/kidzone/app/FirestoreSync.java` | `createdAt` first-write logikasi |
| O'zgartirish | `app/src/test/java/uz/kidzone/app/FirestoreSyncTest.java` | `createdAt` testlari |
| O'zgartirish | `app/src/main/AndroidManifest.xml` | `android:name` + service entry |

---

## KidZoneApplication

```java
public class KidZoneApplication extends Application {
    static final String CHANNEL_ID = "kidzone_push";

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
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
```

---

## KidZoneFirebaseMessagingService

```java
public class KidZoneFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String title = message.getNotification() != null
            ? message.getNotification().getTitle() : "";
        String body = message.getNotification() != null
            ? message.getNotification().getBody() : "";
        // data payload fallback
        if (title == null || title.isEmpty()) title = message.getData().get("title");
        if (body  == null || body.isEmpty())  body  = message.getData().get("body");

        showNotification(title != null ? title : "KidZone",
                         body  != null ? body  : "");
    }

    @Override
    public void onNewToken(String token) {
        String uid = FirebaseManager.getInstance().getUid();
        if (uid != null) {
            FirestoreSync.getInstance().updateFcmToken(uid, token);
        }
    }

    void showNotification(String title, String body) {
        Notification n = buildNotification(title, body);
        NotificationManagerCompat.from(this).notify(1001, n);
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

---

## FcmTokenManager — o'zgarishlar

`registerToken()` ichida token olishdan oldin `all-users` topicga subscribe bo'linadi:

```java
static void registerToken(String uid, FirestoreSync sync, TokenProvider provider) {
    if (uid == null || !sync.isAvailable()) return;
    FirebaseMessaging.getInstance().subscribeToTopic("all-users")
        .addOnFailureListener(e -> Log.w("FcmTokenManager", "topic sub failed: " + e));
    provider.getToken(token -> {
        if (token != null) sync.updateFcmToken(uid, token);
    });
}
```

---

## FirestoreSync.syncUserProfile() — createdAt fix

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
    ref.get().addOnSuccessListener(snap -> {
        if (!snap.exists() || !snap.contains("createdAt")) {
            data.put("createdAt", FieldValue.serverTimestamp());
        }
        ref.set(data, SetOptions.merge())
           .addOnFailureListener(e -> Log.w(TAG, "syncUserProfile failed: " + e));
    }).addOnFailureListener(e -> {
        // get() xatosi: createdAt skip, boshqa maydonlar yoziladi
        ref.set(data, SetOptions.merge())
           .addOnFailureListener(e2 -> Log.w(TAG, "syncUserProfile failed: " + e2));
    });
}
```

---

## AndroidManifest.xml

```xml
<application
    android:name=".KidZoneApplication"
    ...>

    <service
        android:name=".KidZoneFirebaseMessagingService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT"/>
        </intent-filter>
    </service>
```

---

## TDD Testlar

### KidZoneFirebaseMessagingServiceTest

| Test | Nima tekshiriladi |
|------|-------------------|
| `buildNotification_withTitleAndBody_setsCorrectFields` | title, body, channelId to'g'ri |
| `buildNotification_emptyTitle_usesDefault` | bo'sh title → "KidZone" default |
| `buildNotification_nullBody_usesEmpty` | null body → "" |

### FcmTokenManagerTest (qo'shimcha)

| Test | Nima tekshiriladi |
|------|-------------------|
| `registerToken_validUidAndAvailableSync_callsProvider` | provider chaqiriladi (stub sync) |

### FirestoreSyncTest (qo'shimcha)

`syncUserProfile` Firestore calllarini mock qilmasdan test qilish murakkab. Shuning uchun:
- `notConfigured_syncUserProfile_doesNotThrow` — allaqachon bor ✓
- Yangi: `syncUserProfile_nullEmail_usesEmpty` — null email → "" string
- Yangi: `syncUserProfile_nullDisplayName_usesEmpty` — null displayName → "" string

---

## Error handling

| Holat | Yechim |
|-------|--------|
| Topic subscription xatosi | Log.w, silent fail |
| `onNewToken` da uid null | Skip, keyingi session'da yangilanadi |
| `createdAt` get() xatosi | Silent fail, merge yozish davom etadi |
| Notification permission yo'q (Android 13+) | Tizim boshqaradi, biz so'ramaymiz |
| Notification channel allaqachon bor | `createNotificationChannel` idempotent |

---

## Bog'liqliklar

- Firebase Messaging SDK — `firebase-messaging` allaqachon `build.gradle`da bor ✓
- `NotificationCompat` — `androidx.core` allaqachon bor ✓
- Backend `POST /admin/push/send` — mavjud ✓
- `all-users` topic — backend broadcast shu topicga yuboradi ✓
