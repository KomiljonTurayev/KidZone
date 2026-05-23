# Multi-Profile Tizimi Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bir qurilmada 5 tagacha bola profili — har birining yulduzlari, badgelari, tili va o'yin tarixi alohida.

**Architecture:** `ProfileManager` JS klassi `main.js`da barcha localStorage kalitlarini `kz-{profileId}-{suffix}` shaklida boshqaradi. Mavjud `TranslationManager`, `GameManager`, `BadgeManager` kalitlarini `profileManager.key()` orqali oladi. App ochilganda `#profile-picker` overlay chiqadi; profil o'zgarganda `window.location.reload()`.

**Tech Stack:** Vanilla JavaScript, CSS, localStorage; Android Java (`KidWebViewManager`, `ParentalDashboardActivity`).

---

## File Map

| Harakat | Fayl | O'zgarish |
|---------|------|-----------|
| Modify | `app/src/main/assets/www/main.js` | `ProfileManager` klassi; `TranslationManager`, `GameManager`, `BadgeManager` kalitlarini profil-aware qilish; DOMContentLoaded boshlashida `window.profileManager` init; oxirida `openProfilePicker()` |
| Modify | `app/src/main/assets/www/index.html` | 8 ta SVG avatar `<template>`, profil picker CSS, picker HTML, picker JS funksiyalari |
| Modify | `app/src/main/java/uz/kidzone/app/KidWebViewManager.java` | `evaluateJavascript(script, callback)` overload |
| Modify | `app/src/main/java/uz/kidzone/app/MainActivity.java` | `evaluateJs(script, cb)` public metod |
| Modify | `app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java` | "Profillar" bo'limi: profil ro'yxati, qo'shish, o'chirish |

---

## Task 1: ProfileManager klassi

**Files:**
- Modify: `app/src/main/assets/www/main.js`

- [ ] **Step 1: `ProfileManager` klassini `BadgeManager`dan OLDIN qo'shish**

`main.js`da `class BadgeManager {` qatorini toping. Undan OLDIN quyidagi klassni kiriting:

```javascript
class ProfileManager {
    constructor() {
        this._migrate();
        if (!localStorage.getItem('kz-active-profile')) {
            const all = this.getAll();
            if (all.length) localStorage.setItem('kz-active-profile', all[0].id);
        }
    }

    _migrate() {
        if (localStorage.getItem('kz-profiles') !== null) return;
        const KEYS = ['pts','badges','lang','age','game-count','story-count',
                      'song-count','cats-tried','streak-days','cat-off'];
        KEYS.forEach(k => {
            const v = localStorage.getItem('kz-' + k);
            if (v !== null) {
                localStorage.setItem('kz-p1-' + k, v);
                localStorage.removeItem('kz-' + k);
            }
        });
        localStorage.setItem('kz-profiles', JSON.stringify([{id:'p1',name:'Profil 1',avatarIdx:0}]));
        localStorage.setItem('kz-active-profile', 'p1');
    }

    key(suffix) {
        return 'kz-' + (localStorage.getItem('kz-active-profile') || 'p1') + '-' + suffix;
    }

    getActive() {
        const id = localStorage.getItem('kz-active-profile') || 'p1';
        return this.getAll().find(p => p.id === id) || {id, name:'Profil 1', avatarIdx:0};
    }

    getAll() {
        return JSON.parse(localStorage.getItem('kz-profiles') || '[]');
    }

    create(name, avatarIdx) {
        const all = this.getAll();
        if (all.length >= 5) return null;
        const id = 'p' + Date.now();
        all.push({id, name, avatarIdx: parseInt(avatarIdx) || 0});
        localStorage.setItem('kz-profiles', JSON.stringify(all));
        return id;
    }

    rename(id, newName) {
        const all = this.getAll();
        const p = all.find(p => p.id === id);
        if (p) { p.name = newName; localStorage.setItem('kz-profiles', JSON.stringify(all)); }
    }

    remove(id) {
        let all = this.getAll();
        if (all.length <= 1) return;
        all = all.filter(p => p.id !== id);
        localStorage.setItem('kz-profiles', JSON.stringify(all));
        ['pts','badges','lang','age','game-count','story-count',
         'song-count','cats-tried','streak-days','cat-off'].forEach(k =>
            localStorage.removeItem('kz-' + id + '-' + k));
        if (localStorage.getItem('kz-active-profile') === id)
            localStorage.setItem('kz-active-profile', all[0].id);
    }

    switchTo(id) {
        localStorage.setItem('kz-active-profile', id);
        window.location.reload();
    }
}
```

- [ ] **Step 2: `window.profileManager` ni DOMContentLoaded boshiga qo'shish**

`main.js`da `document.addEventListener('DOMContentLoaded'` qatorini toping. Callback funktsiyasining BIRINCHI qatori sifatida:

```javascript
        window.profileManager = new ProfileManager();
```

Agar DOMContentLoaded bloki shunday ko'rinsa:
```javascript
document.addEventListener('DOMContentLoaded', () => {
    const translator = new TranslationManager();
```
Quyidagiga o'zgaradi:
```javascript
document.addEventListener('DOMContentLoaded', () => {
    window.profileManager = new ProfileManager();
    const translator = new TranslationManager();
```

- [ ] **Step 3: DOMContentLoaded oxiriga `openProfilePicker()` qo'shish**

`main.js`da DOMContentLoaded callback'ining yopuvchi `});` dan OLDIN:

```javascript
    openProfilePicker();
```

Masalan, `ui.switchTab(savedTab);` qatoridan keyin (agar u oxirgi bo'lsa), undan keyin `openProfilePicker();` qo'shiladi.

- [ ] **Step 4: Build qilish**

```powershell
.\gradlew :app:assembleDebug 2>&1 | Select-String "error:|BUILD"
```
Kutilgan: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add app/src/main/assets/www/main.js
git commit -m "feat(profiles): ProfileManager class — migration, key(), create/remove/switchTo"
```

---

## Task 2: Mavjud manager kalitlarini profil-aware qilish

**Files:**
- Modify: `app/src/main/assets/www/main.js`

Barcha o'zgarishlar `profileManager.key('suffix')` ishlatish uchun. Har bir o'zgarish uchun Edit tool ishlatiladi.

- [ ] **Step 1: `TranslationManager` — `kz-lang` kalitlarini o'zgartirish**

`TranslationManager` konstruktorida (taxminan line 28):
```javascript
// BEFORE:
this.lang = localStorage.getItem("kz-lang") || "en";
// AFTER:
this.lang = localStorage.getItem(profileManager.key('lang')) || "en";
```

`setLanguage` metodida (taxminan line 33):
```javascript
// BEFORE:
localStorage.setItem("kz-lang", lang);
// AFTER:
localStorage.setItem(profileManager.key('lang'), lang);
```

- [ ] **Step 2: `GameManager` konstruktori — `kz-pts` va `kz-age`**

```javascript
// BEFORE:
this.pts = parseInt(localStorage.getItem("kz-pts") || "0");
this.age = localStorage.getItem("kz-age") || "2-4";
// AFTER:
this.pts = parseInt(localStorage.getItem(profileManager.key('pts')) || "0");
this.age = localStorage.getItem(profileManager.key('age')) || "2-4";
```

- [ ] **Step 3: `GameManager.addPoints` — `kz-pts` yozish**

```javascript
// BEFORE:
localStorage.setItem("kz-pts", this.pts);
// AFTER:
localStorage.setItem(profileManager.key('pts'), this.pts);
```

- [ ] **Step 4: `GameManager.setAge` — `kz-age` yozish**

```javascript
// BEFORE:
localStorage.setItem("kz-age", a);
// AFTER:
localStorage.setItem(profileManager.key('age'), a);
```

- [ ] **Step 5: `BadgeManager` konstruktori — `kz-badges`**

```javascript
// BEFORE:
this._badges = JSON.parse(localStorage.getItem('kz-badges') || '[]');
// AFTER:
this._badges = JSON.parse(localStorage.getItem(profileManager.key('badges')) || '[]');
```

- [ ] **Step 6: `BadgeManager.awardBadge` — `kz-badges` yozish**

```javascript
// BEFORE:
localStorage.setItem('kz-badges', JSON.stringify(this._badges));
// AFTER:
localStorage.setItem(profileManager.key('badges'), JSON.stringify(this._badges));
```

- [ ] **Step 7: `BadgeManager.onGamePlayed` — `kz-game-count` va `kz-cats-tried`**

```javascript
// BEFORE:
const n = parseInt(localStorage.getItem('kz-game-count') || '0') + 1;
localStorage.setItem('kz-game-count', String(n));
const cats = JSON.parse(localStorage.getItem('kz-cats-tried') || '[]');
// ... (cats.push va agar o'zgarganda)
localStorage.setItem('kz-cats-tried', JSON.stringify(cats));
// AFTER:
const n = parseInt(localStorage.getItem(profileManager.key('game-count')) || '0') + 1;
localStorage.setItem(profileManager.key('game-count'), String(n));
const cats = JSON.parse(localStorage.getItem(profileManager.key('cats-tried')) || '[]');
// ... (cats.push logic o'zgarmaydi)
localStorage.setItem(profileManager.key('cats-tried'), JSON.stringify(cats));
```

- [ ] **Step 8: `BadgeManager.onStoryPlayed` — `kz-story-count`**

```javascript
// BEFORE:
const n = parseInt(localStorage.getItem('kz-story-count') || '0') + 1;
localStorage.setItem('kz-story-count', String(n));
// AFTER:
const n = parseInt(localStorage.getItem(profileManager.key('story-count')) || '0') + 1;
localStorage.setItem(profileManager.key('story-count'), String(n));
```

- [ ] **Step 9: `BadgeManager.onSongPlayed` — `kz-song-count`**

```javascript
// BEFORE:
const n = parseInt(localStorage.getItem('kz-song-count') || '0') + 1;
localStorage.setItem('kz-song-count', String(n));
// AFTER:
const n = parseInt(localStorage.getItem(profileManager.key('song-count')) || '0') + 1;
localStorage.setItem(profileManager.key('song-count'), String(n));
```

- [ ] **Step 10: `BadgeManager.checkStreak` — `kz-streak-days`**

```javascript
// BEFORE:
const days = JSON.parse(localStorage.getItem('kz-streak-days') || '[]');
// ... (days yangilanadi)
localStorage.setItem('kz-streak-days', JSON.stringify(days));
// AFTER:
const days = JSON.parse(localStorage.getItem(profileManager.key('streak-days')) || '[]');
// ... (days yangilanadi — o'zgarmaydi)
localStorage.setItem(profileManager.key('streak-days'), JSON.stringify(days));
```

- [ ] **Step 11: Build qilish**

```powershell
.\gradlew :app:assembleDebug 2>&1 | Select-String "error:|BUILD"
```
Kutilgan: `BUILD SUCCESSFUL`

- [ ] **Step 12: Commit**

```
git add app/src/main/assets/www/main.js
git commit -m "feat(profiles): update all managers to use profile-aware localStorage keys"
```

---

## Task 3: Profil Picker CSS + SVG Avatar Templatelar

**Files:**
- Modify: `app/src/main/assets/www/index.html`

- [ ] **Step 1: CSS qo'shish `</style>` dan oldin**

```css
    /* ── Profile Picker ───────────────────────────────── */
    #profile-picker{
      position:fixed;inset:0;background:#FFF8F0;
      z-index:9000;display:flex;flex-direction:column;
      align-items:center;justify-content:center;padding:24px;
    }
    #profile-picker.h{display:none}
    #prof-title{
      font-size:22px;font-weight:700;color:#222;
      font-family:'Fredoka One',cursive;margin-bottom:24px;text-align:center;
    }
    #prof-grid{
      display:grid;grid-template-columns:repeat(3,1fr);
      gap:12px;width:100%;max-width:360px;
    }
    .prof-card{
      background:#fff;border-radius:20px;padding:14px 8px;
      text-align:center;cursor:pointer;
      box-shadow:0 2px 10px rgba(0,0,0,.1);
    }
    .prof-card:active{transform:scale(0.95)}
    .prof-card.prof-active{border:2px solid #FF6B35}
    .prof-av{width:56px;height:56px;margin:0 auto 8px;display:block}
    .prof-av svg{width:100%;height:100%}
    .prof-name{font-size:12px;font-weight:700;color:#222}
    .prof-pts{font-size:11px;color:#FF6B35;margin-top:2px}
    .prof-add{border:2px dashed #CCC;box-shadow:none;background:transparent}
    .prof-add-icon{font-size:28px;margin-bottom:4px;color:#CCC}
    #prof-create{width:100%;max-width:360px}
    #prof-create.h{display:none}
    #prof-create-title{
      font-size:16px;font-weight:700;color:#222;
      margin-bottom:14px;text-align:center;
    }
    #prof-name-input{
      width:100%;padding:12px;border:2px solid #EEE;border-radius:12px;
      font-size:16px;margin-bottom:14px;box-sizing:border-box;
      font-family:'Fredoka One',cursive;
    }
    #av-picker{
      display:grid;grid-template-columns:repeat(4,1fr);
      gap:10px;margin-bottom:16px;
    }
    .av-opt{
      width:56px;height:56px;cursor:pointer;border-radius:50%;
      border:3px solid transparent;padding:2px;box-sizing:border-box;
    }
    .av-opt.selected{border-color:#FF6B35}
    .av-opt svg{width:100%;height:100%;border-radius:50%}
    #prof-create-btns{display:flex;gap:10px}
    .prof-btn{
      flex:1;padding:12px;border:none;border-radius:12px;
      font-size:14px;font-weight:700;cursor:pointer;
    }
    .prof-btn-primary{background:#FF6B35;color:#fff}
    .prof-btn-secondary{background:#EEE;color:#555}
    #prof-grid-wrap.h{display:none}
```

- [ ] **Step 2: 8 ta avatar `<template>` ni `<body>` tegidan KEYIN (birinchi element sifatida) qo'shish**

`<body>` tegidan keyin, barcha boshqa contentdan OLDIN:

```html
<!-- Avatar templates -->
<template id="av-tpl-0">
<svg viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="40" cy="38" rx="26" ry="24" fill="#8B4513"/>
  <circle cx="40" cy="46" r="22" fill="#FFDBB4"/>
  <ellipse cx="30" cy="17" rx="9" ry="6" fill="#FF69B4"/>
  <ellipse cx="50" cy="17" rx="9" ry="6" fill="#FF69B4"/>
  <circle cx="40" cy="17" r="4" fill="#FF1493"/>
  <circle cx="33" cy="43" r="3.5" fill="#222"/>
  <circle cx="47" cy="43" r="3.5" fill="#222"/>
  <circle cx="34" cy="42" r="1.2" fill="#fff"/>
  <circle cx="48" cy="42" r="1.2" fill="#fff"/>
  <circle cx="27" cy="50" r="5" fill="#FFB6C1" opacity="0.5"/>
  <circle cx="53" cy="50" r="5" fill="#FFB6C1" opacity="0.5"/>
  <path d="M33 53 Q40 59 47 53" stroke="#555" stroke-width="1.5" fill="none" stroke-linecap="round"/>
</svg>
</template>
<template id="av-tpl-1">
<svg viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="40" cy="38" rx="26" ry="24" fill="#1C1C1C"/>
  <circle cx="40" cy="46" r="22" fill="#FFDBB4"/>
  <ellipse cx="30" cy="17" rx="9" ry="6" fill="#FFD700"/>
  <ellipse cx="50" cy="17" rx="9" ry="6" fill="#FFD700"/>
  <circle cx="40" cy="17" r="4" fill="#FFA500"/>
  <circle cx="33" cy="43" r="3.5" fill="#222"/>
  <circle cx="47" cy="43" r="3.5" fill="#222"/>
  <circle cx="34" cy="42" r="1.2" fill="#fff"/>
  <circle cx="48" cy="42" r="1.2" fill="#fff"/>
  <circle cx="27" cy="50" r="5" fill="#FFB6C1" opacity="0.5"/>
  <circle cx="53" cy="50" r="5" fill="#FFB6C1" opacity="0.5"/>
  <path d="M33 53 Q40 59 47 53" stroke="#555" stroke-width="1.5" fill="none" stroke-linecap="round"/>
</svg>
</template>
<template id="av-tpl-2">
<svg viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="40" cy="38" rx="26" ry="24" fill="#DAA520"/>
  <circle cx="40" cy="46" r="22" fill="#F0B87A"/>
  <ellipse cx="30" cy="17" rx="9" ry="6" fill="#4169E1"/>
  <ellipse cx="50" cy="17" rx="9" ry="6" fill="#4169E1"/>
  <circle cx="40" cy="17" r="4" fill="#0000CD"/>
  <circle cx="33" cy="43" r="3.5" fill="#222"/>
  <circle cx="47" cy="43" r="3.5" fill="#222"/>
  <circle cx="34" cy="42" r="1.2" fill="#fff"/>
  <circle cx="48" cy="42" r="1.2" fill="#fff"/>
  <circle cx="27" cy="50" r="5" fill="#FFB6C1" opacity="0.5"/>
  <circle cx="53" cy="50" r="5" fill="#FFB6C1" opacity="0.5"/>
  <path d="M33 53 Q40 59 47 53" stroke="#555" stroke-width="1.5" fill="none" stroke-linecap="round"/>
</svg>
</template>
<template id="av-tpl-3">
<svg viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="40" cy="38" rx="26" ry="24" fill="#3B1A08"/>
  <circle cx="40" cy="46" r="22" fill="#C68642"/>
  <ellipse cx="30" cy="17" rx="9" ry="6" fill="#9B59B6"/>
  <ellipse cx="50" cy="17" rx="9" ry="6" fill="#9B59B6"/>
  <circle cx="40" cy="17" r="4" fill="#6C3483"/>
  <circle cx="33" cy="43" r="3.5" fill="#222"/>
  <circle cx="47" cy="43" r="3.5" fill="#222"/>
  <circle cx="34" cy="42" r="1.2" fill="#fff"/>
  <circle cx="48" cy="42" r="1.2" fill="#fff"/>
  <circle cx="27" cy="50" r="5" fill="#FFB6C1" opacity="0.5"/>
  <circle cx="53" cy="50" r="5" fill="#FFB6C1" opacity="0.5"/>
  <path d="M33 53 Q40 59 47 53" stroke="#555" stroke-width="1.5" fill="none" stroke-linecap="round"/>
</svg>
</template>
<template id="av-tpl-4">
<svg viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="40" cy="30" rx="26" ry="16" fill="#8B4513"/>
  <circle cx="40" cy="46" r="22" fill="#FFDBB4"/>
  <path d="M26 30 Q40 14 54 30" fill="#8B4513"/>
  <circle cx="33" cy="43" r="3.5" fill="#222"/>
  <circle cx="47" cy="43" r="3.5" fill="#222"/>
  <circle cx="34" cy="42" r="1.2" fill="#fff"/>
  <circle cx="48" cy="42" r="1.2" fill="#fff"/>
  <circle cx="27" cy="50" r="4" fill="#FFB6C1" opacity="0.3"/>
  <circle cx="53" cy="50" r="4" fill="#FFB6C1" opacity="0.3"/>
  <path d="M33 53 Q40 59 47 53" stroke="#555" stroke-width="1.5" fill="none" stroke-linecap="round"/>
</svg>
</template>
<template id="av-tpl-5">
<svg viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="40" cy="30" rx="26" ry="16" fill="#1C1C1C"/>
  <circle cx="40" cy="46" r="22" fill="#FFDBB4"/>
  <path d="M26 30 Q40 14 54 30" fill="#1C1C1C"/>
  <circle cx="33" cy="43" r="3.5" fill="#222"/>
  <circle cx="47" cy="43" r="3.5" fill="#222"/>
  <circle cx="34" cy="42" r="1.2" fill="#fff"/>
  <circle cx="48" cy="42" r="1.2" fill="#fff"/>
  <circle cx="27" cy="50" r="4" fill="#FFB6C1" opacity="0.3"/>
  <circle cx="53" cy="50" r="4" fill="#FFB6C1" opacity="0.3"/>
  <path d="M33 53 Q40 59 47 53" stroke="#555" stroke-width="1.5" fill="none" stroke-linecap="round"/>
</svg>
</template>
<template id="av-tpl-6">
<svg viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="40" cy="30" rx="26" ry="16" fill="#DAA520"/>
  <circle cx="40" cy="46" r="22" fill="#F0B87A"/>
  <path d="M26 30 Q40 14 54 30" fill="#DAA520"/>
  <circle cx="33" cy="43" r="3.5" fill="#222"/>
  <circle cx="47" cy="43" r="3.5" fill="#222"/>
  <circle cx="34" cy="42" r="1.2" fill="#fff"/>
  <circle cx="48" cy="42" r="1.2" fill="#fff"/>
  <circle cx="27" cy="50" r="4" fill="#FFB6C1" opacity="0.3"/>
  <circle cx="53" cy="50" r="4" fill="#FFB6C1" opacity="0.3"/>
  <path d="M33 53 Q40 59 47 53" stroke="#555" stroke-width="1.5" fill="none" stroke-linecap="round"/>
</svg>
</template>
<template id="av-tpl-7">
<svg viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg">
  <ellipse cx="40" cy="30" rx="26" ry="16" fill="#222"/>
  <circle cx="40" cy="46" r="22" fill="#C68642"/>
  <path d="M26 30 Q40 14 54 30" fill="#222"/>
  <circle cx="33" cy="43" r="3.5" fill="#111"/>
  <circle cx="47" cy="43" r="3.5" fill="#111"/>
  <circle cx="34" cy="42" r="1.2" fill="#fff"/>
  <circle cx="48" cy="42" r="1.2" fill="#fff"/>
  <circle cx="27" cy="50" r="4" fill="#FFB6C1" opacity="0.3"/>
  <circle cx="53" cy="50" r="4" fill="#FFB6C1" opacity="0.3"/>
  <path d="M33 53 Q40 59 47 53" stroke="#333" stroke-width="1.5" fill="none" stroke-linecap="round"/>
</svg>
</template>
```

- [ ] **Step 3: Build qilish**

```powershell
.\gradlew :app:assembleDebug 2>&1 | Select-String "error:|BUILD"
```
Kutilgan: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```
git add app/src/main/assets/www/index.html
git commit -m "feat(profiles): profile picker CSS + 8 SVG avatar templates"
```

---

## Task 4: Profil Picker HTML va JavaScript

**Files:**
- Modify: `app/src/main/assets/www/index.html`

- [ ] **Step 1: Picker HTML ni `<!-- Badges overlay -->` commentdan OLDIN qo'shish**

```html
  <!-- Profile picker -->
  <div id="profile-picker" class="h">
    <div id="prof-title">👶 Kim o'ynaydi?</div>
    <div id="prof-grid-wrap">
      <div id="prof-grid"></div>
    </div>
    <div id="prof-create" class="h">
      <div id="prof-create-title">Yangi profil yaratish</div>
      <input id="prof-name-input" type="text" placeholder="Ism kiriting..." maxlength="12"/>
      <div id="av-picker"></div>
      <div id="prof-create-btns">
        <button class="prof-btn prof-btn-secondary" onclick="cancelCreateProfile()">Bekor</button>
        <button class="prof-btn prof-btn-primary" onclick="confirmCreateProfile()">Yaratish</button>
      </div>
    </div>
  </div>
```

- [ ] **Step 2: Profil picker JS funksiyalarini `openBadges` funksiyasi yoniga qo'shish**

`index.html`dagi `function openBadges()` funksiyasini toping. Undan KEYIN qo'shing:

```javascript
    function openProfilePicker() {
      renderProfilePicker();
      document.getElementById('profile-picker').classList.remove('h');
    }

    function renderProfilePicker() {
      const profiles = window.profileManager ? profileManager.getAll() : [];
      const active = window.profileManager ? profileManager.getActive() : {id:''};
      const grid = document.getElementById('prof-grid');
      if (!grid) return;
      grid.innerHTML = '';
      profiles.forEach(function(p) {
        const pts = localStorage.getItem('kz-' + p.id + '-pts') || '0';
        const card = document.createElement('div');
        card.className = 'prof-card' + (p.id === active.id ? ' prof-active' : '');
        const avEl = document.createElement('div');
        avEl.className = 'prof-av';
        avEl.innerHTML = getAvatarSvg(p.avatarIdx);
        const nmEl = document.createElement('div');
        nmEl.className = 'prof-name';
        nmEl.textContent = p.name;
        const ptEl = document.createElement('div');
        ptEl.className = 'prof-pts';
        ptEl.textContent = '⭐ ' + pts;
        card.appendChild(avEl);
        card.appendChild(nmEl);
        card.appendChild(ptEl);
        card.onclick = function() { selectProfile(p.id); };
        grid.appendChild(card);
      });
      if (profiles.length < 5) {
        const addCard = document.createElement('div');
        addCard.className = 'prof-card prof-add';
        addCard.innerHTML = '<div class="prof-add-icon">➕</div><div class="prof-name">Yangi</div>';
        addCard.onclick = showCreateProfile;
        grid.appendChild(addCard);
      }
    }

    function selectProfile(id) {
      if (!window.profileManager) return;
      if (id === profileManager.getActive().id) {
        document.getElementById('profile-picker').classList.add('h');
      } else {
        profileManager.switchTo(id);
      }
    }

    var _selectedAvatarIdx = 0;

    function showCreateProfile() {
      _selectedAvatarIdx = 0;
      document.getElementById('prof-grid-wrap').classList.add('h');
      document.getElementById('prof-create').classList.remove('h');
      document.getElementById('prof-name-input').value = '';
      renderAvatarPicker();
    }

    function cancelCreateProfile() {
      document.getElementById('prof-create').classList.add('h');
      document.getElementById('prof-grid-wrap').classList.remove('h');
    }

    function renderAvatarPicker() {
      const wrap = document.getElementById('av-picker');
      if (!wrap) return;
      wrap.innerHTML = '';
      for (var i = 0; i < 8; i++) {
        (function(idx) {
          const opt = document.createElement('div');
          opt.className = 'av-opt' + (idx === _selectedAvatarIdx ? ' selected' : '');
          opt.innerHTML = getAvatarSvg(idx);
          opt.onclick = function() { _selectedAvatarIdx = idx; renderAvatarPicker(); };
          wrap.appendChild(opt);
        })(i);
      }
    }

    function confirmCreateProfile() {
      const name = (document.getElementById('prof-name-input').value || '').trim();
      if (!name || !window.profileManager) return;
      profileManager.create(name, _selectedAvatarIdx);
      document.getElementById('prof-create').classList.add('h');
      document.getElementById('prof-grid-wrap').classList.remove('h');
      renderProfilePicker();
    }

    function getAvatarSvg(idx) {
      const t = document.getElementById('av-tpl-' + idx);
      return t ? t.innerHTML : '👤';
    }
```

- [ ] **Step 3: Build qilish**

```powershell
.\gradlew :app:assembleDebug 2>&1 | Select-String "error:|BUILD"
```
Kutilgan: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```
git add app/src/main/assets/www/index.html
git commit -m "feat(profiles): profile picker HTML + JS — openProfilePicker, create, select"
```

---

## Task 5: Parental Dashboard — Profillar bo'limi

**Files:**
- Modify: `app/src/main/java/uz/kidzone/app/KidWebViewManager.java`
- Modify: `app/src/main/java/uz/kidzone/app/MainActivity.java`
- Modify: `app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java`

- [ ] **Step 1: `KidWebViewManager`ga callback'li `evaluateJavascript` qo'shish**

`KidWebViewManager.java`da mavjud `evaluateJavascript(String script)` metodidan KEYIN:

```java
    public void evaluateJavascript(String script, android.webkit.ValueCallback<String> cb) {
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript(script, cb));
        }
    }
```

- [ ] **Step 2: `MainActivity`ga `evaluateJs` public metodi qo'shish**

`MainActivity.java`da `void injectJs(String script)` metodidan KEYIN:

```java
    public void evaluateJs(String script, android.webkit.ValueCallback<String> cb) {
        if (webViewManager != null) webViewManager.evaluateJavascript(script, cb);
    }
```

- [ ] **Step 3: `ParentalDashboardActivity`ga profillar bo'limini qo'shish**

`ParentalDashboardActivity.java`da mavjud fieldlar (`tvToday`, `chartLayout`, ...) ro'yxatiga qo'shish:

```java
    private LinearLayout profilesLayout;
```

`bindViews()` metodiga qo'shish:

```java
        profilesLayout = findViewById(R.id.pd_profiles);
```

`onCreate()`da `loadStats()` chaqiruvidan OLDIN:

```java
        loadProfiles();
```

Yangi `loadProfiles()` metodi (mavjud `loadStats()` metodidan KEYIN):

```java
    private void loadProfiles() {
        MainActivity main = MainActivity.instance != null ? MainActivity.instance.get() : null;
        if (main == null) return;
        main.evaluateJs(
            "JSON.stringify(window.profileManager ? profileManager.getAll() : [])",
            value -> runOnUiThread(() -> renderProfiles(value))
        );
    }

    private void renderProfiles(String json) {
        profilesLayout.removeAllViews();
        // json from evaluateJavascript is wrapped in quotes: "\"[...]\"" — unescape it
        if (json == null || json.equals("null")) return;
        // Strip surrounding JS string quotes if present
        if (json.startsWith("\"")) json = json.substring(1, json.length() - 1).replace("\\\"", "\"");

        try {
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject p = arr.getJSONObject(i);
                String id   = p.getString("id");
                String name = p.getString("name");
                profilesLayout.addView(buildProfileRow(id, name));
            }
        } catch (org.json.JSONException e) { /* ignore */ }

        if (profilesLayout.getChildCount() < 5) {
            MaterialButton addBtn = new MaterialButton(this);
            addBtn.setText("+ Profil qo'shish");
            addBtn.setOnClickListener(v -> showAddProfileDialog());
            profilesLayout.addView(addBtn);
        }
    }

    private View buildProfileRow(String id, String name) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        TextView label = new TextView(this);
        label.setText("👤  " + name);
        label.setTextSize(14f);
        label.setTextColor(0xFF222222);
        label.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        MaterialButton delBtn = new MaterialButton(this);
        delBtn.setText("O'chirish");
        delBtn.setTextSize(12f);
        delBtn.setOnClickListener(v -> confirmDeleteProfile(id, name));

        row.addView(label);
        row.addView(delBtn);
        return row;
    }

    private void showAddProfileDialog() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
        b.setTitle("Yangi profil");
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Ism kiriting");
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        b.setView(et);
        b.setPositiveButton("Yaratish", (d, w) -> {
            String name = et.getText().toString().trim();
            if (name.isEmpty()) return;
            injectJs("if(window.profileManager) profileManager.create('" +
                    name.replace("'", "\\'") + "', 0)");
            profilesLayout.postDelayed(this::loadProfiles, 300);
        });
        b.setNegativeButton("Bekor", null);
        b.show();
    }

    private void confirmDeleteProfile(String id, String name) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Profilni o'chirish")
            .setMessage("\"" + name + "\" profilini o'chirishni tasdiqlaysizmi?")
            .setPositiveButton("O'chirish", (d, w) -> {
                injectJs("if(window.profileManager) profileManager.remove('" + id + "')");
                profilesLayout.postDelayed(this::loadProfiles, 300);
            })
            .setNegativeButton("Bekor", null)
            .show();
    }
```

- [ ] **Step 4: `pd_profiles` view ni layout faylga qo'shish**

`app/src/main/res/layout/activity_parental_dashboard.xml` faylini oching. Birinchi `<LinearLayout>` yoki bo'limdan OLDIN (masalan, mavjud `pd_today_minutes`dan oldin) qo'shish:

```xml
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="👤 Profillar"
        android:textSize="16sp"
        android:textStyle="bold"
        android:textColor="#222222"
        android:layout_marginBottom="8dp"/>

    <LinearLayout
        android:id="@+id/pd_profiles"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:layout_marginBottom="16dp">
    </LinearLayout>
```

- [ ] **Step 5: Build qilish**

```powershell
.\gradlew :app:assembleDebug 2>&1 | Select-String "error:|BUILD"
```
Kutilgan: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```
git add app/src/main/java/uz/kidzone/app/KidWebViewManager.java
git add app/src/main/java/uz/kidzone/app/MainActivity.java
git add app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java
git add app/src/main/res/layout/activity_parental_dashboard.xml
git commit -m "feat(profiles): parental dashboard profiles section — list, add, delete"
```

---

## Task 6: Build, O'rnatish va Smoke Test

**Files:** Hech qaysi fayl o'zgarmaydi.

- [ ] **Step 1: APK o'rnatish**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Kutilgan: `Success`

- [ ] **Step 2: Ilovani ishga tushirish**

```powershell
& $adb shell am start -n uz.kidzone.app.debug/uz.kidzone.app.MainActivity
```

- [ ] **Step 3: Smoke test checklist (qurilmada qo'lda)**

1. **Profil picker chiqadi:** App ochilganda to'liq ekran `👶 Kim o'ynaydi?` overlay ko'rinadi
2. **Mavjud profil:** `Profil 1` kartochkasi avatar + ism bilan ko'rinadi
3. **Profil tanlash:** `Profil 1` ga bosing → overlay yopiladi, app ishlaydi
4. **Yangi profil yaratish:** `➕ Yangi` kartochkaga bosing → ism kiritish + avatar tanlash chiqadi → `Yaratish` → yangi profil ro'yxatda ko'rinadi
5. **Profil almashtirish:** 2 ta profil bo'lganda boshqasiga bosing → app reload bo'ladi, yangi profil ishlaydi
6. **Alohida yulduzlar:** Profil 1 da 50 ball to'plang → Profil 2 ga o'ting → 0 ball ko'rinadi → Profil 1 ga qaytganda 50 ball saqlangan
7. **Alohida badgelar:** Profil 1 da `game_first` badge oling → Profil 2 ga o'ting → badge ko'rinmaydi
8. **Alohida til:** Profil 1 da UZ, Profil 2 da RU → har biri o'z tilini saqlaydi
9. **Migration:** Ilova birinchi marta yangilanganda eski ma'lumotlar `Profil 1`ga ko'chirilgan (yangi o'rnatishda test qilish mumkin emas, lekin migration kodi tekshirilishi mumkin)
10. **Parental Dashboard:** 3 sekund long-press → PIN → Dashboard → Profillar bo'limi ko'rinadi, profil qo'shish/o'chirish ishlaydi

- [ ] **Step 4: Xatolar bo'lsa tuzatish va commit**

```
git add app/src/main/assets/www/main.js app/src/main/assets/www/index.html
git add app/src/main/java/uz/kidzone/app/
git commit -m "fix(profiles): smoke test fixes"
```

- [ ] **Step 5: Push**

```
git push origin master
```
