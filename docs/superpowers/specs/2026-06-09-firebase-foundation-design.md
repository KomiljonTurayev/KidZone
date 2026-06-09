# Firebase Foundation (CRM Block 1) — Design Spec

> **Scope:** CRM tizimining birinchi bloki. Keyingi bloklar (Admin Panel, Push Notifications, Marketing Analytics) shu poydevonga quriladi.

**Maqsad:** KidZone ilovasiga Firebase Auth + Firestore qo'shish — ota-onalar cloud hisobi orqali bola profillari, badge'lar, o'yin statistikasi va sozlamalarini saqlaydi va qurilmalar o'rtasida sinxronlaydi.

**Arxitektura printsipi:** Local-first — `localStorage` birlamchi saqlovchi bo'lib qoladi. Firestore asinxron cloud backup/sync. Bolalar o'yini internet bo'lmasa ham uzilishsiz ishlaydi.

---

## Arxitektura

```
┌─────────────────────────────────────────┐
│              Android App                │
│                                         │
│  [localStorage / SharedPrefs]           │
│       ↑↓ (async sync)                   │
│  [FirestoreSync.java]                   │
│       ↑↓                                │
│  [FirebaseManager.java]                 │
│       ↑↓                                │
│  [Firebase Auth + Firestore]  (cloud)   │
└─────────────────────────────────────────┘
```

- `localStorage` — birlamchi saqlovchi (o'zgarishsiz qoladi)
- `FirebaseManager.java` — Auth init, sign-in (Email + Google), sign-out, UID boshqaruvi
- `FirestoreSync.java` — upload/download, offline queue, conflict resolution
- `ParentalDashboardActivity` — login tugmasi, sinxronizatsiya UI

---

## Yangi va o'zgaritiladigan fayllar

| Harakat | Fayl | Nima |
|---------|------|------|
| Create | `app/src/main/java/uz/kidzone/app/FirebaseManager.java` | Auth wrapper |
| Create | `app/src/main/java/uz/kidzone/app/FirestoreSync.java` | Sync engine |
| Modify | `app/build.gradle` | Firebase dependencies |
| Add | `app/google-services.json` | Firebase config (gitignore'da bo'lmaydi) |
| Modify | `app/src/main/java/uz/kidzone/app/MainActivity.java` | FirebaseManager.init() |
| Modify | `app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java` | Login UI + sync trigger |
| Modify | `app/src/main/res/values/strings.xml` | Yangi UI matnlar |

---

## Firestore Schema

```
users/{uid}/
  ├── email: string
  ├── createdAt: Timestamp
  ├── lang: "uz" | "ru" | "en"
  ├── pin: string          // SHA-256 hash
  ├── timeLimitMinutes: number
  ├── ageFilter: "3-5" | "5-7" | "7+"
  └── updatedAt: Timestamp

users/{uid}/profiles/{profileId}/
  ├── name: string
  ├── age: number
  ├── lang: string
  ├── stars: number
  ├── avatarIndex: number
  └── updatedAt: Timestamp

users/{uid}/profiles/{profileId}/badges/{badgeId}/
  └── earnedAt: Timestamp

users/{uid}/stats/{YYYY-MM-DD}/
  ├── totalMinutes: number
  ├── gameBreakdown: map<gameId, minutes>
  └── updatedAt: Timestamp
```

**Security Rules:** `users/{uid}` yo'li faqat `request.auth.uid == uid` bo'lganda read/write.

---

## Firebase Dependencies (build.gradle)

```groovy
// Firebase BoM — versiyalarni avtomatik boshqaradi
implementation platform('com.google.firebase:firebase-bom:33.x.x')
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
implementation 'com.google.android.gms:play-services-auth:21.x.x'  // Google Sign-In
```

---

## Authentication

**Usullar:** Email/Parol + Google Sign-In (ikkalasi ham Firebase Auth orqali)

**FirebaseManager.java API:**
```java
FirebaseManager.init(Context ctx)
FirebaseManager.signInWithEmail(String email, String password, Callback cb)
FirebaseManager.signInWithGoogle(Activity activity, Callback cb)
FirebaseManager.signOut()
FirebaseManager.getCurrentUser()   // null = not logged in
FirebaseManager.getUid()           // null = not logged in
```

**Login UI (ParentalDashboardActivity):**
- Login bo'lmagan holat: "☁️ Bulutga saqlash" tugmasi
- Login bo'lgan holat: akkaunt email + "↕️ Sinxronlash" + "Chiqish" tugmalari

---

## Sync Strategiyasi

### Upload triggerlari (local → Firestore)

| Hodisa | Metod |
|--------|-------|
| Profil yaratish/tahrirlash | `FirestoreSync.uploadProfiles()` |
| Badge topildi | `FirestoreSync.uploadBadge(profileId, badgeId)` |
| O'yin sessiyasi tugadi | `FirestoreSync.uploadDayStat(date, gameId, minutes)` |
| Ota-ona sozlamalari o'zgardi | `FirestoreSync.uploadParentSettings()` |
| App background'ga o'tsa | `FirestoreSync.flushQueue()` |

### Download (Firestore → local)

Login muvaffaqiyatli bo'lgandan keyin bir marta to'liq download:
1. `users/{uid}` → SharedPreferences (lang, pin, timeLimit, ageFilter)
2. `users/{uid}/profiles/*` → ProfileManager localStorage keys
3. `users/{uid}/profiles/*/badges/*` → BadgeManager localStorage keys
4. `users/{uid}/stats/*` → ParentalStatsManager SharedPreferences

### Conflict Resolution

Har bir hujjatda `updatedAt: Timestamp` bor.
- Agar Firestore `updatedAt` > local `updatedAt` → cloud wins
- Agar local `updatedAt` > Firestore `updatedAt` → local wins (upload)
- Yangi qurilmada: Firestore wins (download)

### Offline Queue

Internet yo'q bo'lganda pending upload'lar `SharedPreferences["kz-sync-queue"]`'da JSON array sifatida saqlanadi. `ConnectivityManager` listener internet kelganda `flushQueue()` chaqiradi.

---

## Login Oqimi (step-by-step)

```
Ota-ona → Parental Dashboard → "Bulutga saqlash" tugmasi
    → FirebaseManager.signIn() (email yoki Google)
    → Muvaffaqiyatli → FirestoreSync.downloadAll(uid)
    → Merge (updatedAt comparison)
    → Dashboard yangilanadi: "✅ Sinxronlashdi — user@email.com"

Keyingi har bir o'zgarishda → async uploadQueue
App yopilayotganda → flushQueue()
```

---

## Error Handling

| Xato | Holat | Yechim |
|------|-------|--------|
| Tarmoq yo'q | Upload | Queue'ga qo'shiladi, keyinroq yuboriladi |
| Auth xatosi | Login | Xato matni ko'rsatiladi, retry tugmasi |
| Firestore ruxsat yo'q | Download | Log, silent fail (local ma'lumot qoladi) |
| Token muddati o'tgan | Any | Auto-refresh (Firebase SDK o'zi qiladi) |

---

## Security

- PIN `SharedPreferences`'da SHA-256 hash sifatida saqlanadi (plain text yo'q)
- Firestore'ga PIN hash yuklanadi — server ham plain text ko'rmaydi
- `google-services.json` — git'ga commit qilinadi (API key emas, project config)
- Firestore rules: har bir `uid` faqat o'z ma'lumotiga yetadi

---

## Keyingi bloklar (bu spec'dan tashqari)

| Blok | Bog'liqlik |
|------|-----------|
| Admin Web Panel | Firestore'dagi `users/*` to'plamiga Firebase Admin SDK orqali |
| Push Notifications | Firebase FCM + `users/{uid}/fcmToken` field |
| Marketing Analytics | Firebase Analytics events + user properties |
