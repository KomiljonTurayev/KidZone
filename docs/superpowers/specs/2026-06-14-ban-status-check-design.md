# Ban Status Check — Design Spec

**Date:** 2026-06-14
**Scope:** CRM Block 3 — Admin panel ban qilgan foydalanuvchini app ochilganda aniqlash va OnboardingActivity ga yo'naltirish.

---

## Maqsad

Backend `POST /admin/users/{uid}/ban` orqali ban qilingan foydalanuvchi KidZone app'ni ochganda Firestore'dan `status` tekshiriladi. `"banned"` bo'lsa onboarding prefs tozalanib, `OnboardingActivity`'ga yo'naltiriladi. Mavjud barcha funksionallik o'zgarishsiz qoladi.

---

## Arxitektura

```
MainActivity.onCreate()
    └── FirebaseManager.ensureAuthAsync()
            └── uid != null bo'lsa:
                    ├── FcmTokenManager.registerToken(uid, firestoreSync)   ← mavjud
                    └── BanChecker.checkAsync(uid, firestoreSync, status -> {
                                if (status == BANNED) → banUser()
                                // ACTIVE | ERROR → davom etadi
                        })

BanChecker
    ├── checkAsync(uid, FirestoreSync, Callback)      ← production
    └── checkAsync(uid, DocProvider, Callback)        ← test uchun injectable

MainActivity.banUser()
    └── prefs.putBoolean(KEY_DONE, false)
    └── startActivity(OnboardingActivity)
    └── finish()
```

---

## Fayllar

| Harakat | Fayl | Nima |
|---------|------|------|
| Yaratish | `app/src/main/java/uz/kidzone/app/BanChecker.java` | Ban tekshiruv utility |
| Yaratish | `app/src/test/java/uz/kidzone/app/BanCheckerTest.java` | TDD testlar (7 ta) |
| O'zgartirish | `app/src/main/java/uz/kidzone/app/MainActivity.java` | `checkAsync` chaqiruvi + `banUser()` |

---

## BanChecker API

```java
public final class BanChecker {

    public enum Status { ACTIVE, BANNED, ERROR }

    public interface Callback {
        void onResult(Status status);
    }

    interface DocProvider {
        void getDoc(String uid, DocCallback cb);
    }

    interface DocCallback {
        void onDoc(boolean exists, String status);
        void onError();
    }

    /** Production: Firestore orqali tekshiradi */
    public static void checkAsync(String uid, FirestoreSync sync, Callback cb) { ... }

    /** Test: injectable DocProvider */
    static void checkAsync(String uid, DocProvider provider, Callback cb) { ... }
}
```

---

## Status Resolution

| Firestore holati | Natija |
|------------------|--------|
| `uid` null | `ERROR` |
| Firestore not available | `ERROR` |
| Tarmoq xatosi / timeout | `ERROR` |
| Hujjat mavjud emas | `ACTIVE` |
| `status` maydoni yo'q | `ACTIVE` |
| `status = "active"` | `ACTIVE` |
| `status = "banned"` | `BANNED` |
| Boshqa qiymat | `ACTIVE` |

**`ERROR` → app davom etadi** (tarmoq muammosi sababli bolani bloklamaslik uchun).

---

## MainActivity o'zgarishlari

`ensureAuthAsync` callback ichiga `BanChecker.checkAsync` qo'shiladi:

```java
FirebaseManager.getInstance().ensureAuthAsync(() -> {
    String uid = FirebaseManager.getInstance().getUid();
    if (uid != null) {
        FcmTokenManager.registerToken(uid, firestoreSync);
        BanChecker.checkAsync(uid, firestoreSync, status -> {
            if (status == BanChecker.Status.BANNED) banUser();
        });
    }
});
```

`banUser()` metodi qo'shiladi:

```java
private void banUser() {
    getSharedPreferences(OnboardingActivity.PREFS, MODE_PRIVATE)
        .edit().putBoolean(OnboardingActivity.KEY_DONE, false).apply();
    startActivity(new Intent(this, OnboardingActivity.class));
    finish();
}
```

---

## TDD Testlar

| Test | Nima tekshiriladi |
|------|-------------------|
| `nullUid_returnsError` | uid null → `ERROR` |
| `unavailableSync_returnsError` | db null → `ERROR` |
| `docNotExists_returnsActive` | hujjat yo'q → `ACTIVE` |
| `statusActive_returnsActive` | `"active"` → `ACTIVE` |
| `statusBanned_returnsBanned` | `"banned"` → `BANNED` |
| `statusMissing_returnsActive` | maydon yo'q → `ACTIVE` |
| `unknownStatus_returnsActive` | `"suspended"` → `ACTIVE` |

---

## Error Handling

| Holat | Yechim |
|-------|--------|
| Tarmoq yo'q | `ERROR` → app davom etadi |
| Firestore not configured | `ERROR` → app davom etadi |
| `status` maydoni yo'q | `ACTIVE` → app davom etadi |
| Ban aniqlandi | prefs reset + OnboardingActivity |
| `banUser()` Activity allaqachon finish bo'lgan | `isFinishing()` guard |

---

## Bog'liqliklar

- `FirestoreSync.isAvailable()` — mavjud ✓
- `OnboardingActivity.PREFS`, `KEY_DONE` — mavjud ✓
- `FirebaseManager.ensureAuthAsync()` — mavjud ✓
- Backend `POST /admin/users/{uid}/ban` → `users/{uid}.status = "banned"` ✓
