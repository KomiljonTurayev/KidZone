# Faza 3: Stories & Songs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** WebView dagi KidAI tabni Songs tab bilan almashtirish va Songs funksionalligini qo'shish (SongManager, songs-section HTML, i18n, placeholder MP3 fayllar).

**Architecture:** Faqat HTML5/JS layer — `assets/www/` papkasi. Native Kotlin/Compose qismga tegish yo'q. `content.json` allaqachon 20 ta ertak + 20 ta qo'shiq ma'lumotini saqlaydi. `AudioPlayer`, `ContentManager` bazasi tayyor. `SongManager` faqat `ContentManager` ni extend qiladi — `_play()` override kerak emas (baza klass audio + error callback ni to'liq boshqaradi).

**Tech Stack:** HTML5, vanilla JS (ES6 classes), Android WebView, `.\gradlew installDebug`, adb

## Global Constraints

- Tab tartib: `games` → `stories` → `songs` (KidAI tab to'liq o'chiriladi)
- Song kategoriyalar: `lullaby`, `alphabet`, `animals`, `dance`, `games` (bular songs uchun — stories kategoriyalaridan farqli)
- Story kategoriyalar (o'zgarmaydi): `animals`, `nature`, `heroes`, `family`, `space`
- `AudioPlayer` singleton — `storyManager` va `songManager` bir xil instance ni ishlatadi
- `content.json` dagi songs har bir entry da `text` maydoni yo'q (faqat `audio`)
- Placeholder MP3 fayllar bo'sh (0 byte) — `AudioPlayer.onerror` `noAudio` toast ko'rsatadi, UI ishlashda davom etadi
- Barcha HTML/JS o'zgarishlar faqat `app/src/main/assets/www/` ichida

---

## File Map

| Amal | Fayl |
|---|---|
| Create (120 ta) | `app/src/main/assets/www/audio/stories/{uz,ru,en}/story-001..020.mp3` |
| Create (120 ta) | `app/src/main/assets/www/audio/songs/{uz,ru,en}/song-001..020.mp3` |
| Modify | `app/src/main/assets/www/index.html` — tab, section, CSS, i18n, functions |
| Modify | `app/src/main/assets/www/main.js` — SongManager, switchTab, validTabs, init |

---

### Task 1: Placeholder MP3 fayllar (120 ta)

**Files:**
- Create: `app/src/main/assets/www/audio/stories/{uz,ru,en}/story-001..020.mp3` (60 fayl)
- Create: `app/src/main/assets/www/audio/songs/{uz,ru,en}/song-001..020.mp3` (60 fayl)

**Interfaces:**
- Produces: AudioPlayer `new Audio(src)` — onerror callback ishga tushadi, `noAudio` toast ko'rsatadi. UI to'liq testlanadi.

- [ ] **Step 1: PowerShell script yozish va ishlatish**

`D:\android_projects\KidZone` papkasida ishga tushiring:

```powershell
$BASE = "app/src/main/assets/www/audio"
foreach ($type in @('stories','songs')) {
    foreach ($lang in @('uz','ru','en')) {
        for ($i = 1; $i -le 20; $i++) {
            $prefix = if ($type -eq 'stories') { 'story' } else { 'song' }
            $name = "{0}-{1:D3}.mp3" -f $prefix, $i
            $path = "$BASE/$type/$lang/$name"
            if (-not (Test-Path $path)) {
                New-Item -ItemType File -Path $path -Force | Out-Null
            }
        }
    }
}
Write-Host "Done."
```

- [ ] **Step 2: Fayl sonini tekshiring**

```powershell
(Get-ChildItem "app/src/main/assets/www/audio" -Recurse -Filter "*.mp3").Count
```

Kutilgan natija: `120`

- [ ] **Step 3: Commit**

```powershell
git add "app/src/main/assets/www/audio"
git commit -m "feat(faza3): 120 empty placeholder MP3 files for stories and songs"
```

---

### Task 2: index.html — KidAI tab → Songs tab

**Files:**
- Modify: `app/src/main/assets/www/index.html`

**Interfaces:**
- Consumes: `app.songManager` (Task 3 da yaratiladi) — `app.songManager?.render()`, `app.songManager?.togglePlay()`, `app.songManager?.stop()`
- Produces: `#tab-songs`, `#songs-section`, `songFilter()`, `clearSongSearch()`, `window._songCat`

**Muhim:** Bu katta faylda ko'p joyda o'zgarish bor. Har bir o'zgarish uchun aniq eski → yangi qiymat berilgan.

- [ ] **Step 1: CSS — kidai sectionni o'chir, songs section qo'sh**

Topish:
```css
    /* ── KIDAI CHAT ── */
    #kidai-section{display:flex;flex-direction:column;height:100%;overflow:hidden}
    #kidai-section.h{display:none}
    #kidai-messages{flex:1;overflow-y:auto;padding:16px var(--pad);display:flex;flex-direction:column;gap:12px;-webkit-overflow-scrolling:touch}
    .kai-msg{display:flex;align-items:flex-start;gap:8px;max-width:85%}
    .kai-msg .kai-av{font-size:22px;flex-shrink:0;margin-top:2px}
    .kai-msg .kai-bub{background:var(--surface);border-radius:0 16px 16px 16px;padding:10px 14px;font-size:15px;color:var(--text);line-height:1.6;box-shadow:0 1px 3px rgba(0,0,0,0.08);word-break:break-word}
    .kai-msg.user{flex-direction:row-reverse;align-self:flex-end}
    .kai-msg.user .kai-bub{background:var(--accent);color:#fff;border-radius:16px 0 16px 16px}
```

Almashtirish (ushbu CSS blokni quyidagi bilan):
```css
    /* ── SONGS SECTION ── */
    #songs-section{padding-top:10px}
    #songs-section.h{display:none}
```

- [ ] **Step 2: Tab button — KidAI → Songs**

Topish:
```html
    <button class="kz-tab" id="tab-kidai" onclick="switchTab('kidai')">🤖 <span id="tab-lbl-kidai">KidAI</span></button>
```

Almashtirish:
```html
    <button class="kz-tab" id="tab-songs" onclick="switchTab('songs')">🎵 <span id="tab-lbl-songs">Qo'shiqlar</span></button>
```

- [ ] **Step 3: Songs section HTML qo'shish (kidai-section ni almashtirish)**

Topish (bu blokni to'liq o'chirish va almashtirish):
```html
    <!-- KIDAI SECTION -->
    <div id="kidai-section" class="h">
      <div id="kidai-messages"></div>
      <div id="kai-quick">
        <button class="kai-qchip" id="kai-q1" onclick="kaiQuick('story')">📖 Ertak ayt</button>
        <button class="kai-qchip" id="kai-q2" onclick="kaiQuick('game')">🎮 O'yin topchi</button>
        <button class="kai-qchip" id="kai-q3" onclick="kaiQuick('fact')">❓ Savol ber</button>
      </div>
      <div id="kai-input-row">
        <input id="kai-input" type="text" placeholder="Yoz..." maxlength="200"
               onkeydown="if(event.key==='Enter')kaiSend()">
        <button id="kai-send" onclick="kaiSend()">➤</button>
      </div>
    </div><!-- /#kidai-section -->
```

Almashtirish:
```html
    <!-- SONGS SECTION -->
    <div id="songs-section" class="h">
      <div class="cc-cats" id="songs-cats">
        <button class="cc-chip active" onclick="songFilter(document.getElementById('songs-search').value,'all',this)">🎯 <span id="sqcat-all">Hammasi</span></button>
        <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'lullaby',this)">🌙 <span id="sqcat-lullaby">Allalar</span></button>
        <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'alphabet',this)">🔤 <span id="sqcat-alphabet">Alifbo</span></button>
        <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'animals',this)">🐾 <span id="sqcat-animals">Hayvonlar</span></button>
        <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'dance',this)">💃 <span id="sqcat-dance">Raqs</span></button>
        <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'games',this)">🎈 <span id="sqcat-games">O'yin</span></button>
      </div>
      <div class="cs-search-wrap">
        <input class="cs-search" type="text" id="songs-search"
               placeholder="🔍 Qo'shiq qidirish..."
               oninput="clearTimeout(window._sqTs);window._sqTs=setTimeout(()=>{var v=this.value;document.getElementById('songs-search-clear').classList.toggle('h',!v);songFilter(v,window._songCat||'all')},180)">
        <button class="cs-clear h" id="songs-search-clear" onclick="clearSongSearch()">✕</button>
      </div>
      <div class="cs-count" id="songs-count"></div>
      <div class="content-grid" id="songs-grid"></div>
    </div><!-- /#songs-section -->
```

- [ ] **Step 4: kidai.js script tagini o'chirish**

Topish:
```html
  <script src="kidai.js"></script>
```

O'chirish (bu qatorni to'liq o'chirish).

- [ ] **Step 5: i18n — Uzbek (uz) qismi**

Topish:
```js
        tabKidai:"KidAI", kidaiPlaceholder:"Yoz...", kidaiClear:"Tozala",
        kidaiQ1:"📖 Ertak ayt", kidaiQ2:"🎮 O'yin topchi", kidaiQ3:"❓ Savol ber",
```

Almashtirish:
```js
        tabSongs:"Qo'shiqlar", searchSong:"Qo'shiq qidirish...",
        sqCatAll:"Hammasi", sqCatLullaby:"Allalar", sqCatAlphabet:"Alifbo",
        sqCatAnimals:"Hayvonlar", sqCatDance:"Raqs", sqCatGames:"O'yin",
```

- [ ] **Step 6: i18n — Russian (ru) qismi**

Topish:
```js
        tabKidai:"KidAI", kidaiPlaceholder:"Напиши...", kidaiClear:"Очистить",
        kidaiQ1:"📖 Расскажи сказку", kidaiQ2:"🎮 Найди игру", kidaiQ3:"❓ Вопрос",
```

Almashtirish:
```js
        tabSongs:"Песни", searchSong:"Поиск песни...",
        sqCatAll:"Все", sqCatLullaby:"Колыбельные", sqCatAlphabet:"Азбука",
        sqCatAnimals:"Животные", sqCatDance:"Танцы", sqCatGames:"Игры",
```

- [ ] **Step 7: i18n — English (en) qismi**

Topish:
```js
        tabKidai:"KidAI", kidaiPlaceholder:"Type...", kidaiClear:"Clear",
        kidaiQ1:"📖 Tell a story", kidaiQ2:"🎮 Find a game", kidaiQ3:"❓ Ask me",
```

Almashtirish:
```js
        tabSongs:"Songs", searchSong:"Search songs...",
        sqCatAll:"All", sqCatLullaby:"Lullabies", sqCatAlphabet:"Alphabet",
        sqCatAnimals:"Animals", sqCatDance:"Dance", sqCatGames:"Games",
```

- [ ] **Step 8: switchTab() inline funksiyasini yangilash**

Topish:
```js
    function switchTab(tab) {
      app.ui.switchTab(tab);
      if (tab === 'stories') app.storyManager?.render();
      if (tab === 'kidai') kaiInit();
    }
```

Almashtirish:
```js
    function switchTab(tab) {
      app.ui.switchTab(tab);
      if (tab === 'stories') app.storyManager?.render();
      if (tab === 'songs') app.songManager?.render();
    }
```

- [ ] **Step 9: playerToggle() ni songs uchun yangilash**

Topish:
```js
    function playerToggle() {
      const active = localStorage.getItem('kz-tab');
      if (active === 'stories') app.storyManager?.togglePlay();
    }
```

Almashtirish:
```js
    function playerToggle() {
      const active = localStorage.getItem('kz-tab');
      if (active === 'stories') app.storyManager?.togglePlay();
      else if (active === 'songs') app.songManager?.togglePlay();
    }
```

- [ ] **Step 10: playerStop() ni songs uchun yangilash**

Topish:
```js
    function playerStop() {
      app.storyManager?.stop();
      app.audioPlayer?.stop();
```

Almashtirish:
```js
    function playerStop() {
      app.storyManager?.stop();
      app.songManager?.stop();
      app.audioPlayer?.stop();
```

- [ ] **Step 11: playContent() ni songs uchun yangilash**

Topish:
```js
    window.playContent = function(id) {
      if (!app) return;
      if (id.startsWith('story-')) {
        app.ui.switchTab('stories');
        const item = app.storyManager?.items.find(i => i.id === id);
        if (item) app.storyManager._play(item);
      }
    };
```

Almashtirish:
```js
    window.playContent = function(id) {
      if (!app) return;
      if (id.startsWith('story-')) {
        app.ui.switchTab('stories');
        const item = app.storyManager?.items.find(i => i.id === id);
        if (item) app.storyManager._play(item);
      } else if (id.startsWith('song-')) {
        app.ui.switchTab('songs');
        const item = app.songManager?.items.find(i => i.id === id);
        if (item) app.songManager._play(item);
      }
    };
```

- [ ] **Step 12: songFilter, clearSongSearch, window._songCat qo'shish**

`window._storyCat = 'all';` qatorini topib, uning PASTIGA qo'shish:

```js
    window._songCat = 'all';

    function songFilter(query, cat, chipEl) {
      if (chipEl) {
        document.querySelectorAll('#songs-cats .cc-chip').forEach(c => c.classList.remove('active'));
        chipEl.classList.add('active');
      }
      if (cat) window._songCat = cat;
      app.songManager?.filter(query || '', window._songCat);
      app.songManager?.render();
    }

    function clearSongSearch() {
      var inp = document.getElementById('songs-search');
      var btn = document.getElementById('songs-search-clear');
      if (inp) inp.value = '';
      if (btn) btn.classList.add('h');
      songFilter('', window._songCat || 'all');
    }
```

- [ ] **Step 13: updateLangUI() — KidAI refs → Songs refs**

Topish:
```js
        const tk = document.getElementById('tab-lbl-kidai');
        if (tk) tk.textContent = t2.tabKidai;
        const kInput = document.getElementById('kai-input');
        if (kInput) kInput.placeholder = t2.kidaiPlaceholder;
        const q1 = document.getElementById('kai-q1'); if (q1) q1.textContent = t2.kidaiQ1;
        const q2 = document.getElementById('kai-q2'); if (q2) q2.textContent = t2.kidaiQ2;
        const q3 = document.getElementById('kai-q3'); if (q3) q3.textContent = t2.kidaiQ3;
```

Almashtirish:
```js
        const tk = document.getElementById('tab-lbl-songs');
        if (tk) tk.textContent = t2.tabSongs;
        const ssearch2 = document.getElementById('songs-search');
        if (ssearch2) ssearch2.placeholder = '🔍 ' + t2.searchSong;
        const sqcatMap = {
          'sqcat-all': t2.sqCatAll, 'sqcat-lullaby': t2.sqCatLullaby,
          'sqcat-alphabet': t2.sqCatAlphabet, 'sqcat-animals': t2.sqCatAnimals,
          'sqcat-dance': t2.sqCatDance, 'sqcat-games': t2.sqCatGames
        };
        Object.entries(sqcatMap).forEach(([id,lbl]) => { const el=document.getElementById(id); if(el) el.textContent=lbl; });
```

- [ ] **Step 14: updateLangUI() oxirida songManager render qo'shish**

Topish:
```js
        // Re-render content grids in new language
        app.storyManager?.render();
    }
```

Almashtirish:
```js
        // Re-render content grids in new language
        app.storyManager?.render();
        app.songManager?.render();
    }
```

- [ ] **Step 15: Commit**

```powershell
git add "app/src/main/assets/www/index.html"
git commit -m "feat(faza3): replace KidAI tab with Songs tab in index.html"
```

---

### Task 3: main.js — SongManager class + switchTab + init

**Files:**
- Modify: `app/src/main/assets/www/main.js`

**Interfaces:**
- Consumes: `ContentManager` (base class, already in main.js), `AudioPlayer` instance, `TranslationManager` instance, `UIManager` instance
- Produces: `SongManager` class, `app.songManager` — `load()`, `render()`, `filter()`, `togglePlay()`, `stop()`, `_play()`

- [ ] **Step 1: SongManager class qo'shish**

`StoryManager` klassining oxirida (ya'ni `class GameManager {` qatoridan OLDIN) qo'shish:

```js
class SongManager extends ContentManager {
    constructor(player, translator, ui) {
        super('songs', player, translator, ui);
    }
}
```

`ContentManager._play()` songs uchun to'liq ishlaydi (text viewer yo'q, faqat audio). Override kerak emas.

- [ ] **Step 2: UIManager.switchTab — kidai → songs**

Topish:
```js
        ['games', 'stories', 'kidai'].forEach(s => {
```

Almashtirish:
```js
        ['games', 'stories', 'songs'].forEach(s => {
```

- [ ] **Step 3: validTabs — kidai → songs**

Topish:
```js
    const validTabs = ['games', 'stories', 'kidai'];
```

Almashtirish:
```js
    const validTabs = ['games', 'stories', 'songs'];
```

- [ ] **Step 4: songManager init qo'shish, KidAIEngine o'chirish**

Topish:
```js
    app.storyManager.load().then(() => {
      app.storyManager.render();
      window.kidAI = new KidAIEngine(app.storyManager.items, GAMES);
    });
```

Almashtirish:
```js
    app.songManager = new SongManager(audioPlayer, translator, ui);
    app.songManager.load().then(() => app.songManager.render());

    app.storyManager.load().then(() => app.storyManager.render());
```

- [ ] **Step 5: Commit**

```powershell
git add "app/src/main/assets/www/main.js"
git commit -m "feat(faza3): SongManager class, switchTab songs, init songManager"
```

---

### Task 4: Build, Install va Verify

**Files:**
- Test only — hech qanday kod o'zgartirilmaydi

**Interfaces:**
- Consumes: Tasks 1-3 dan barcha o'zgarishlar

- [ ] **Step 1: Build va install**

```powershell
.\gradlew installDebug
```

Kutilgan: `BUILD SUCCESSFUL` + qurilmada install.

- [ ] **Step 2: App ni ochish**

```powershell
& "C:\Users\Komiljon\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n uz.kidzone.app/.MainActivity
```

- [ ] **Step 3: Songs tab ni tekshirish**

Quyidagilarni tekshiring:
1. Tab bar: `🎮 O'yinlar | 📖 Ertaklar | 🎵 Qo'shiqlar` ko'rinadi (KidAI yo'q)
2. `🎵 Qo'shiqlar` bosilganda songs-section ko'rinadi
3. 20 ta qo'shiq kartasi grid da ko'rinadi
4. Kategoriya chip (Allalar, Alifbo, Hayvonlar...) bilan filtrlash ishlaydi
5. Qidiruv maydoni real-time ishlaydi
6. Karta bosilganda: `#kz-player` qisqacha ko'rinadi → "Audio fayl yo'q 🎵" toast → player yashiriladi (placeholder bo'sh fayl)
7. Til o'zgartirish: Songs tab va kategoriya nomlari yangi tilda ko'rinadi

- [ ] **Step 4: Stories tab xatoliklarini tekshirish**

`📖 Ertaklar` tabni bosing:
1. 20 ta ertak kartasi ko'rinadi
2. Kategoriya filtrlash ishlaydi
3. Xato yo'q

- [ ] **Step 5: logcat ni tekshirish**

```powershell
& "C:\Users\Komiljon\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat -d 2>&1 | Select-String "Error|Exception|FATAL|kidzone" | Select-Object -Last 20
```

Kutilgan: JavaScript xatolari yo'q (`Uncaught`), `FATAL` yo'q.

- [ ] **Step 6: Final commit**

```powershell
git add -A
git commit -m "feat(faza3): Faza 3 complete — Songs tab replaces KidAI, SongManager, 120 placeholder MP3s"
```
