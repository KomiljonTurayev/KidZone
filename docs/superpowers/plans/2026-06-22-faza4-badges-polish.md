# Faza 4: Badges & Profile Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Faza 3 dan keyin qolgan 4 ta muammoni tuzatish: SongManager badge hook, profile picker UX, i18n gaps.

**Architecture:** Faqat 2 fayl — `main.js` va `index.html`. Native Android kodi o'zgartirilmaydi. Barcha o'zgarishlar WebView HTML5 layer da.

**Tech Stack:** Vanilla JavaScript, HTML5, CSS. Android WebView orqali ishlaydi.

## Global Constraints

- Faqat `app/src/main/assets/www/main.js` va `app/src/main/assets/www/index.html` o'zgartiriladi
- Native Kotlin/Compose kodi (`.kt` fayllar) o'zgartirilmaydi
- `SongManager._play()` `StoryManager._play()` bilan parallel pattern ishlatadi: badge hook AVVAL, keyin `super._play(item)`
- T ob'ektiga yangi kalitlar qo'shilganda UZ, RU, EN — uchalasiga ham qo'shilishi shart
- `updateLangUI()` da barcha `getElementById`/`querySelector` `null` tekshiruvidan o'tishi shart (pattern: `const el = document.getElementById('x'); if (el) el.textContent = ...`)
- `openProfilePicker()` fix: `profiles.length <= 1` → `return` — picker ko'rsatilmaydi

---

## File Map

| Harakat | Fayl | Nima o'zgaradi |
|---------|------|----------------|
| Modify | `app/src/main/assets/www/main.js` | `SongManager._play()` override qo'shish |
| Modify | `app/src/main/assets/www/index.html` | T ob'ekti 7 ta yangi kalit (×3 til) + `openProfilePicker` fix + `renderProfilePicker` "Yangi" text + `updateLangUI` i18n blok |

---

## Task 1: main.js — SongManager badge hook

**Files:**
- Modify: `app/src/main/assets/www/main.js:399-403`

**Interfaces:**
- Consumes: `window.badgeManager.onSongPlayed()` — mavjud, `BadgeManager` klassi (line 866)
- Consumes: `ContentManager._play(item)` — mavjud base metod (line 273)
- Produces: `SongManager._play(item)` — qo'shiq o'ynatilganda badge hook chaqiriladi

**Joriy holat (line 399-403):**
```javascript
class SongManager extends ContentManager {
    constructor(player, translator, ui) {
        super('songs', player, translator, ui);
    }
}
```

- [ ] **Step 1: `SongManager._play()` override qo'shish**

`main.js` da `class SongManager` blokini topib, quyidagicha o'zgartirish:

```javascript
class SongManager extends ContentManager {
    constructor(player, translator, ui) {
        super('songs', player, translator, ui);
    }

    _play(item) {
        if (window.badgeManager) badgeManager.onSongPlayed();
        super._play(item);
    }
}
```

`StoryManager._play()` (line 363-396) bilan bir xil pattern: badge hook birinchi, keyin asosiy logika.

- [ ] **Step 2: O'zgarishni tekshirish**

`main.js` faylda `SongManager` bloki (line 399 atrofida) quyidagicha ko'rinishini tekshiring:
```javascript
class SongManager extends ContentManager {
    constructor(player, translator, ui) {
        super('songs', player, translator, ui);
    }

    _play(item) {
        if (window.badgeManager) badgeManager.onSongPlayed();
        super._play(item);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/www/main.js
git commit -m "fix(faza4): SongManager._play badge hook — onSongPlayed wired"
```

---

## Task 2: index.html — i18n + Profile UX fix

**Files:**
- Modify: `app/src/main/assets/www/index.html`

**Interfaces:**
- Consumes: `T` ob'ekti — mavjud, har 3 tilda (line ~1260-1405)
- Consumes: `window.profileManager.getAll()` — mavjud, `ProfileManager` klassi
- Consumes: `window.badgeManager.renderOverlay()` — mavjud, `BadgeManager` klassi
- Produces: `T.uz.profTitle`, `T.uz.profCreateTitle`, va boshqa yangi kalitlar — barcha 3 tilda
- Produces: `openProfilePicker()` — 1 profil bo'lsa ko'rsatmaydi
- Produces: `updateLangUI()` — profile/badge elementlarini yangilaydi

**4 ta o'zgarish ketma-ket bajariladi:**

---

### 2a. T ob'ektiga yangi kalitlar (UZ bloki)

`index.html` da `uz:` blokining oxirini toping. Bu blok quyidagi qatorlar bilan tugaydi (line ~1307-1308):

```javascript
        sqCatAnimals:"Hayvonlar", sqCatDance:"Raqs", sqCatGames:"O'yin",
      },
```

- [ ] **Step 1: UZ blokiga 7 ta yangi kalit qo'shish**

Yuqoridagi qismni quyidagicha almashtiring:

```javascript
        sqCatAnimals:"Hayvonlar", sqCatDance:"Raqs", sqCatGames:"O'yin",
        profTitle:"👶 Kim o'ynaydi?",
        profCreateTitle:"Yangi profil yaratish",
        profNamePlaceholder:"Ism kiriting...",
        profAddNew:"Yangi",
        badgesTitle:"🏅 Mening Yutuqlarim",
        profCancelBtn:"Bekor",
        profCreateBtn:"Yaratish",
      },
```

---

### 2b. RU blokiga yangi kalitlar

`ru:` blokining oxirini toping (line ~1354-1356):

```javascript
        sqCatAnimals:"Животные", sqCatDance:"Танцы", sqCatGames:"Игры",
      },
```

- [ ] **Step 2: RU blokiga 7 ta yangi kalit qo'shish**

```javascript
        sqCatAnimals:"Животные", sqCatDance:"Танцы", sqCatGames:"Игры",
        profTitle:"👶 Кто играет?",
        profCreateTitle:"Создать профиль",
        profNamePlaceholder:"Введите имя...",
        profAddNew:"Новый",
        badgesTitle:"🏅 Мои достижения",
        profCancelBtn:"Отмена",
        profCreateBtn:"Создать",
      },
```

---

### 2c. EN blokiga yangi kalitlar

`en:` blokining oxirini toping (line ~1402-1404):

```javascript
        sqCatAnimals:"Animals", sqCatDance:"Dance", sqCatGames:"Games",
      },
```

- [ ] **Step 3: EN blokiga 7 ta yangi kalit qo'shish**

```javascript
        sqCatAnimals:"Animals", sqCatDance:"Dance", sqCatGames:"Games",
        profTitle:"👶 Who's playing?",
        profCreateTitle:"Create Profile",
        profNamePlaceholder:"Enter name...",
        profAddNew:"New",
        badgesTitle:"🏅 My Achievements",
        profCancelBtn:"Cancel",
        profCreateBtn:"Create",
      },
```

---

### 2d. `openProfilePicker()` UX fix

`openProfilePicker()` funksiyasini toping (line ~1431-1434):

```javascript
    function openProfilePicker() {
      renderProfilePicker();
      document.getElementById('profile-picker').classList.remove('h');
    }
```

- [ ] **Step 4: 1 profil bo'lsa picker ko'rsatmaslik**

```javascript
    function openProfilePicker() {
      var profiles = window.profileManager ? profileManager.getAll() : [];
      if (profiles.length <= 1) return;
      renderProfilePicker();
      document.getElementById('profile-picker').classList.remove('h');
    }
```

---

### 2e. `renderProfilePicker()` — "Yangi" text i18n

`renderProfilePicker()` da "Yangi" hardcoded qatorni toping (line ~1461-1466):

```javascript
      if (profiles.length < 5) {
        var addCard = document.createElement('div');
        addCard.className = 'prof-card prof-add';
        addCard.innerHTML = '<div class="prof-add-icon">➕</div><div class="prof-name">Yangi</div>';
        addCard.onclick = showCreateProfile;
        grid.appendChild(addCard);
      }
```

- [ ] **Step 5: "Yangi" textni i18n qilish**

```javascript
      if (profiles.length < 5) {
        var addCard = document.createElement('div');
        addCard.className = 'prof-card prof-add';
        var addLbl = (window.app && T[app.translator.lang]) ? T[app.translator.lang].profAddNew : 'Yangi';
        addCard.innerHTML = '<div class="prof-add-icon">➕</div><div class="prof-name">' + addLbl + '</div>';
        addCard.onclick = showCreateProfile;
        grid.appendChild(addCard);
      }
```

---

### 2f. `updateLangUI()` — profile + badges i18n blok

`updateLangUI()` funksiyasining oxirini toping (line ~1720-1723):

```javascript
        // Re-render content grids in new language
        app.storyManager?.render();
        app.songManager?.render();
    }
```

- [ ] **Step 6: Profile va badges i18n blok qo'shish**

```javascript
        // Re-render content grids in new language
        app.storyManager?.render();
        app.songManager?.render();

        // Profile picker i18n
        const profT = document.getElementById('prof-title');
        if (profT) profT.textContent = t.profTitle;
        const profCT = document.getElementById('prof-create-title');
        if (profCT) profCT.textContent = t.profCreateTitle;
        const profNI = document.getElementById('prof-name-input');
        if (profNI) profNI.placeholder = t.profNamePlaceholder;
        const profCC = document.querySelector('.prof-btn-secondary');
        if (profCC) profCC.textContent = t.profCancelBtn;
        const profOK = document.querySelector('.prof-btn-primary');
        if (profOK) profOK.textContent = t.profCreateBtn;

        // Badges overlay i18n
        const badgHdr = document.getElementById('badges-hdr-title');
        if (badgHdr) badgHdr.textContent = t.badgesTitle;

        // Re-render badges overlay if open
        const badgeOv = document.getElementById('badges-overlay');
        if (badgeOv && !badgeOv.classList.contains('h') && window.badgeManager) {
            badgeManager.renderOverlay();
        }

        // Re-render profile picker if open
        const profPicker = document.getElementById('profile-picker');
        if (profPicker && !profPicker.classList.contains('h')) {
            renderProfilePicker();
        }
    }
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/assets/www/index.html
git commit -m "fix(faza4): profile+badges i18n, openProfilePicker skip on 1 profile"
```

---

## Task 3: Build, Install va Verify

**Files:** Hech qanday fayl o'zgartirilmaydi

**Interfaces:**
- Consumes: Task 1 + Task 2 o'zgarishlari

- [ ] **Step 1: Build va install**

```powershell
.\gradlew installDebug
```

Kutilgan natija: `BUILD SUCCESSFUL` + APK qurilmaga o'rnatiladi.

- [ ] **Step 2: Song badge tekshiruvi**

1. App ochiladi
2. `🎵 Qo'shiqlar` tabiga o'tish
3. Biror qo'shiqni bosish (audio yo'q bo'lsa ham — card bosilishi kifoya)
4. `song_first` badge popup ekran markazida bounce animatsiya bilan chiqishi kerak
5. Badge `🎵 Birinchi qo'shiq` (UZ) ko'rsatishi kerak
6. `🏅` tugmani bosib badges overlay ochish — `song_first` rangli ko'rinishi kerak

- [ ] **Step 3: Profile picker 1-profil UX tekshiruvi**

1. App yopib qayta ochish
2. Profile picker chiqmasligi kerak (1 profil mavjud)
3. App to'g'ridan-to'g'ri asosiy ekranga o'tishi kerak

- [ ] **Step 4: i18n tekshiruvi**

1. `🌍 EN` → `🌍 RU` ga o'tish
2. Badges overlay (`🏅` tugma) ochish → "🏅 Мои достижения" ko'rinishi kerak
3. `🌍 RU` → `🌍 EN` ga o'tish
4. Badges overlay ochiq bo'lsa — "🏅 My Achievements" ga o'zgarishi kerak

- [ ] **Step 5: Logcat tekshiruvi**

```powershell
adb logcat -s chromium | Select-String -Pattern "Error|error|Uncaught"
```

Kutilgan natija: Hech qanday JS xatosi yo'q.

- [ ] **Step 6: Commit**

```bash
git commit --allow-empty -m "chore(faza4): build verified — badge hook + i18n + UX fix working"
```

Agar Task 3 qo'shimcha o'zgarish talab qilmasa, yuqoridagi `--allow-empty` flag bilan empty commit qilish yoki faqat logcat screenshot olinishi kifoya.
