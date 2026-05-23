# Achievements — Badge Tizimi Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mavjud yulduzcha tizimiga 16 ta badge qo'shish — badge sahifasi (`index.html` overlay) va badge olinganida animatsiyali popup.

**Architecture:** `BadgeManager` JS klassi `main.js`da, `BADGE_DEFS` konstanti `index.html`da. Barcha ma'lumotlar `localStorage`da. Mavjud `GameManager.closeGame()`, `addPoints()`, `StoryManager._play()`, `SongManager._play()` metodlariga hook qilib ulanadi. Popup `#badge-popup` div, badge sahifasi `#badges-overlay` div.

**Tech Stack:** Vanilla JavaScript, CSS animations, localStorage. Native Android kodi o'zgartirilmaydi.

---

## File Map

| Harakat | Fayl | Nima o'zgaradi |
|---------|------|----------------|
| Modify | `app/src/main/assets/www/main.js` | `BADGE_DEFS`, `BadgeManager` klassi, hook ulanishlari |
| Modify | `app/src/main/assets/www/index.html` | Popup HTML/CSS, overlay HTML/CSS, header tugma, `openBadges()` |

---

## Task 1: BADGE_DEFS va BadgeManager klassi

**Files:**
- Modify: `app/src/main/assets/www/main.js`

- [ ] **Step 1: `BADGE_DEFS` konstantini `main.js` boshiga qo'shish**

`main.js`ning eng boshiga (1-qatordan oldin) qo'shiladi:

```javascript
const BADGE_DEFS = [
    {id:'game_first', emoji:'🎮', name:{uz:'Birinchi qadam',  ru:'Первый шаг',          en:'First Step'}},
    {id:'game_5',     emoji:'🕹️', name:{uz:"O'yinchi",        ru:'Игрок',               en:'Gamer'}},
    {id:'game_25',    emoji:'🏆', name:{uz:"Pro o'yinchi",    ru:'Про игрок',           en:'Pro Gamer'}},
    {id:'game_50',    emoji:'🌟', name:{uz:'Ustoz',           ru:'Мастер',              en:'Master'}},
    {id:'story_first',emoji:'📖', name:{uz:'Birinchi ertak',  ru:'Первая сказка',       en:'First Story'}},
    {id:'story_10',   emoji:'📚', name:{uz:'Kitobxon',        ru:'Книголюб',            en:'Book Lover'}},
    {id:'song_first', emoji:'🎵', name:{uz:"Birinchi qo'shiq",ru:'Первая песня',        en:'First Song'}},
    {id:'song_10',    emoji:'🎶', name:{uz:"Musiqa sevari",   ru:'Меломан',             en:'Music Lover'}},
    {id:'cat_3',      emoji:'🌍', name:{uz:'Izlovchi',        ru:'Исследователь',       en:'Explorer'}},
    {id:'cat_all',    emoji:'🏅', name:{uz:'Tadqiqotchi',     ru:'Всесторонний',        en:'All-Around'}},
    {id:'stars_100',  emoji:'⭐', name:{uz:'Yulduzcha',       ru:'Звёздочка',           en:'Star'}},
    {id:'stars_500',  emoji:'🌟', name:{uz:'Porloq yulduz',   ru:'Яркая звезда',        en:'Bright Star'}},
    {id:'stars_1000', emoji:'💫', name:{uz:'Super yulduz',    ru:'Суперзвезда',         en:'Super Star'}},
    {id:'stars_5000', emoji:'🏆', name:{uz:"Yulduzlar qiroli",ru:'Король звёзд',        en:'Star King'}},
    {id:'streak_3',   emoji:'🔥', name:{uz:'Uch kun',         ru:'Три дня подряд',      en:'Three Days'}},
    {id:'streak_7',   emoji:'🔥🔥',name:{uz:'Bir hafta',      ru:'Целая неделя',        en:'One Week'}},
];
```

- [ ] **Step 2: `BadgeManager` klassi qo'shish**

`main.js`da `let app;` qatoridan OLDIN qo'shiladi (taxminan 808-qator atrofida):

```javascript
class BadgeManager {
    constructor() {
        this._badges = JSON.parse(localStorage.getItem('kz-badges') || '[]');
        this._queue = [];
        this._busy = false;
    }

    isEarned(id) { return this._badges.includes(id); }

    awardBadge(id) {
        if (this.isEarned(id)) return;
        this._badges.push(id);
        localStorage.setItem('kz-badges', JSON.stringify(this._badges));
        if (window.app) window.app.addPoints(50);
        this._queue.push(id);
        if (!this._busy) this._next();
    }

    _next() {
        if (this._queue.length === 0) { this._busy = false; return; }
        this._busy = true;
        const id = this._queue.shift();
        const def = BADGE_DEFS.find(b => b.id === id);
        if (!def) { this._next(); return; }

        const popup = document.getElementById('badge-popup');
        if (!popup) { this._next(); return; }

        const lang = window.app?.translator?.lang || 'uz';
        const emEl  = document.getElementById('bdp-emoji');
        const nameEl = document.getElementById('bdp-name');
        const descEl = document.getElementById('bdp-desc');
        if (emEl)   emEl.textContent  = def.emoji;
        if (nameEl) nameEl.textContent = def.name[lang] || def.name.uz;
        if (descEl) descEl.textContent = '+50 ⭐ bonus!';

        popup.classList.remove('h', 'bdp-out');
        popup.classList.add('bdp-in');

        const dismiss = () => {
            clearTimeout(this._timer);
            popup.onclick = null;
            popup.classList.remove('bdp-in');
            popup.classList.add('bdp-out');
            setTimeout(() => {
                popup.classList.add('h');
                popup.classList.remove('bdp-out');
                this._next();
            }, 300);
        };
        popup.onclick = dismiss;
        this._timer = setTimeout(dismiss, 3000);
    }

    onGamePlayed(catId) {
        const n = parseInt(localStorage.getItem('kz-game-count') || '0') + 1;
        localStorage.setItem('kz-game-count', String(n));

        const cats = JSON.parse(localStorage.getItem('kz-cats-tried') || '[]');
        if (catId && !cats.includes(catId)) {
            cats.push(catId);
            localStorage.setItem('kz-cats-tried', JSON.stringify(cats));
        }

        if (n === 1)  this.awardBadge('game_first');
        if (n === 5)  this.awardBadge('game_5');
        if (n === 25) this.awardBadge('game_25');
        if (n === 50) this.awardBadge('game_50');
        if (cats.length >= 3) this.awardBadge('cat_3');
        if (cats.length >= 7) this.awardBadge('cat_all');
    }

    onStoryPlayed() {
        const n = parseInt(localStorage.getItem('kz-story-count') || '0') + 1;
        localStorage.setItem('kz-story-count', String(n));
        if (n === 1)  this.awardBadge('story_first');
        if (n === 10) this.awardBadge('story_10');
    }

    onSongPlayed() {
        const n = parseInt(localStorage.getItem('kz-song-count') || '0') + 1;
        localStorage.setItem('kz-song-count', String(n));
        if (n === 1)  this.awardBadge('song_first');
        if (n === 10) this.awardBadge('song_10');
    }

    onPointsUpdated(pts) {
        if (pts >= 100)  this.awardBadge('stars_100');
        if (pts >= 500)  this.awardBadge('stars_500');
        if (pts >= 1000) this.awardBadge('stars_1000');
        if (pts >= 5000) this.awardBadge('stars_5000');
    }

    checkStreak() {
        const today = new Date().toISOString().slice(0,10).replace(/-/g,'');
        const days = JSON.parse(localStorage.getItem('kz-streak-days') || '[]');
        if (!days.includes(today)) {
            days.push(today);
            if (days.length > 30) days.shift();
            localStorage.setItem('kz-streak-days', JSON.stringify(days));
        }
        const streak = this._streak(days);
        if (streak >= 3) this.awardBadge('streak_3');
        if (streak >= 7) this.awardBadge('streak_7');
    }

    _streak(days) {
        if (!days.length) return 0;
        const sorted = [...days].sort();
        let cur = 1, best = 1;
        for (let i = 1; i < sorted.length; i++) {
            const a = new Date(sorted[i-1].replace(/(\d{4})(\d{2})(\d{2})/,'$1-$2-$3'));
            const b = new Date(sorted[i].replace(/(\d{4})(\d{2})(\d{2})/,'$1-$2-$3'));
            cur = (b - a) === 86400000 ? cur + 1 : 1;
            if (cur > best) best = cur;
        }
        return best;
    }

    renderOverlay() {
        const lang = window.app?.translator?.lang || 'uz';
        const grid = document.getElementById('badges-grid');
        const prog = document.getElementById('badges-progress');
        if (!grid) return;

        const earned = this._badges.length;
        const total  = BADGE_DEFS.length;
        const label  = lang==='uz' ? ' yutuq olindi' : lang==='ru' ? ' достижений' : ' achievements';
        if (prog) prog.textContent = earned + ' / ' + total + label;

        grid.innerHTML = '';
        BADGE_DEFS.forEach(def => {
            const ok   = this.isEarned(def.id);
            const name = def.name[lang] || def.name.uz;
            const card = document.createElement('div');
            card.className = 'bdg-card' + (ok ? ' bdg-earned' : ' bdg-locked');
            card.innerHTML =
                '<div class="bdg-em">' + (ok ? def.emoji : '🔒') + '</div>' +
                '<div class="bdg-nm">' + (ok ? name : '???') + '</div>';
            grid.appendChild(card);
        });
    }
}
```

- [ ] **Step 3: `badgeManager` ini init blokiga qo'shish**

`main.js`da `app.updateProgress();` qatoridan KEYIN:

```javascript
    window.badgeManager = new BadgeManager();
    badgeManager.checkStreak();
    badgeManager.onPointsUpdated(app.pts);
```

- [ ] **Step 4: Build qilish va xatolik yo'qligini tekshirish**

```
.\gradlew :app:assembleDebug 2>&1 | Select-String "error:|BUILD"
```
Kutilgan natija: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add app/src/main/assets/www/main.js
git commit -m "feat(badges): BadgeManager class — 16 badges, localStorage, popup queue"
```

---

## Task 2: Hook ulanishlari

**Files:**
- Modify: `app/src/main/assets/www/main.js`

- [ ] **Step 1: `GameManager.closeGame()` — o'yin tugaganda badge tekshirish**

`closeGame()` metodida `if (game) {` bloki ichidagi `this.addPoints(earned);` qatoridan KEYIN:

```javascript
        if (window.badgeManager) badgeManager.onGamePlayed(game.cat);
```

- [ ] **Step 2: `GameManager.addPoints()` — ball qo'shilganda badge tekshirish**

`addPoints(n)` metodida `this.updateProgress();` qatoridan KEYIN:

```javascript
        if (window.badgeManager) badgeManager.onPointsUpdated(this.pts);
```

- [ ] **Step 3: `StoryManager._play()` — ertak ochilganda badge tekshirish**

`StoryManager._play(item)` metodining birinchi qatoriga (funktsiya boshiga):

```javascript
        if (window.badgeManager) badgeManager.onStoryPlayed();
```

- [ ] **Step 4: `SongManager._play()` — qo'shiq ochilganda badge tekshirish**

`SongManager._play(item)` metodining birinchi qatoriga:

```javascript
        if (window.badgeManager) badgeManager.onSongPlayed();
```

- [ ] **Step 5: Build qilish**

```
.\gradlew :app:assembleDebug 2>&1 | Select-String "error:|BUILD"
```
Kutilgan natija: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```
git add app/src/main/assets/www/main.js
git commit -m "feat(badges): wire hooks — closeGame, addPoints, StoryManager, SongManager"
```

---

## Task 3: Badge popup HTML va CSS

**Files:**
- Modify: `app/src/main/assets/www/index.html`

- [ ] **Step 1: CSS qo'shish**

`index.html`da `</style>` tegidan OLDIN qo'shiladi:

```css
    /* ── Badge popup ──────────────────────────────── */
    #badge-popup{
      position:fixed;top:50%;left:50%;
      transform:translate(-50%,-50%) scale(0);
      z-index:9999;pointer-events:auto;
    }
    #badge-popup.h{display:none}
    #badge-popup.bdp-in{animation:bdpIn .4s ease forwards}
    #badge-popup.bdp-out{animation:bdpOut .3s ease forwards}
    @keyframes bdpIn{
      0%{transform:translate(-50%,-50%) scale(0)}
      70%{transform:translate(-50%,-50%) scale(1.15)}
      100%{transform:translate(-50%,-50%) scale(1)}
    }
    @keyframes bdpOut{
      from{transform:translate(-50%,-50%) scale(1);opacity:1}
      to{transform:translate(-50%,-50%) scale(.8);opacity:0}
    }
    .bdp-inner{
      background:#FFF8F0;border-radius:24px;
      padding:28px 36px;text-align:center;
      box-shadow:0 8px 32px rgba(0,0,0,.28);min-width:220px;
    }
    .bdp-emoji{font-size:56px;margin-bottom:8px}
    .bdp-name{font-size:20px;font-weight:700;color:#222;margin-bottom:4px}
    .bdp-desc{font-size:14px;color:#FF6B35;font-weight:600}
```

- [ ] **Step 2: Popup HTML qo'shish**

`index.html`da `</body>` tegidan OLDIN, long-press `<script>` blokidan OLDIN:

```html
  <!-- Badge popup -->
  <div id="badge-popup" class="h">
    <div class="bdp-inner">
      <div id="bdp-emoji" class="bdp-emoji">🏅</div>
      <div id="bdp-name"  class="bdp-name">Badge!</div>
      <div id="bdp-desc"  class="bdp-desc">+50 ⭐</div>
    </div>
  </div>
```

- [ ] **Step 3: Build qilish**

```
.\gradlew :app:assembleDebug 2>&1 | Select-String "error:|BUILD"
```
Kutilgan natija: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```
git add app/src/main/assets/www/index.html
git commit -m "feat(badges): badge popup HTML + CSS bounce animation"
```

---

## Task 4: Badge sahifasi HTML + CSS + header tugma

**Files:**
- Modify: `app/src/main/assets/www/index.html`

- [ ] **Step 1: Badge overlay va badge card CSS qo'shish**

`index.html`da `</style>` tegidan OLDIN:

```css
    /* ── Badges overlay ───────────────────────────── */
    #badges-overlay{
      position:fixed;inset:0;background:#FFF8F0;
      z-index:1000;display:flex;flex-direction:column;overflow:hidden;
    }
    #badges-overlay.h{display:none}
    #badges-hdr{
      display:flex;align-items:center;
      padding:16px;border-bottom:1px solid #EEE;
      background:#FFF8F0;
    }
    #badges-hdr-title{
      flex:1;font-size:18px;font-weight:700;
      color:#222;font-family:'Fredoka One',cursive;
    }
    #badges-close{
      background:none;border:none;font-size:22px;
      color:#888;cursor:pointer;padding:4px 8px;
    }
    #badges-progress{
      text-align:center;font-size:13px;
      color:#FF6B35;font-weight:600;padding:10px 16px;
    }
    #badges-grid{
      display:grid;grid-template-columns:repeat(3,1fr);
      gap:12px;padding:16px;overflow-y:auto;flex:1;
    }
    .bdg-card{
      background:#fff;border-radius:16px;
      padding:16px 8px;text-align:center;
      box-shadow:0 2px 8px rgba(0,0,0,.08);
    }
    .bdg-card.bdg-earned{border:2px solid #FF6B35}
    .bdg-card.bdg-locked{opacity:.45}
    .bdg-em{font-size:32px;margin-bottom:6px}
    .bdg-nm{font-size:11px;color:#444;font-weight:600;line-height:1.3}
    /* Badge pill in header */
    #badge-pill{
      background:var(--surface);border:none;border-radius:50%;
      width:36px;height:36px;display:flex;align-items:center;
      justify-content:center;font-size:18px;cursor:pointer;
      margin-left:2px;flex-shrink:0;
    }
```

- [ ] **Step 2: Badge overlay HTML qo'shish**

`index.html`da badge popup div'dan OLDIN (Task 3 popup'dan oldin):

```html
  <!-- Badges overlay -->
  <div id="badges-overlay" class="h">
    <div id="badges-hdr">
      <div id="badges-hdr-title">🏅 Mening Yutuqlarim</div>
      <button id="badges-close" onclick="document.getElementById('badges-overlay').classList.add('h')">✕</button>
    </div>
    <div id="badges-progress">0 / 16</div>
    <div id="badges-grid"></div>
  </div>
```

- [ ] **Step 3: Headerga `🏅` tugma qo'shish**

`index.html`da:
```html
    <div id="stars-pill">⭐<span id="stars-val">0</span></div>
```
ni quyidagiga almashtirish:
```html
    <div id="stars-pill">⭐<span id="stars-val">0</span></div>
    <button id="badge-pill" onclick="openBadges()">🏅</button>
```

- [ ] **Step 4: `openBadges()` funksiyasini `<script>` blokiga qo'shish**

`index.html`dagi asosiy `<script>` blokida `function toggleMusic()` yoki shunga o'xshash global funksiya bilan bir joyga qo'shiladi:

```javascript
    function openBadges() {
      document.getElementById('badges-overlay').classList.remove('h');
      if (window.badgeManager) badgeManager.renderOverlay();
    }
```

- [ ] **Step 5: Build qilish**

```
.\gradlew :app:assembleDebug 2>&1 | Select-String "error:|BUILD"
```
Kutilgan natija: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```
git add app/src/main/assets/www/index.html
git commit -m "feat(badges): badges overlay page + header button + openBadges()"
```

---

## Task 5: Qurilmaga o'rnatish va smoke test

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

1. **`game_first` badge:** Birinchi o'yinni oching va yoping → popup chiqadi (🎮 Birinchi qadam, +50 ⭐)
2. **`story_first` badge:** Ertaklar tabiga o'ting, biron ertak bosing → popup chiqadi (📖 Birinchi ertak)
3. **`song_first` badge:** Qo'shiqlar tabiga o'ting, biron qo'shiq bosing → popup chiqadi (🎵)
4. **`stars_100` badge:** Ballar 100 dan oshganda popup chiqadi (⭐ Yulduzcha)
5. **Badge sahifasi:** Header'dagi 🏅 ni bosing → overlay ochiladi, olingan badgelar rangli, olinmaganlar kulrang
6. **Progress:** `X / 16 yutuq olindi` to'g'ri ko'rsatiladi
7. **Popup auto-dismiss:** 3 soniyadan keyin o'z-o'zidan yopiladi
8. **Popup tap-dismiss:** Popup ustiga bossangiz darhol yopiladi
9. **Ikki marta berilmaydi:** Xuddi shu badge ikkinchi marta chiqmaydi
10. **`streak_3` badge:** 3 kun ketma-ket kirgach chiqadi (localStorage'da `kz-streak-days` tekshirish mumkin)

- [ ] **Step 4: Final commit agar fix kerak bo'lsa**

```
git add app/src/main/assets/www/main.js app/src/main/assets/www/index.html
git commit -m "fix(badges): smoke test fixes"
```

- [ ] **Step 5: Push**

```
git push origin master
```
