# Admin Panel + Banner System — Design Spec

**Date:** 2026-06-18
**Scope:** CRM Block 4 — In-app admin panel (user list + ban/unban) va Banner tizimi (Firestore + FCM push + deep link).

---

## Maqsad

Admin (operator) KidZone app'ni o'z qurilmasida ochib:
1. Barcha foydalanuvchilarni ko'radi va ban/unban qila oladi.
2. Barcha foydalanuvchilarga banner yuboradi — push notification + app ichida overlay karta.

Banner admin o'chirguncha ko'rinadi. Bosilganda admin belgilagan URL ochiladi.

---

## Arxitektura

```
ParentalDashboardActivity  (faqat ADMIN_UID uchun ko'rinadi)
├── [A] Foydalanuvchilar ro'yxati
│     ├── FirestoreSync.getAllUsers(callback)
│     └── FirestoreSync.setUserStatus(uid, "banned"|"active", callback)
└── [B] Banner boshqaruvi
      ├── FirestoreSync.setBanner(title, body, url, adminUid)
      ├── BackendClient.sendTopicPush(idToken, title, body, url)  → POST /push/send-all
      └── FirestoreSync.clearBanner()

MainActivity (barcha foydalanuvchilar)
├── onCreate → BannerChecker.checkAsync(firestoreSync, callback)
│     └── aktiv?  →  BannerView ko'rsatadi (games grid ustida)
│           └── tap  →  URL ochadi (CustomTabs yoki kidzone:// WebView)
└── onResume → SharedPrefs["kz_pending_url"] → URL ochadi → tozalaydi

KidZoneFirebaseMessagingService
└── onMessageReceived → data["url"] ni SharedPrefs["kz_pending_url"]'ga saqlaydi
```

---

## Firestore Schema

### Mavjud (o'zgarmaydi)
```
users/{uid}/
  ├── email: string
  ├── status: "active" | "banned"
  ├── createdAt: Timestamp
  └── lastActiveAt: Timestamp
```

### Yangi
```
config/banner
  ├── active:    boolean
  ├── title:     string
  ├── body:      string
  ├── url:       string
  ├── createdAt: Timestamp
  └── createdBy: string  (admin UID)
```

---

## Firestore Security Rules

Firebase Console'da yangilanadi (repo fayliga emas):

```
match /users/{uid} {
  allow read, write: if request.auth.uid == uid
                     || request.auth.uid == "ADMIN_UID";
}
match /config/{doc} {
  allow read:  if request.auth != null;
  allow write: if request.auth.uid == "ADMIN_UID";
}
```

---

## Admin Panel UI

`ParentalDashboardActivity`'da `setupAdminPanel()` — faqat `FirebaseManager.getUid() == AdminConfig.ADMIN_UID` bo'lganda ikki karta render qiladi.

### Karta A — Foydalanuvchilar

```
┌─────────────────────────────────────────┐
│  👥 Foydalanuvchilar          [Yangilash]│
├─────────────────────────────────────────┤
│  user@mail.com   ● aktiv    [Ban]       │
│  other@mail.com  ● ban      [Unban]     │
└─────────────────────────────────────────┘
```

- `getAllUsers()` chaqiriladi — email + status ko'rsatiladi.
- "Ban" tugmasi → confirmation dialog → `setUserStatus(uid, "banned")`.
- "Unban" tugmasi → `setUserStatus(uid, "active")`.
- "Yangilash" → ro'yxatni qayta yuklaydi.

### Karta B — Banner

```
┌─────────────────────────────────────────┐
│  📢 Banner                              │
│  Sarlavha: [________________]           │
│  Matn:     [________________]           │
│  URL:      [________________]           │
│                         [Yuborish]      │
├─────────────────────────────────────────┤
│  Aktiv: "Yangi o'yin chiqdi!"  [O'chirish]│
└─────────────────────────────────────────┘
```

- "Yuborish" → `setBanner()` + `BackendClient.sendTopicPush()`.
- "O'chirish" → `clearBanner()` (active = false).
- Aktiv banner mavjud bo'lsa, form ustida ko'rsatiladi.

---

## App-side Banner

### BannerView (overlay karta)

`MainActivity` layout'iga qo'shiladi — games grid ustida, boshlang'ich holatda `GONE`:

```
┌──────────────────────────────────────[X]┐
│  🎉 Yangi o'yin chiqdi!                 │
│  Shape Match 2.0 — sinab ko'ring        │
└─────────────────────────────────────────┘
```

- `X` tugmasi — faqat shu sessiyada yashiradi (Firestore'da o'zgarmaydi).
- Karta bosilsa URL ochiladi.

### URL ochish

| URL turi | Harakat |
|----------|---------|
| `kidzone://game/{id}` | WebView ichida navigatsiya |
| `https://...` | `CustomTabsIntent` |
| Boshqa | `Intent.ACTION_VIEW` |

### Push → deep link oqimi

1. `KidZoneFirebaseMessagingService.onMessageReceived` → `data["url"]` → `SharedPrefs["kz_pending_url"]`.
2. `MainActivity.onResume()` → `kz_pending_url` bor? → URL ochadi → tozalaydi.

---

## Backend Endpoint (yangi kerak)

```
POST /push/send-all
Authorization: Bearer {Firebase ID token}   ← admin UID server'da tekshiriladi
Body: {
  "title": "...",
  "body":  "...",
  "data":  { "url": "..." }
}
→ FCM topic "all-users" ga push yuboradi
```

**ID token olish:** `FirebaseManager.getInstance().getCurrentUser().getIdToken(true)` — async, `BackendClient` bu token'ni header'ga qo'shadi.

---

## Yangi va o'zgaritiladigan fayllar

| Harakat | Fayl | Nima |
|---------|------|------|
| Yaratish | `AdminConfig.java` | `ADMIN_UID` konstantasi |
| Yaratish | `BannerChecker.java` | `config/banner` o'qiydi, injectable DocProvider |
| Yaratish | `BackendClient.java` | `POST /push/send-all` chaqiruvi |
| O'zgartirish | `FirestoreSync.java` | `getAllUsers()`, `setUserStatus()`, `setBanner()`, `clearBanner()` |
| O'zgartirish | `ParentalDashboardActivity.java` | `setupAdminPanel()` — ikkita yangi karta |
| O'zgartirish | `activity_parental_dashboard.xml` | Admin kartalari uchun placeholder view'lar |
| O'zgartirish | `KidZoneFirebaseMessagingService.java` | `data["url"]` → SharedPrefs |
| O'zgartirish | `MainActivity.java` | `BannerChecker.checkAsync()` + `onResume` URL handling + BannerView |
| Yaratish | `BannerCheckerTest.java` | TDD — aktiv/inaktiv/hujjat yo'q holatlari |
| Yaratish | `FirestoreSyncAdminTest.java` | TDD — `getAllUsers`, `setUserStatus`, `setBanner`, `clearBanner` |

---

## TDD Tests

### BannerCheckerTest (6 test)
- `checkAsync_activeBanner_returnsBanner`
- `checkAsync_inactiveBanner_returnsEmpty`
- `checkAsync_noDocument_returnsEmpty`
- `checkAsync_firestoreUnavailable_returnsEmpty`
- `checkAsync_missingUrl_returnsEmpty`
- `checkAsync_callbackOnMainThread`

### FirestoreSyncAdminTest (6 test)
- `getAllUsers_returnsUserList`
- `getAllUsers_firestoreUnavailable_returnsEmpty`
- `setUserStatus_banned_writesCorrectly`
- `setUserStatus_active_writesCorrectly`
- `setBanner_writesAllFields`
- `clearBanner_setsActiveFalse`

---

## Keyingi bloklar (bu spec'dan tashqari)

| Blok | Bog'liqlik |
|------|-----------|
| Marketing Analytics | Firebase Analytics events |
| Segmented push | Foydalanuvchi guruhlariga alohida push yuborish |
