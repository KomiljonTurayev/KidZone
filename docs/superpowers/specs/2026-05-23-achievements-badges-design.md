# Achievements — Badge Tizimi Design

**Sana:** 2026-05-23
**Feature:** Mavjud yulduzcha tizimiga badge (yutuq) tizimi qo'shish
**Yondashuv:** To'liq JavaScript — `main.js` + `index.html`

---

## Maqsad

Bola o'yin o'ynagani, ertak o'qigani, qo'shiq tinglagani va kunlik faolligini badge bilan taqdirlash. Badge olinganida ekranda animatsiyali popup chiqadi; barcha badgelar alohida sahifada ko'rinadi.

---

## Arxitektura

`BadgeManager` — yangi JS ob'ekti `main.js`da. Mavjud `GameManager` metodlariga hook qilib ulanadi. Alohida fayl kerak emas — bir modul `main.js` oxiriga qo'shiladi.

### localStorage kalitlari

| Kalit | Tur | Default | Tavsif |
|-------|-----|---------|--------|
| `kz-badges` | JSON array | `[]` | Olingan badge IDlar |
| `kz-game-count` | int string | `"0"` | Jami o'yinlar soni |
| `kz-story-count` | int string | `"0"` | Jami ertaklar soni |
| `kz-song-count` | int string | `"0"` | Jami qo'shiqlar soni |
| `kz-cats-tried` | JSON array | `[]` | Sinab ko'rilgan kategoriyalar |
| `kz-streak-days` | JSON array | `[]` | Kirgan kunlar (YYYYMMDD format) |

`kz-pts` — mavjud, o'zgarmaydi.

---

## Badgelar (16 ta)

### O'yin badgelari
| ID | Emoji | Nomi (UZ) | Shart |
|----|-------|-----------|-------|
| `game_first` | 🎮 | Birinchi qadam | 1 o'yin |
| `game_5` | 🕹️ | O'yinchi | 5 o'yin |
| `game_25` | 🏆 | Pro o'yinchi | 25 o'yin |
| `game_50` | 🌟 | Ustoz | 50 o'yin |

### Ertak badgelari
| ID | Emoji | Nomi (UZ) | Shart |
|----|-------|-----------|-------|
| `story_first` | 📖 | Birinchi ertak | 1 ertak |
| `story_10` | 📚 | Kitobxon | 10 ertak |

### Musiqa badgelari
| ID | Emoji | Nomi (UZ) | Shart |
|----|-------|-----------|-------|
| `song_first` | 🎵 | Birinchi qo'shiq | 1 qo'shiq |
| `song_10` | 🎶 | Musiqa sevari | 10 qo'shiq |

### Kategoriya badgelari
| ID | Emoji | Nomi (UZ) | Shart |
|----|-------|-----------|-------|
| `cat_3` | 🌍 | Izlovchi | 3 ta turli kategoriya |
| `cat_all` | 🏅 | Tadqiqotchi | 9 ta kategoriyaning hammasi |

Kategoriyalar: `alphabet`, `animals`, `dance`, `family`, `games`, `heroes`, `lullaby`, `nature`, `space`.

### Ball badgelari
| ID | Emoji | Nomi (UZ) | Shart |
|----|-------|-----------|-------|
| `stars_100` | ⭐ | Yulduzcha | 100 ball |
| `stars_500` | 🌟 | Porloq yulduz | 500 ball |
| `stars_1000` | 💫 | Super yulduz | 1000 ball |
| `stars_5000` | 🏆 | Yulduzlar qiroli | 5000 ball |

### Streak badgelari
| ID | Emoji | Nomi (UZ) | Shart |
|----|-------|-----------|-------|
| `streak_3` | 🔥 | Uch kun | 3 kun ketma-ket |
| `streak_7` | 🔥🔥 | Bir hafta | 7 kun ketma-ket |

---

## BadgeManager interfeysi

```javascript
class BadgeManager {
    constructor()                    // localStorage dan holat yuklaydi
    onGamePlayed(categoryId)         // game-count++, cats-tried yangilanadi, badgelar tekshiriladi
    onStoryPlayed()                  // story-count++, badgelar tekshiriladi
    onSongPlayed()                   // song-count++, badgelar tekshiriladi
    onPointsUpdated(totalPts)        // ball badgelarini tekshiradi
    checkStreak()                    // bugungi kunni streak-days ga qo'shadi, streak tekshiriladi
    awardBadge(id)                   // localStorage ga yozadi, popup navbatiga qo'shadi
    showNextPopup()                  // navbatdagi badge popupni ko'rsatadi
    isEarned(id)                     // boolean — badge allaqachon olinganmi
}
```

`awardBadge(id)` bir xil badge ikki marta berilishidan himoya qiladi (`isEarned` tekshiruvi).

---

## Integratsiya nuqtalari (`main.js`)

| Nuqta | Mavjud kod | Qo'shimcha |
|-------|-----------|------------|
| O'yin ochilishi | `GameManager.openGame()` | `badgeManager.onGamePlayed(game.cat)` |
| Ball qo'shilishi | `GameManager.addPoints(n)` | `badgeManager.onPointsUpdated(this.pts)` |
| Ertak ochilishi | story tab click handler | `badgeManager.onStoryPlayed()` |
| Qo'shiq ochilishi | song tab click handler | `badgeManager.onSongPlayed()` |
| Ilova yuklanganda | `GameManager` init | `badgeManager.checkStreak()` |

---

## UI — Badge Sahifasi

**Entry point:** `index.html` header'da `⭐ 1985` yoniga `🏅` tugma qo'shiladi. Bosganida `#badges-overlay` to'liq ekran overlay ochiladi.

**Ko'rinish:**
- Tepada sarlavha: `🏅 Mening Yutuqlarim` + yopish tugmasi `✕`
- Grid: 3 ustun, badgelar kartochka shaklida
- **Olingan badge:** rangli fon, emoji + nomi ko'rinadi
- **Olinmagan badge:** kulrang, emoji `🔒` + nomi ko'rinmaydi (`???`)
- Tepada progress: `5 / 16 yutuq olindi`

---

## UI — Badge Popup Animatsiyasi

Badge olingan zahoti ekran markazida bounce animatsiya bilan chiqadi:

```
╔══════════════════════╗
║        🏆            ║
║   Pro o'yinchi!      ║
║   25 o'yin o'ynaldi  ║
║   +50 ⭐ bonus       ║
╚══════════════════════╝
```

- Animatsiya: `scale(0) → scale(1.15) → scale(1)`, davomiyligi 400ms
- 3 soniyadan keyin `fadeOut` bilan o'z-o'zidan yopiladi
- Tap bilan ham yopiladi
- Bir sessiyada bir vaqtda bitta popup — qolgan navbat (`popupQueue`) bilan boshqariladi
- Badge olinganida **+50 ball bonus** beriladi (faqat birinchi marta)

---

## Ko'p til (i18n)

Badge nomlari `T` ob'ektiga UZ/RU/EN qo'shiladi. `index.html`dagi mavjud `TranslationManager` orqali til o'zgarganda badge sahifasi ham yangilanadi.

---

## Fayllar

| Harakat | Fayl |
|---------|------|
| Modify | `app/src/main/assets/www/main.js` |
| Modify | `app/src/main/assets/www/index.html` |

---

## Sinov

1. Birinchi o'yin ochilsa → `game_first` badge + popup chiqadi
2. 5 o'yin dan keyin → `game_5` badge chiqadi
3. Ertak o'qilsa → `story_first` chiqadi
4. Qo'shiq tinglanguncha → `song_first` chiqadi
5. 3 xil kategoriya sinalganda → `cat_3` chiqadi
6. 100 ball to'plansa → `stars_100` chiqadi
7. 3 kun ketma-ket kirsa → `streak_3` chiqadi
8. Badge sahifasi ochilsa — olingan rangli, olinmagan kulrang ko'rinadi
9. Bir badge ikki marta berilmaydi
10. Popup navbat bilan chiqadi (bir vaqtda bitta)
