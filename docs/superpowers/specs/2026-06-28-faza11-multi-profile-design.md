# Faza 11 — Ko'p Farzand Profili: Dizayn Spesifikatsiyasi

**Sana:** 2026-06-28
**Holat:** Approved
**Bog'liq:** `docs/superpowers/specs/2026-06-23-faza10-play-store-release-design.md`

---

## Maqsad

Har bir bola uchun to'liq izolyatsiyalangan profil tizimi: alohida til, vaqt limiti, PIN, statistika va avatar. App start'da profil tanlash ekrani, ota-ona dashboard'dan ham almashtirish imkoni. Profillar Room DB'da lokal saqlanadi, Firestore'ga sync bo'ladi.

---

## Qamrov

**Kiradi:**
- `ProfileEntity` va `ProfileStatsEntity` — Room entities
- `KidZoneDatabase` — Room database (1-versiya)
- `ProfileDao`, `ProfileStatsDao` — CRUD
- `ProfileRepository` — Room + Firestore sync wrapper
- `ProfileSyncManager` — Firestore push/pull
- `ProfileViewModel` — profil boshqaruvi
- `ProfileSelectScreen` — app start profil tanlash ekrani
- `AddEditProfileScreen` — yangi profil / tahrirlash
- `ParentDashboardScreen` — profil ro'yxati, qo'shish, o'chirish, almashtirish (mavjud ekranga integratsiya)
- Avatar: kamera/galereya → `filesDir/profiles/{id}.jpg`
- Migration: mavjud SharedPreferences → "Default" profil
- TDD: `ProfileRepositoryTest`, `ProfileViewModelTest`, `ProfileSyncManagerTest`

**Kirmaydi:**
- Avatar Firestore'ga yuklanmaydi (faqat lokal)
- Ko'p til per-game konfiguratsiya (mavjud `?lang=` parametr profil tiliga bog'lanadi)
- Backend REST API profil sync (faqat Firestore)

---

## Ma'lumotlar modeli

### Room — `ProfileEntity`

| Maydon | Tip | Izoh |
|--------|-----|------|
| `id` | `String` (UUID) | Primary key |
| `name` | `String` | Bola ismi |
| `avatarPath` | `String?` | `filesDir/profiles/{id}.jpg` yo'li |
| `language` | `String` | `"uz"` \| `"ru"` \| `"en"` |
| `timeLimitMinutes` | `Int` | 0 = cheksiz |
| `pinHash` | `String?` | SHA-256, null = PIN yo'q |
| `isDefault` | `Boolean` | Migration'dan kelgan birinchi profil |
| `createdAt` | `Long` | Unix timestamp (ms) |

### Room — `ProfileStatsEntity`

| Maydon | Tip | Izoh |
|--------|-----|------|
| `id` | `Int` | Auto-increment primary key |
| `profileId` | `String` | Foreign key → `ProfileEntity.id` |
| `date` | `String` | `"2026-06-28"` format |
| `minutesPlayed` | `Int` | Bugungi o'yin vaqti |
| `gamesPlayed` | `Int` | Bugungi o'yinlar soni |

### Firestore tuzilma

```
users/{uid}/profiles/{profileId}
  name, language, timeLimitMinutes, pinHash, isDefault, createdAt

users/{uid}/profiles/{profileId}/stats/{date}
  minutesPlayed, gamesPlayed
```

Avatar Firestore'ga tushmaydi — faqat lokal `filesDir`'da saqlanadi.

---

## Arxitektura

```
UI (Compose)
  ProfileSelectScreen
  AddEditProfileScreen
  ParentDashboardScreen (profil tab qo'shiladi)
  MainScreen (faol profil badge)
        │
        ▼
ProfileViewModel
MainViewModel (activeProfile: StateFlow<ProfileEntity> qabul qiladi)
        │
        ▼
ProfileRepository
  ├── ProfileDao (Room)
  ├── ProfileStatsDao (Room)
  └── ProfileSyncManager (Firestore push/pull)
        │
        ▼
KidZoneDatabase (Room)   +   Firebase Firestore
```

### `ProfileRepository` javobgarligi
- CRUD operatsiyalar (Room orqali)
- Profil o'zgarishida Firestore'ga push
- Login bo'lganda Firestore'dan pull (merge strategiyasi: `createdAt` bo'yicha yangisi ustunlik qiladi)
- Faol profil `SharedPreferences`'da `active_profile_id` sifatida saqlanadi (tez o'qish uchun)

### `ProfileSyncManager`
Mavjud `FirestoreSync.kt` pattern'ini kuzatadi:
- `pushProfile(profile: ProfileEntity)`
- `pushStats(stats: ProfileStatsEntity)`
- `pullProfiles(uid: String): List<ProfileEntity>`
- Offline holatda xatosiz ishlaydi (Firestore lokal cache ishlatadi)

---

## UI oqimlari

### App start oqimi
```
App ochiladi
  ↓
Profillar soni == 0?
  → Migration (SharedPreferences → "Default" profil yaratiladi)
Profillar soni == 1?
  → To'g'ridan MainScreen (ProfileSelectScreen o'tkazib yuboriladi)
Profillar soni >= 2?
  → ProfileSelectScreen
      → Bola avatar/ism bosadi
      → MainScreen (faol profil o'rnatiladi)
```

### Dashboard orqali profil almashtirish
```
MainScreen → FAB → PIN gate → ParentDashboardScreen
  → "Profil almashtirish" tugmasi
  → ProfileSelectScreen (modal yoki navigate)
  → Tanlov → faol profil o'zgaradi → MainScreen
```

### Profil qo'shish / tahrirlash
```
ParentDashboardScreen → "Yangi profil" / "Tahrirlash"
  → AddEditProfileScreen
      Ism (TextField)
      Avatar (Kamera / Galereya → filesDir saqlash)
      Til (UZ / RU / EN chip)
      Vaqt limiti (slider yoki raqam kiritish)
      PIN (ixtiyoriy, 4 xona)
  → Saqlash → Room insert/update → Firestore push
```

### Avatar oqimi
```
AddEditProfileScreen → "Rasm" tugmasi
  → System picker (kamera yoki galereya)
  → Rasm compress + filesDir/profiles/{profileId}.jpg saqlash
  → ProfileEntity.avatarPath yangilanadi
  → UI'da AsyncImage / Coil bilan ko'rsatiladi
```

---

## Migration (bir martalik)

App yangi versiya bilan birinchi ochilganda:
1. `Room.getCount() == 0` tekshirish
2. SharedPreferences'dan o'qish: `kz-lang`, `time_limit_minutes`, `pin_hash`
3. UUID generate qilib `ProfileEntity(isDefault=true, name="Asosiy")` yaratish
4. `active_profile_id` SharedPreferences'ga yozish
5. Mavjud stats ma'lumotlari (agar bor bo'lsa) yangi profil ID'siga ko'chirish

---

## `MainViewModel` integratsiyasi

```kotlin
val activeProfile: StateFlow<ProfileEntity> // ProfileRepository'dan
```

- `timeLimitMinutes` → `TimeLimitViewModel` ga uzatiladi
- `language` → WebView URL parametri (`?lang=uz`)
- `pinHash` → PIN gate validatsiyasi

`ParentalStatsManager` profil-aware bo'ladi: barcha SharedPreferences kalitlari `{profileId}_` prefiksi bilan namespace'lanadi.

---

## Xatolarni boshqarish

| Holat | Yechim |
|-------|--------|
| Firestore offline | Room lokal ishlayveradi, sync keyinroq avtomatik |
| Avatar fayl yo'qolgan | Rangli doira + ism initiali (fallback) |
| PIN unutildi | Dashboard → "PIN reset" (ota-ona faqat) |
| Bitta profil o'chirilishi | Kamida 1 ta profil qolishi shart (UI'da tekshirish) |
| Migration xatosi | Catch → "Default" profil default qiymatlar bilan yaratiladi |

---

## Testlar (TDD)

| Test sinfi | Nimani tekshiradi |
|------------|-------------------|
| `ProfileRepositoryTest` | CRUD, migration logikasi (fake DAO) |
| `ProfileViewModelTest` | Faol profil almashinuvi, vaqt limiti state o'zgarishi |
| `ProfileSyncManagerTest` | Firestore push/pull (mock Firestore) |

---

## Yangi dependencies

```groovy
// Room
implementation "androidx.room:room-runtime:2.6.1"
implementation "androidx.room:room-ktx:2.6.1"
kapt "androidx.room:room-compiler:2.6.1"

// Coil (avatar image loading)
implementation "io.coil-kt:coil-compose:2.6.0"
```

`kapt` uchun `build.gradle`'ga `id 'kotlin-kapt'` plugin qo'shiladi.

---

## Versiya

- `versionCode`: 11
- `versionName`: "1.2.0"
