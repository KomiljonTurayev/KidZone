# Faza 4: Badges & Profile Polish — Dizayn Spesifikatsiyasi

**Sana:** 2026-06-22
**Holat:** Approved

---

## Maqsad

Faza 3 dan keyin Achievements/Badges va Multi-Profile implementatsiyasida qolgan 4 ta muammoni tuzatish:
1 ta Bug, 1 ta UX muammo, 1 ta i18n Bug, 1 ta Minor.

---

## Joriy holat

`main.js` va `index.html` da allaqachon implementatsiya qilingan:
- `ProfileManager` klassi (migrate, key, create/remove/switchTo)
- `BadgeManager` klassi (16 badge, popup queue, overlay render)
- Profile picker HTML/CSS (8 avatar, create flow)
- Badges overlay HTML/CSS (grid, progress)
- `openBadges()`, `openProfilePicker()` funksiyalari
- `badgeManager.onGamePlayed()` — `GameManager.closeGame()` da hooked ✅
- `badgeManager.onStoryPlayed()` — `StoryManager._play()` da hooked ✅
- `badgeManager.onPointsUpdated()` — `GameManager.addPoints()` da hooked ✅

---

## Audit Topilmalari

| # | Muammo | Darajasi | Fayl |
|---|--------|----------|------|
| 1 | `SongManager._play()` da `badgeManager.onSongPlayed()` yo'q | Bug | `main.js` |
| 2 | Profile picker har safar app ochilganda chiqadi (1 profil bo'lsa ham) | UX | `index.html` |
| 3 | Profile picker + badges overlay i18n yo'q | i18n Bug | `index.html` |
| 4 | `badgeManager.renderOverlay()` til o'zgarganda chaqirilmaydi | Minor | `index.html` |

---

## O'zgarishlar

### Faqat 2 fayl o'zgaradi

| Fayl | Nima |
|------|------|
| `app/src/main/assets/www/main.js` | SongManager._play() override |
| `app/src/main/assets/www/index.html` | i18n strings + openProfilePicker fix + updateLangUI |

---

## Task 1 — `main.js`: SongManager badge hook

`SongManager` klassi (line 399-403) `_play()` override qo'shiladi:

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

`StoryManager._play()` bilan parallel pattern — badge hook avval, keyin `super._play()`.

---

## Task 2 — `index.html`: i18n + UX fix

### 2a. T ob'ektiga yangi kalitlar

Har 3 tilda (`uz`, `ru`, `en`) qo'shiladi:

| Kalit | UZ | RU | EN |
|-------|----|----|-----|
| `profTitle` | `"👶 Kim o'ynaydi?"` | `"👶 Кто играет?"` | `"👶 Who's playing?"` |
| `profCreateTitle` | `"Yangi profil yaratish"` | `"Создать профиль"` | `"Create Profile"` |
| `profNamePlaceholder` | `"Ism kiriting..."` | `"Введите имя..."` | `"Enter name..."` |
| `profAddNew` | `"Yangi"` | `"Новый"` | `"New"` |
| `badgesTitle` | `"🏅 Mening Yutuqlarim"` | `"🏅 Мои достижения"` | `"🏅 My Achievements"` |
| `profCancelBtn` | `"Bekor"` | `"Отмена"` | `"Cancel"` |
| `profCreateBtn` | `"Yaratish"` | `"Создать"` | `"Create"` |

### 2b. `openProfilePicker()` UX fix

```javascript
function openProfilePicker() {
    var profiles = window.profileManager ? profileManager.getAll() : [];
    if (profiles.length <= 1) return;   // 1 profil — picker ko'rsatma
    renderProfilePicker();
    document.getElementById('profile-picker').classList.remove('h');
}
```

Faqat 2+ profil bo'lganda picker ochiladi. 1 profil — avtomatik faol, picker chiqmaydi.

### 2c. `renderProfilePicker()` — "Yangi" text i18n

```javascript
addCard.querySelector('.prof-name').textContent =
    T[window.app?.translator?.lang || 'uz']?.profAddNew || 'Yangi';
```

### 2d. `updateLangUI()` — profile + badge elementlarini yangilash

`updateLangUI()` funksiyasiga qo'shimcha (Songs tab labels blokidan keyin):

```javascript
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
```

---

## Muvaffaqiyat mezonlari

- [ ] Qo'shiq tinglaganda `song_first` badge popup chiqadi
- [ ] 10 ta qo'shiq tinglaganda `song_10` badge chiqadi
- [ ] 1 profil bo'lganda app ochilganda picker chiqmaydi
- [ ] 2+ profil bo'lganda picker avvalgidek chiqadi
- [ ] RU da app ochilganda profile picker matnlari ruscha
- [ ] EN da badges overlay "My Achievements" ko'rsatadi
- [ ] Til o'zgarganda badges overlay ochiq bo'lsa — avtomatik yangilanadi
- [ ] Build muvaffaqiyatli — hech qanday JS xatosi yo'q
