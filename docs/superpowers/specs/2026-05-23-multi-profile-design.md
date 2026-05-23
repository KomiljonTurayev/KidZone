# Multi-Profile Tizimi Design

**Sana:** 2026-05-23
**Feature:** Bir qurilmada 5 tagacha bola profili
**Yondashuv:** Prefix-based localStorage — `ProfileManager` JS klassi

---

## Maqsad

Bir qurilmani bir nechta bola ishlatishi mumkin. Har bir bolaning yulduzlari, badgelari, tili, yosh guruhi, streak va o'yin tarixi to'liq alohida saqlanadi. App ochilganda profil tanlash ekrani chiqadi.

---

## Arxitektura

`ProfileManager` — yangi JS klassi `main.js`da. Barcha mavjud manager'lar (`BadgeManager`, `GameManager`, `StoryManager`, `SongManager`) localStorage kalitlarini `ProfileManager.key(suffix)` orqali oladi — bu metod `kz-{activeId}-{suffix}` qaytaradi.

Profil o'zgarganda `window.location.reload()` — barcha managerlar yangi profil bilan qayta init bo'ladi.

---

## localStorage Kalitlari

### Global (profil tashqarida)

| Kalit | Tur | Tavsif |
|-------|-----|--------|
| `kz-profiles` | JSON array | `[{id, name, avatarIdx}]` — barcha profillar metadata |
| `kz-active-profile` | string | Faol profil ID (`"p1"`, `"p2"`, ...) |

### Per-profil (`kz-{id}-*`)

| Kalit | Tur | Default | Tavsif |
|-------|-----|---------|--------|
| `kz-{id}-pts` | int string | `"0"` | Yulduzlar (mavjud `kz-pts`) |
| `kz-{id}-badges` | JSON array | `[]` | Badgelar (mavjud `kz-badges`) |
| `kz-{id}-lang` | string | `"uz"` | Til (mavjud `kz-lang`) |
| `kz-{id}-age` | string | `"2-4"` | Yosh guruhi (mavjud `kz-age`) |
| `kz-{id}-game-count` | int string | `"0"` | O'yin soni |
| `kz-{id}-story-count` | int string | `"0"` | Ertak soni |
| `kz-{id}-song-count` | int string | `"0"` | Qo'shiq soni |
| `kz-{id}-cats-tried` | JSON array | `[]` | Sinab ko'rilgan kategoriyalar |
| `kz-{id}-streak-days` | JSON array | `[]` | Streak kunlar (YYYYMMDD) |
| `kz-{id}-cat-off` | string | `""` | O'chirilgan kategoriyalar (CSV) |

---

## Migration

Birinchi ishga tushganda (yangi versiya o'rnatilganda) mavjud prefikssiz kalitlar `p1` profiliga ko'chiriladi:

```
kz-pts        → kz-p1-pts
kz-badges     → kz-p1-badges
kz-lang       → kz-p1-lang
kz-age        → kz-p1-age
kz-game-count → kz-p1-game-count
kz-story-count → kz-p1-story-count
kz-song-count → kz-p1-song-count
kz-cats-tried → kz-p1-cats-tried
kz-streak-days → kz-p1-streak-days
kz-cat-off    → kz-p1-cat-off
```

Shundan keyin eski kalitlar o'chiriladi. Profil nomi: `"Profil 1"`, avatarIdx: `0`.

---

## ProfileManager Interfeysi

```javascript
class ProfileManager {
    constructor()                  // migration, active profile yuklash
    key(suffix)                    // "kz-{activeId}-{suffix}" qaytaradi
    getActive()                    // {id, name, avatarIdx} qaytaradi
    getAll()                       // barcha profillar array
    create(name, avatarIdx)        // yangi profil (max 5), ID auto-generate
    rename(id, newName)            // profil nomini o'zgartirish
    remove(id)                     // profil + uning barcha kz-{id}-* kalitlari
    switchTo(id)                   // kz-active-profile = id, reload
}
```

`ProfileManager` `window.profileManager` sifatida global bo'ladi.

---

## Mavjud Managerlarni O'zgartirish

`BadgeManager`, `GameManager` va boshqalar `localStorage.getItem('kz-pts')` o'rniga `localStorage.getItem(profileManager.key('pts'))` ishlatadi.

O'zgartirilishi kerak bo'lgan kalitlar:
- `kz-pts`, `kz-badges`, `kz-lang`, `kz-age`
- `kz-game-count`, `kz-story-count`, `kz-song-count`
- `kz-cats-tried`, `kz-streak-days`, `kz-cat-off`

`kz-music`, `kz-vol` (musiqa sozlamalari) — umumiy qoladi (profil tashqarida).

---

## UI — Profil Tanlash Ekrani

**Trigger:** App ochilganda `#profile-picker` overlay to'liq ekranda chiqadi (z-index: 9000).

**Tuzilish:**
```
╔══════════════════════════════════╗
║   👶 Kim o'ynaydi?               ║
║                                  ║
║  ┌──────┐  ┌──────┐  ┌──────┐   ║
║  │ [av] │  │ [av] │  │  ➕  │   ║
║  │ Ali  │  │ Zara │  │ Yangi│   ║
║  └──────┘  └──────┘  └──────┘   ║
╚══════════════════════════════════╝
```

- Profillar 3 ustunli grid
- Har bir karta: avatar + ism + bugungi yulduzlar soni
- `➕ Yangi profil` kartochkasi (faqat < 5 profil bo'lganda)
- Profil bosilganda: `profileManager.switchTo(id)` → reload → app shu profil bilan ishga tushadi
- Yangi profil bosilganda: ism kiritish + avatar tanlash form chiqadi (overlay ichida)

**Header:** `🏠` (home-style sarlavha), yopish tugmasi YO'Q — profil tanlash majburiy.

---

## UI — Avatar Tanlash

8 ta inline SVG kartun avatar `index.html`da:

| Indeks | Tavsif |
|--------|--------|
| 0 | Qiz — jigarrang soch, pushti bant |
| 1 | Qiz — qora soch, sariq bant |
| 2 | Qiz — sariq soch, ko'k bant |
| 3 | Qiz — qizil soch, binafsha bant |
| 4 | O'g'il — jigarrang soch, ko'k ko'ylak |
| 5 | O'g'il — qora soch, yashil ko'ylak |
| 6 | O'g'il — sariq soch, to'q sariq ko'ylak |
| 7 | O'g'il — qora soch, to'q teri, qizil ko'ylak |

SVGlar `<svg>` tegi sifatida `index.html`da inline saqlanadi, `<template id="av-0">` ... `<template id="av-7">` ichida.

---

## UI — Parental Dashboard

`ParentalDashboardActivity.java`ga yangi **"Profillar"** bo'limi qo'shiladi (mavjud bo'limlardan yuqorida):

- Profillar horizontal scroll yoki grid ko'rinishida
- Har bir profil: avatar + ism + bugungi daqiqalar
- **Qo'shish:** `+` tugma → ism va avatar tanlash dialog → `profileManager.create()` via JS inject
- **O'chirish:** Profil long-press → confirm dialog → `profileManager.remove()` via JS inject
- **Cheklov:** Yagona profil o'chirilmaydi

Dashboard'dagi mavjud Yosh guruhi va Til tugmalari faol profil uchun ishlaydi — `kz-{activeId}-age`, `kz-{activeId}-lang`.

---

## Fayllar

| Harakat | Fayl | Nima o'zgaradi |
|---------|------|----------------|
| Modify | `app/src/main/assets/www/main.js` | `ProfileManager` klassi, migration, barcha manager kalitlari prefix orqali |
| Modify | `app/src/main/assets/www/index.html` | `#profile-picker` overlay, avatar `<template>`lar, `openProfilePicker()` |
| Modify | `app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java` | Profillar bo'limi (add/delete) |

---

## Sinov

1. Birinchi ishga tushganda mavjud ma'lumotlar `p1` ga ko'chiriladi
2. Profil tanlash ekrani har safar app ochilganda chiqadi
3. Ali profili → 50 ball → Zara profiliga o'tish → Ali'ning 50 bali saqlanadi
4. Yangi profil yaratish — ism + avatar tanlash ishlaydi
5. Maksimum 5 ta profil: `➕` tugma 5 tadan keyin ko'rinmaydi
6. Oxirgi profilni o'chirish mumkin emas
7. Parental Dashboard'da profil qo'shish/o'chirish ishlaydi
8. Har bir profil uchun til, yosh guruhi alohida ishlaydi
9. Badge'lar profil bo'yicha alohida hisoblanadi
10. Streak profil bo'yicha alohida hisoblanadi
