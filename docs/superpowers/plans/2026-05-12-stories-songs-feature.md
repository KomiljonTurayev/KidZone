# Stories & Songs Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Ertaklar (Stories) and Qo'shiqlar (Songs) tabs to KidZone's main screen with pre-recorded MP3 playback, category filters, and real-time search.

**Architecture:** Tab bar inserted between `#hdr` and `#main` in index.html. Existing games wrapped in `#games-section`. New `#stories-section` and `#songs-section` added to `#main`. A single fixed `#kz-player` (shared audio player) sits above the bottom nav. Three new JS classes in main.js: `AudioPlayer` (singleton), `ContentManager` (base), `StoryManager` / `SongManager` (subclasses). Content metadata lives in `content.json`; MP3 files go in `audio/stories/<lang>/` and `audio/songs/<lang>/`.

**Tech Stack:** HTML5, vanilla JS (ES6 classes), Android WebView, Gradle assembleDebug, adb install

---

## File Map

| Action | File |
|--------|------|
| Create | `app/src/main/assets/www/content.json` |
| Create dirs | `app/src/main/assets/www/audio/stories/{uz,ru,en}/` |
| Create dirs | `app/src/main/assets/www/audio/songs/{uz,ru,en}/` |
| Modify | `app/src/main/assets/www/main.js` — add AudioPlayer, ContentManager, StoryManager, SongManager; extend UIManager; update init |
| Modify | `app/src/main/assets/www/index.html` — tab bar HTML, games-section wrapper, stories/songs sections, player, CSS, i18n, bridge fns |

---

## Task 1: Create content.json

**Files:**
- Create: `app/src/main/assets/www/content.json`

- [ ] **Step 1: Write content.json with 20 stories and 20 songs**

```json
{
  "stories": [
    {"id":"story-001","category":"animals","emoji":"🦁","title":{"uz":"Sher va Sichqon","ru":"Лев и Мышь","en":"Lion and Mouse"},"audio":{"uz":"audio/stories/uz/story-001.mp3","ru":"audio/stories/ru/story-001.mp3","en":"audio/stories/en/story-001.mp3"}},
    {"id":"story-002","category":"animals","emoji":"🐘","title":{"uz":"Fil va Chumoli","ru":"Слон и Муравей","en":"Elephant and Ant"},"audio":{"uz":"audio/stories/uz/story-002.mp3","ru":"audio/stories/ru/story-002.mp3","en":"audio/stories/en/story-002.mp3"}},
    {"id":"story-003","category":"animals","emoji":"🐢","title":{"uz":"Toshbaqa va Quyon","ru":"Черепаха и Заяц","en":"Tortoise and Hare"},"audio":{"uz":"audio/stories/uz/story-003.mp3","ru":"audio/stories/ru/story-003.mp3","en":"audio/stories/en/story-003.mp3"}},
    {"id":"story-004","category":"animals","emoji":"🦊","title":{"uz":"Tulki va Uzum","ru":"Лиса и Виноград","en":"Fox and Grapes"},"audio":{"uz":"audio/stories/uz/story-004.mp3","ru":"audio/stories/ru/story-004.mp3","en":"audio/stories/en/story-004.mp3"}},
    {"id":"story-005","category":"nature","emoji":"🌳","title":{"uz":"Sehrli Daraxt","ru":"Волшебное Дерево","en":"Magic Tree"},"audio":{"uz":"audio/stories/uz/story-005.mp3","ru":"audio/stories/ru/story-005.mp3","en":"audio/stories/en/story-005.mp3"}},
    {"id":"story-006","category":"nature","emoji":"🌈","title":{"uz":"Yomg'irdan Keyin","ru":"После Дождя","en":"After the Rain"},"audio":{"uz":"audio/stories/uz/story-006.mp3","ru":"audio/stories/ru/story-006.mp3","en":"audio/stories/en/story-006.mp3"}},
    {"id":"story-007","category":"nature","emoji":"🌸","title":{"uz":"Bahor Keldi","ru":"Пришла Весна","en":"Spring Has Come"},"audio":{"uz":"audio/stories/uz/story-007.mp3","ru":"audio/stories/ru/story-007.mp3","en":"audio/stories/en/story-007.mp3"}},
    {"id":"story-008","category":"nature","emoji":"⛄","title":{"uz":"Qorbobo","ru":"Снеговик","en":"Snowman"},"audio":{"uz":"audio/stories/uz/story-008.mp3","ru":"audio/stories/ru/story-008.mp3","en":"audio/stories/en/story-008.mp3"}},
    {"id":"story-009","category":"heroes","emoji":"🦸","title":{"uz":"Jasur Bolakay","ru":"Смелый Мальчик","en":"Brave Boy"},"audio":{"uz":"audio/stories/uz/story-009.mp3","ru":"audio/stories/ru/story-009.mp3","en":"audio/stories/en/story-009.mp3"}},
    {"id":"story-010","category":"heroes","emoji":"🌟","title":{"uz":"Kichkina Qahramonlar","ru":"Маленькие Герои","en":"Little Heroes"},"audio":{"uz":"audio/stories/uz/story-010.mp3","ru":"audio/stories/ru/story-010.mp3","en":"audio/stories/en/story-010.mp3"}},
    {"id":"story-011","category":"heroes","emoji":"🚀","title":{"uz":"Yulduzga Sayohat","ru":"Путь к Звезде","en":"Journey to Stars"},"audio":{"uz":"audio/stories/uz/story-011.mp3","ru":"audio/stories/ru/story-011.mp3","en":"audio/stories/en/story-011.mp3"}},
    {"id":"story-012","category":"heroes","emoji":"🧙","title":{"uz":"Sehrgar Bola","ru":"Волшебный Ребёнок","en":"Magic Child"},"audio":{"uz":"audio/stories/uz/story-012.mp3","ru":"audio/stories/ru/story-012.mp3","en":"audio/stories/en/story-012.mp3"}},
    {"id":"story-013","category":"family","emoji":"👨‍👩‍👧","title":{"uz":"Biz Birgamiz","ru":"Мы Вместе","en":"We Are Together"},"audio":{"uz":"audio/stories/uz/story-013.mp3","ru":"audio/stories/ru/story-013.mp3","en":"audio/stories/en/story-013.mp3"}},
    {"id":"story-014","category":"family","emoji":"👵","title":{"uz":"Buvi Ertagi","ru":"Бабушкина Сказка","en":"Grandmother's Story"},"audio":{"uz":"audio/stories/uz/story-014.mp3","ru":"audio/stories/ru/story-014.mp3","en":"audio/stories/en/story-014.mp3"}},
    {"id":"story-015","category":"family","emoji":"🏠","title":{"uz":"Yangi Uy","ru":"Новый Дом","en":"New Home"},"audio":{"uz":"audio/stories/uz/story-015.mp3","ru":"audio/stories/ru/story-015.mp3","en":"audio/stories/en/story-015.mp3"}},
    {"id":"story-016","category":"family","emoji":"👦","title":{"uz":"Aka-Uka","ru":"Братья","en":"Brothers"},"audio":{"uz":"audio/stories/uz/story-016.mp3","ru":"audio/stories/ru/story-016.mp3","en":"audio/stories/en/story-016.mp3"}},
    {"id":"story-017","category":"space","emoji":"🌙","title":{"uz":"Koinotda Sayohat","ru":"Космическое Путешествие","en":"Space Journey"},"audio":{"uz":"audio/stories/uz/story-017.mp3","ru":"audio/stories/ru/story-017.mp3","en":"audio/stories/en/story-017.mp3"}},
    {"id":"story-018","category":"space","emoji":"⭐","title":{"uz":"Yulduz Bolasi","ru":"Дитя Звезды","en":"Star Child"},"audio":{"uz":"audio/stories/uz/story-018.mp3","ru":"audio/stories/ru/story-018.mp3","en":"audio/stories/en/story-018.mp3"}},
    {"id":"story-019","category":"space","emoji":"🔴","title":{"uz":"Marsdagi Sarguzasht","ru":"Приключение на Марсе","en":"Mars Adventure"},"audio":{"uz":"audio/stories/uz/story-019.mp3","ru":"audio/stories/ru/story-019.mp3","en":"audio/stories/en/story-019.mp3"}},
    {"id":"story-020","category":"space","emoji":"🤖","title":{"uz":"Robot Do'stim","ru":"Мой Друг Робот","en":"My Robot Friend"},"audio":{"uz":"audio/stories/uz/story-020.mp3","ru":"audio/stories/ru/story-020.mp3","en":"audio/stories/en/story-020.mp3"}}
  ],
  "songs": [
    {"id":"song-001","category":"lullaby","emoji":"🌙","title":{"uz":"Alla","ru":"Колыбельная","en":"Lullaby"},"audio":{"uz":"audio/songs/uz/song-001.mp3","ru":"audio/songs/ru/song-001.mp3","en":"audio/songs/en/song-001.mp3"}},
    {"id":"song-002","category":"lullaby","emoji":"⭐","title":{"uz":"Yoqimli Tush","ru":"Приятный Сон","en":"Sweet Dream"},"audio":{"uz":"audio/songs/uz/song-002.mp3","ru":"audio/songs/ru/song-002.mp3","en":"audio/songs/en/song-002.mp3"}},
    {"id":"song-003","category":"lullaby","emoji":"🌜","title":{"uz":"Oy Botdi","ru":"Луна Зашла","en":"Moon Is Down"},"audio":{"uz":"audio/songs/uz/song-003.mp3","ru":"audio/songs/ru/song-003.mp3","en":"audio/songs/en/song-003.mp3"}},
    {"id":"song-004","category":"lullaby","emoji":"😴","title":{"uz":"Uxlayapsizmi?","ru":"Ты Спишь?","en":"Are You Sleeping?"},"audio":{"uz":"audio/songs/uz/song-004.mp3","ru":"audio/songs/ru/song-004.mp3","en":"audio/songs/en/song-004.mp3"}},
    {"id":"song-005","category":"alphabet","emoji":"🔤","title":{"uz":"ABC Qo'shig'i","ru":"Песня ABC","en":"ABC Song"},"audio":{"uz":"audio/songs/uz/song-005.mp3","ru":"audio/songs/ru/song-005.mp3","en":"audio/songs/en/song-005.mp3"}},
    {"id":"song-006","category":"alphabet","emoji":"📝","title":{"uz":"O'zbek Harflari","ru":"Буквы Алфавита","en":"Alphabet Letters"},"audio":{"uz":"audio/songs/uz/song-006.mp3","ru":"audio/songs/ru/song-006.mp3","en":"audio/songs/en/song-006.mp3"}},
    {"id":"song-007","category":"alphabet","emoji":"🔢","title":{"uz":"Raqamlar Qo'shig'i","ru":"Песня Цифр","en":"Numbers Song"},"audio":{"uz":"audio/songs/uz/song-007.mp3","ru":"audio/songs/ru/song-007.mp3","en":"audio/songs/en/song-007.mp3"}},
    {"id":"song-008","category":"alphabet","emoji":"🌈","title":{"uz":"Ranglar Qo'shig'i","ru":"Песня Цветов","en":"Colors Song"},"audio":{"uz":"audio/songs/uz/song-008.mp3","ru":"audio/songs/ru/song-008.mp3","en":"audio/songs/en/song-008.mp3"}},
    {"id":"song-009","category":"animals","emoji":"🐾","title":{"uz":"Hayvonlar Qo'shig'i","ru":"Песня Животных","en":"Animal Song"},"audio":{"uz":"audio/songs/uz/song-009.mp3","ru":"audio/songs/ru/song-009.mp3","en":"audio/songs/en/song-009.mp3"}},
    {"id":"song-010","category":"animals","emoji":"🐄","title":{"uz":"Ferma Qo'shig'i","ru":"Песня Фермы","en":"Farm Song"},"audio":{"uz":"audio/songs/uz/song-010.mp3","ru":"audio/songs/ru/song-010.mp3","en":"audio/songs/en/song-010.mp3"}},
    {"id":"song-011","category":"animals","emoji":"🐠","title":{"uz":"Dengiz Hayvonlari","ru":"Морские Животные","en":"Sea Animals"},"audio":{"uz":"audio/songs/uz/song-011.mp3","ru":"audio/songs/ru/song-011.mp3","en":"audio/songs/en/song-011.mp3"}},
    {"id":"song-012","category":"animals","emoji":"🐦","title":{"uz":"Qushlar Sayraydi","ru":"Птицы Поют","en":"Birds Singing"},"audio":{"uz":"audio/songs/uz/song-012.mp3","ru":"audio/songs/ru/song-012.mp3","en":"audio/songs/en/song-012.mp3"}},
    {"id":"song-013","category":"dance","emoji":"💃","title":{"uz":"Raqs Qilamiz","ru":"Будем Танцевать","en":"Let's Dance"},"audio":{"uz":"audio/songs/uz/song-013.mp3","ru":"audio/songs/ru/song-013.mp3","en":"audio/songs/en/song-013.mp3"}},
    {"id":"song-014","category":"dance","emoji":"🙌","title":{"uz":"Qo'llarimni Ko'taraman","ru":"Подниму Руки","en":"Raise My Hands"},"audio":{"uz":"audio/songs/uz/song-014.mp3","ru":"audio/songs/ru/song-014.mp3","en":"audio/songs/en/song-014.mp3"}},
    {"id":"song-015","category":"dance","emoji":"👣","title":{"uz":"Tepkilaymiz","ru":"Топаем!","en":"Stomp!"},"audio":{"uz":"audio/songs/uz/song-015.mp3","ru":"audio/songs/ru/song-015.mp3","en":"audio/songs/en/song-015.mp3"}},
    {"id":"song-016","category":"dance","emoji":"⭕","title":{"uz":"Doira Raqsi","ru":"Круговой Танец","en":"Circle Dance"},"audio":{"uz":"audio/songs/uz/song-016.mp3","ru":"audio/songs/ru/song-016.mp3","en":"audio/songs/en/song-016.mp3"}},
    {"id":"song-017","category":"games","emoji":"🌱","title":{"uz":"Mitti Bog'bon","ru":"Маленький Садовник","en":"Little Gardener"},"audio":{"uz":"audio/songs/uz/song-017.mp3","ru":"audio/songs/ru/song-017.mp3","en":"audio/songs/en/song-017.mp3"}},
    {"id":"song-018","category":"games","emoji":"🏎️","title":{"uz":"Poyga","ru":"Гонка","en":"Race Song"},"audio":{"uz":"audio/songs/uz/song-018.mp3","ru":"audio/songs/ru/song-018.mp3","en":"audio/songs/en/song-018.mp3"}},
    {"id":"song-019","category":"games","emoji":"🐙","title":{"uz":"Sakkizoyoq","ru":"Осьминог","en":"Octopus"},"audio":{"uz":"audio/songs/uz/song-019.mp3","ru":"audio/songs/ru/song-019.mp3","en":"audio/songs/en/song-019.mp3"}},
    {"id":"song-020","category":"games","emoji":"🕊️","title":{"uz":"Kaptar","ru":"Голубь","en":"Pigeon"},"audio":{"uz":"audio/songs/uz/song-020.mp3","ru":"audio/songs/ru/song-020.mp3","en":"audio/songs/en/song-020.mp3"}}
  ]
}
```

- [ ] **Step 2: Create audio directory structure**

```bash
mkdir -p app/src/main/assets/www/audio/stories/uz
mkdir -p app/src/main/assets/www/audio/stories/ru
mkdir -p app/src/main/assets/www/audio/stories/en
mkdir -p app/src/main/assets/www/audio/songs/uz
mkdir -p app/src/main/assets/www/audio/songs/ru
mkdir -p app/src/main/assets/www/audio/songs/en
```

> Note: Leave directories empty for now. AudioPlayer handles missing files gracefully with a toast message. Add real MP3 files by dropping them into these directories and rebuilding.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/www/content.json
git add app/src/main/assets/www/audio/
git commit -m "feat(content): add content.json with 20 stories and 20 songs metadata"
```

---

## Task 2: Add AudioPlayer class to main.js

**Files:**
- Modify: `app/src/main/assets/www/main.js` — insert after line 67 (after closing `}` of UIManager)

- [ ] **Step 1: Insert AudioPlayer after UIManager's closing brace (after line 67)**

Insert this block between the `}` of UIManager (line 67) and `class GameManager` (line 69):

```js
class AudioPlayer {
    constructor() {
        this._audio = null;
        this._paused = false;
    }

    play(src, title, onTimeUpdate, onEnded, onError) {
        if (this._audio) {
            this._audio.pause();
            this._audio.src = '';
        }
        this._audio = new Audio(src);
        this._paused = false;

        this._audio.addEventListener('timeupdate', () => {
            if (onTimeUpdate) onTimeUpdate(this._audio.currentTime, this._audio.duration || 0);
        });
        this._audio.addEventListener('ended', () => {
            this._paused = false;
            if (onEnded) onEnded();
        });
        this._audio.addEventListener('error', () => {
            this._paused = false;
            if (onError) onError();
        });

        this._audio.play().catch(() => { if (onError) onError(); });
    }

    pause() {
        if (this._audio && !this._audio.paused) {
            this._audio.pause();
            this._paused = true;
        }
    }

    resume() {
        if (this._audio && this._audio.paused) {
            this._audio.play().catch(() => {});
            this._paused = false;
        }
    }

    stop() {
        if (this._audio) {
            this._audio.pause();
            this._audio.src = '';
            this._audio = null;
        }
        this._paused = false;
    }

    seek(pct) {
        if (this._audio && this._audio.duration) {
            this._audio.currentTime = pct * this._audio.duration / 100;
        }
    }

    isPlaying() {
        return this._audio && !this._audio.paused && !this._paused;
    }

    isPaused() {
        return this._paused;
    }
}
```

- [ ] **Step 2: Verify insertion — file should have AudioPlayer between UIManager and GameManager**

Open `main.js` and confirm the class order is: `TranslationManager → UIManager → AudioPlayer → GameManager`.

---

## Task 3: Add ContentManager, StoryManager, SongManager to main.js

**Files:**
- Modify: `app/src/main/assets/www/main.js` — insert after AudioPlayer class

- [ ] **Step 1: Insert ContentManager, StoryManager, SongManager after AudioPlayer**

Insert this block immediately after the AudioPlayer class closing brace, before `class GameManager`:

```js
class ContentManager {
    constructor(type, player, translator, ui) {
        this.type = type;
        this.player = player;
        this.translator = translator;
        this.ui = ui;
        this.items = [];
        this.filtered = [];
        this.currentId = null;
        this._catFilter = 'all';
        this._searchQuery = '';
    }

    async load() {
        try {
            const res = await fetch('content.json');
            const data = await res.json();
            this.items = data[this.type] || [];
            this.filtered = [...this.items];
        } catch (e) {
            this.items = [];
            this.filtered = [];
        }
    }

    filter(query, cat) {
        this._searchQuery = query;
        this._catFilter = cat;
        const lang = this.translator.lang;
        this.filtered = this.items.filter(item => {
            const matchCat = cat === 'all' || item.category === cat;
            const title = (item.title[lang] || item.title.en || '').toLowerCase();
            const matchQuery = !query || title.includes(query.toLowerCase());
            return matchCat && matchQuery;
        });
    }

    render() {
        const gridId = this.type === 'stories' ? 'stories-grid' : 'songs-grid';
        const grid = document.getElementById(gridId);
        if (!grid) return;
        const lang = this.translator.lang;
        grid.innerHTML = '';
        this.filtered.forEach(item => {
            const card = document.createElement('div');
            card.className = 'content-card' + (item.id === this.currentId ? ' playing' : '');
            const title = item.title[lang] || item.title.en;
            card.innerHTML =
                '<div class="cc-art">' + item.emoji + '</div>' +
                '<div class="cc-title">' + title + '</div>' +
                '<div class="cc-ic">' + (item.id === this.currentId ? (this.player.isPlaying() ? '⏸' : '▶') : '▶') + '</div>';
            card.onclick = () => this._play(item);
            grid.appendChild(card);
        });
    }

    _play(item) {
        const lang = this.translator.lang;
        const src = item.audio[lang] || item.audio.en;
        const title = (item.title[lang] || item.title.en);

        this.currentId = item.id;
        this.render();
        this._showPlayer(item.emoji + ' ' + title);

        this.player.play(
            src, title,
            (cur, dur) => this._onTimeUpdate(cur, dur),
            () => this._onEnded(),
            () => {
                this.ui.showToast(this.translator.get('noAudio') || 'Audio unavailable');
                this._hidePlayer();
                this.currentId = null;
                this.render();
            }
        );
        document.getElementById('kzp-play').textContent = '⏸';
    }

    _onTimeUpdate(cur, dur) {
        const prog = document.getElementById('kzp-progress');
        const time = document.getElementById('kzp-time');
        if (prog && dur) prog.value = (cur / dur) * 100;
        if (time) time.textContent = this._fmt(cur) + ' / ' + this._fmt(dur);
    }

    _onEnded() {
        const btn = document.getElementById('kzp-play');
        if (btn) btn.textContent = '▶';
        this.currentId = null;
        this.render();
    }

    _showPlayer(title) {
        const player = document.getElementById('kz-player');
        const titleEl = document.getElementById('kzp-title');
        if (player) player.classList.remove('h');
        if (titleEl) titleEl.textContent = title;
        const prog = document.getElementById('kzp-progress');
        const time = document.getElementById('kzp-time');
        if (prog) prog.value = 0;
        if (time) time.textContent = '0:00 / 0:00';
        document.getElementById('main')?.classList.add('with-player');
    }

    _hidePlayer() {
        const player = document.getElementById('kz-player');
        if (player) player.classList.add('h');
        document.getElementById('main')?.classList.remove('with-player');
    }

    togglePlay() {
        if (this.player.isPlaying()) {
            this.player.pause();
            document.getElementById('kzp-play').textContent = '▶';
            this.render();
        } else if (this.player.isPaused()) {
            this.player.resume();
            document.getElementById('kzp-play').textContent = '⏸';
            this.render();
        }
    }

    stop() {
        this.player.stop();
        this.currentId = null;
        this._hidePlayer();
        this.render();
    }

    _fmt(s) {
        if (!s || isNaN(s)) return '0:00';
        const m = Math.floor(s / 60);
        const sec = String(Math.floor(s % 60)).padStart(2, '0');
        return m + ':' + sec;
    }
}

class StoryManager extends ContentManager {
    constructor(player, translator, ui) {
        super('stories', player, translator, ui);
    }
}

class SongManager extends ContentManager {
    constructor(player, translator, ui) {
        super('songs', player, translator, ui);
    }
}
```

- [ ] **Step 2: Verify class order in main.js**

Confirm order: `TranslationManager → UIManager → AudioPlayer → ContentManager → StoryManager → SongManager → GameManager`.

---

## Task 4: Add switchTab() to UIManager and update navTo()

**Files:**
- Modify: `app/src/main/assets/www/main.js`

- [ ] **Step 1: Add switchTab() method to UIManager**

Inside the `UIManager` class, after the `closeModal` method (before the closing `}`), add:

```js
    switchTab(tab) {
        localStorage.setItem('kz-tab', tab);
        document.querySelectorAll('.kz-tab').forEach(t => t.classList.remove('active'));
        const tabEl = document.getElementById('tab-' + tab);
        if (tabEl) tabEl.classList.add('active');
        const sections = ['games', 'stories', 'songs'];
        sections.forEach(s => {
            const el = document.getElementById(s + '-section');
            if (el) el.classList.toggle('h', s !== tab);
        });
    }
```

- [ ] **Step 2: Update GameManager.navTo() to switch to games tab when bottom nav is used**

In `GameManager.navTo()`, add one line at the very start of the method body, before the existing `document.querySelectorAll(".ni")` line:

```js
        if (window.app?.ui?.switchTab) app.ui.switchTab('games');
```

So the method starts as:
```js
    navTo(pg) {
        if (window.app?.ui?.switchTab) app.ui.switchTab('games');
        document.querySelectorAll(".ni").forEach(n => n.classList.remove("on"));
        // ... rest unchanged
```

- [ ] **Step 3: Commit main.js changes so far**

```bash
git add app/src/main/assets/www/main.js
git commit -m "feat(main.js): add AudioPlayer, ContentManager, StoryManager, SongManager, UIManager.switchTab"
```

---

## Task 5: Update app initialization in main.js

**Files:**
- Modify: `app/src/main/assets/www/main.js` — the `window.addEventListener("load", ...)` block at the bottom

- [ ] **Step 1: Update the load handler to initialize new managers**

Replace the existing `window.addEventListener("load", ...)` block (from line 378 to end of file) with:

```js
// Initialize application
let app;
window.addEventListener("load", () => {
    const translator = new TranslationManager(T);
    const ui = new UIManager();
    app = new GameManager(GAMES, ui, translator);

    const audioPlayer = new AudioPlayer();
    app.storyManager = new StoryManager(audioPlayer, translator, ui);
    app.songManager  = new SongManager(audioPlayer, translator, ui);
    app.audioPlayer  = audioPlayer;

    // Initial state
    app.updateProgress();
    if (window.updateLangUI) window.updateLangUI();
    ui.updateMusicUI(app.isMuted);

    if (app.isMuted && window.AndroidAdMob?.toggleMusic) {
        window.AndroidAdMob.toggleMusic(true);
    }

    // Load content and render
    app.storyManager.load().then(() => app.storyManager.render());
    app.songManager.load().then(() => app.songManager.render());

    // Restore last active tab (default: games)
    const savedTab = localStorage.getItem('kz-tab') || 'games';
    ui.switchTab(savedTab);

    setTimeout(() => {
        const loader = document.getElementById("loader");
        if (loader) loader.classList.add("h");
    }, 1400);
});
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/assets/www/main.js
git commit -m "feat(main.js): wire up StoryManager and SongManager in app init"
```

---

## Task 6: Add CSS to index.html

**Files:**
- Modify: `app/src/main/assets/www/index.html` — inside `<style>` block, append before `</style>`

- [ ] **Step 1: Append new CSS rules before the closing `</style>` tag**

Find the line `</style>` (it comes right before `</head>`) and insert this block before it:

```css
    /* ── TAB BAR ── */
    #tab-bar{
      display:flex;background:var(--surface);
      border-bottom:2px solid rgba(0,0,0,0.06);
      flex-shrink:0;
    }
    .kz-tab{
      flex:1;padding:10px 4px 8px;border:none;background:transparent;
      font-family:'Fredoka One',cursive;font-size:13px;color:var(--dim);
      cursor:pointer;border-bottom:3px solid transparent;
      transition:color .2s,border-bottom-color .2s;
      touch-action:manipulation;
    }
    .kz-tab.active{color:var(--accent);border-bottom-color:var(--accent)}

    /* ── MAIN WITH PLAYER ── */
    #main.with-player{padding-bottom:calc(var(--bh)+68px+78px+16px)!important}

    /* ── CONTENT SECTIONS ── */
    #stories-section,#songs-section{padding-top:8px}

    /* ── CONTENT SEARCH ── */
    .cs-search-wrap{padding:0 var(--pad) 8px}
    .cs-search{
      width:100%;background:var(--surface);border:none;border-radius:16px;
      padding:10px 14px;font-size:14px;font-family:'Nunito',sans-serif;
      box-shadow:0 3px 0 rgba(0,0,0,0.07);color:var(--text);
    }

    /* ── CONTENT CATEGORY CHIPS ── */
    .cc-cats{
      display:flex;gap:8px;overflow-x:auto;padding:0 var(--pad) 10px;
      scrollbar-width:none;
    }
    .cc-cats::-webkit-scrollbar{display:none}
    .cc-chip{
      display:flex;align-items:center;gap:4px;
      background:var(--surface);border:none;border-radius:20px;
      padding:6px 12px;font-size:11px;font-weight:800;white-space:nowrap;
      cursor:pointer;box-shadow:0 3px 0 rgba(0,0,0,0.10);
      font-family:'Nunito',sans-serif;color:var(--text);
      touch-action:manipulation;
    }
    .cc-chip.active{background:var(--accent);color:white;box-shadow:0 3px 0 var(--accent-dark)}

    /* ── CONTENT GRID ── */
    .content-grid{
      display:grid;
      grid-template-columns:repeat(3,1fr);
      gap:var(--gap);padding:0 var(--pad) 12px;
    }
    @media(min-width:480px){.content-grid{grid-template-columns:repeat(4,1fr)}}

    .content-card{
      background:var(--surface);border-radius:16px;
      padding:12px 8px 10px;text-align:center;cursor:pointer;
      box-shadow:0 4px 0 rgba(0,0,0,0.10);
      transition:transform .12s var(--spring);
      display:flex;flex-direction:column;align-items:center;gap:5px;
    }
    .content-card:active{transform:translateY(3px);box-shadow:0 1px 0 rgba(0,0,0,0.08)}
    .content-card.playing{
      background:rgba(255,107,53,0.08);
      outline:2px solid var(--accent);
    }
    .cc-art{font-size:34px}
    .cc-title{
      font-family:'Fredoka One',cursive;font-size:10px;
      color:var(--text);line-height:1.2;
    }
    .cc-ic{font-size:13px;color:var(--accent);font-weight:900}

    /* ── KZ AUDIO PLAYER ── */
    #kz-player{
      position:fixed;
      bottom:calc(var(--bh)+64px);
      left:0;right:0;z-index:19;
      background:var(--surface);
      padding:10px var(--pad) 8px;
      box-shadow:0 -3px 8px rgba(0,0,0,0.10);
    }
    .kzp-top{
      display:flex;align-items:center;gap:10px;margin-bottom:6px;
    }
    #kzp-title{
      font-family:'Fredoka One',cursive;font-size:13px;
      color:var(--text);flex:1;
      overflow:hidden;text-overflow:ellipsis;white-space:nowrap;
    }
    .kzp-btn{
      background:none;border:none;font-size:22px;
      cursor:pointer;padding:2px;touch-action:manipulation;
    }
    #kzp-progress{
      width:100%;height:4px;-webkit-appearance:none;appearance:none;
      background:rgba(0,0,0,0.10);border-radius:2px;outline:none;
      cursor:pointer;
    }
    #kzp-progress::-webkit-slider-thumb{
      -webkit-appearance:none;width:14px;height:14px;
      border-radius:50%;background:var(--accent);
    }
    #kzp-time{
      font-size:10px;color:var(--dim);font-weight:700;
      text-align:right;margin-top:3px;
    }
```

---

## Task 7: Add HTML — kz-player, tab-bar, sections

**Files:**
- Modify: `app/src/main/assets/www/index.html`

- [ ] **Step 1: Add #kz-player before <!-- MAIN SHELL -->**

Find the comment `<!-- MAIN SHELL -->` (around line 656) and insert this block directly before it:

```html
<!-- AUDIO PLAYER (shared for stories and songs) -->
<div id="kz-player" class="h">
  <div class="kzp-top">
    <span id="kzp-title"></span>
    <button class="kzp-btn" id="kzp-play" onclick="playerToggle()">⏸</button>
    <button class="kzp-btn" onclick="playerStop()">■</button>
  </div>
  <input type="range" id="kzp-progress" value="0" min="0" max="100" step="1"
         oninput="playerSeek(this.value)">
  <div id="kzp-time">0:00 / 0:00</div>
</div>

```

- [ ] **Step 2: Add #tab-bar inside #shell, between #hdr and #main**

Find the line `<!-- MAIN -->` (which precedes `<div id="main">`) inside `#shell`, and insert the tab bar before it:

```html
  <!-- TAB BAR -->
  <div id="tab-bar">
    <button class="kz-tab active" id="tab-games" onclick="switchTab('games')">
      🎮 <span id="tab-lbl-games">O'yinlar</span>
    </button>
    <button class="kz-tab" id="tab-stories" onclick="switchTab('stories')">
      📖 <span id="tab-lbl-stories">Ertaklar</span>
    </button>
    <button class="kz-tab" id="tab-songs" onclick="switchTab('songs')">
      🎵 <span id="tab-lbl-songs">Qo'shiqlar</span>
    </button>
  </div>

```

- [ ] **Step 3: Wrap existing #main content in #games-section**

Inside `<div id="main">`, add `<div id="games-section">` immediately after the opening `<div id="main">` tag, and add `</div><!-- /#games-section -->` immediately before `</div><!-- /#main -->`.

The result should look like:
```html
  <div id="main">
    <div id="games-section">
      <!-- HERO -->
      ... (all existing content unchanged) ...
      <!-- PARENT BANNER -->
      <div id="parent-banner">...</div>
    </div><!-- /#games-section -->
  </div><!-- /#main -->
```

- [ ] **Step 4: Add #stories-section inside #main, after #games-section**

After `</div><!-- /#games-section -->` and before `</div><!-- /#main -->`, add:

```html
    <!-- STORIES SECTION -->
    <div id="stories-section" class="h">
      <div class="cs-search-wrap">
        <input class="cs-search" type="text" id="stories-search"
               placeholder="🔍 Ertak qidirish..."
               oninput="storyFilter(this.value, window._storyCat||'all')">
      </div>
      <div class="cc-cats" id="stories-cats">
        <button class="cc-chip active" onclick="storyFilter(document.getElementById('stories-search').value,'all',this)">🎯 <span id="scat-all">Hammasi</span></button>
        <button class="cc-chip" onclick="storyFilter(document.getElementById('stories-search').value,'animals',this)">🐾 <span id="scat-animals">Hayvonlar</span></button>
        <button class="cc-chip" onclick="storyFilter(document.getElementById('stories-search').value,'nature',this)">🌿 <span id="scat-nature">Tabiat</span></button>
        <button class="cc-chip" onclick="storyFilter(document.getElementById('stories-search').value,'heroes',this)">🦸 <span id="scat-heroes">Qahramonlar</span></button>
        <button class="cc-chip" onclick="storyFilter(document.getElementById('stories-search').value,'family',this)">👨‍👩‍👧 <span id="scat-family">Oila</span></button>
        <button class="cc-chip" onclick="storyFilter(document.getElementById('stories-search').value,'space',this)">🚀 <span id="scat-space">Koinot</span></button>
      </div>
      <div class="content-grid" id="stories-grid"></div>
    </div><!-- /#stories-section -->

    <!-- SONGS SECTION -->
    <div id="songs-section" class="h">
      <div class="cs-search-wrap">
        <input class="cs-search" type="text" id="songs-search"
               placeholder="🔍 Qo'shiq qidirish..."
               oninput="songFilter(this.value, window._songCat||'all')">
      </div>
      <div class="cc-cats" id="songs-cats">
        <button class="cc-chip active" onclick="songFilter(document.getElementById('songs-search').value,'all',this)">🎯 <span id="socat-all">Hammasi</span></button>
        <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'lullaby',this)">🌙 <span id="socat-lullaby">Allalar</span></button>
        <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'alphabet',this)">🔤 <span id="socat-alphabet">Alifbo</span></button>
        <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'animals',this)">🐾 <span id="socat-animals">Hayvonlar</span></button>
        <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'dance',this)">💃 <span id="socat-dance">Raqs</span></button>
        <button class="cc-chip" onclick="songFilter(document.getElementById('songs-search').value,'games',this)">🎮 <span id="socat-games">O'yin</span></button>
      </div>
      <div class="content-grid" id="songs-grid"></div>
    </div><!-- /#songs-section -->
```

---

## Task 8: Add i18n keys and bridge functions to index.html

**Files:**
- Modify: `app/src/main/assets/www/index.html`

- [ ] **Step 1: Add i18n keys to T.uz**

Inside the `uz:` object of T (in the inline `<script>` block), after the last key before `}` of `uz`, add:

```js
        tabGames:"O'yinlar", tabStories:"Ertaklar", tabSongs:"Qo'shiqlar",
        searchStory:"Ertak qidirish...", searchSong:"Qo'shiq qidirish...",
        catAll:"Hammasi", catAnimals:"Hayvonlar", catNature:"Tabiat",
        catHeroes:"Qahramonlar", catFamily:"Oila", catSpace:"Koinot",
        catLullaby:"Allalar", catAlphabet:"Alifbo", catDance:"Raqs", catGamesLabel:"O'yin",
        noAudio:"Audio fayl yo'q 🎵",
```

- [ ] **Step 2: Add i18n keys to T.ru**

Inside the `ru:` object, add:

```js
        tabGames:"Игры", tabStories:"Сказки", tabSongs:"Песни",
        searchStory:"Поиск сказки...", searchSong:"Поиск песни...",
        catAll:"Все", catAnimals:"Животные", catNature:"Природа",
        catHeroes:"Герои", catFamily:"Семья", catSpace:"Космос",
        catLullaby:"Колыбельные", catAlphabet:"Азбука", catDance:"Танцы", catGamesLabel:"Игры",
        noAudio:"Аудио недоступно 🎵",
```

- [ ] **Step 3: Add i18n keys to T.en**

Inside the `en:` object, add:

```js
        tabGames:"Games", tabStories:"Stories", tabSongs:"Songs",
        searchStory:"Search story...", searchSong:"Search song...",
        catAll:"All", catAnimals:"Animals", catNature:"Nature",
        catHeroes:"Heroes", catFamily:"Family", catSpace:"Space",
        catLullaby:"Lullabies", catAlphabet:"Alphabet", catDance:"Dance", catGamesLabel:"Games",
        noAudio:"Audio unavailable 🎵",
```

- [ ] **Step 4: Add bridge functions and tab i18n update**

After the existing bridge functions block (after `function openAiMusic() {...}`), add:

```js
    function switchTab(tab) {
        app.ui.switchTab(tab);
        if (tab === 'stories') app.storyManager?.render();
        if (tab === 'songs') app.songManager?.render();
    }

    window._storyCat = 'all';
    window._songCat  = 'all';

    function storyFilter(query, cat, chipEl) {
        if (chipEl) {
            document.querySelectorAll('#stories-cats .cc-chip').forEach(c => c.classList.remove('active'));
            chipEl.classList.add('active');
        }
        window._storyCat = cat || window._storyCat;
        app.storyManager?.filter(query, window._storyCat);
        app.storyManager?.render();
    }

    function songFilter(query, cat, chipEl) {
        if (chipEl) {
            document.querySelectorAll('#songs-cats .cc-chip').forEach(c => c.classList.remove('active'));
            chipEl.classList.add('active');
        }
        window._songCat = cat || window._songCat;
        app.songManager?.filter(query, window._songCat);
        app.songManager?.render();
    }

    function playerToggle() {
        const active = localStorage.getItem('kz-tab');
        if (active === 'stories') app.storyManager?.togglePlay();
        else if (active === 'songs') app.songManager?.togglePlay();
    }

    function playerStop() {
        app.storyManager?.stop();
        app.songManager?.stop();
        app.audioPlayer?.stop();
        const player = document.getElementById('kz-player');
        if (player) player.classList.add('h');
        document.getElementById('main')?.classList.remove('with-player');
    }

    function playerSeek(val) {
        app.audioPlayer?.seek(parseFloat(val));
    }
```

- [ ] **Step 5: Update updateLangUI() to refresh tab labels and content grids**

In the existing `updateLangUI()` function, after the last block (after `buildGrid(app.cat || 'all')`), add:

```js
        // Tab labels
        const t2 = T[app.translator.lang];
        const tg = document.getElementById('tab-lbl-games');
        const ts = document.getElementById('tab-lbl-stories');
        const tso = document.getElementById('tab-lbl-songs');
        if (tg) tg.textContent = t2.tabGames;
        if (ts) ts.textContent = t2.tabStories;
        if (tso) tso.textContent = t2.tabSongs;

        // Story search placeholder
        const ssearch = document.getElementById('stories-search');
        if (ssearch) ssearch.placeholder = '🔍 ' + t2.searchStory;
        const sosearch = document.getElementById('songs-search');
        if (sosearch) sosearch.placeholder = '🔍 ' + t2.searchSong;

        // Story category chip labels
        const scatMap = {
            'scat-all':t2.catAll,'scat-animals':t2.catAnimals,'scat-nature':t2.catNature,
            'scat-heroes':t2.catHeroes,'scat-family':t2.catFamily,'scat-space':t2.catSpace
        };
        Object.entries(scatMap).forEach(([id,lbl]) => {
            const el = document.getElementById(id); if (el) el.textContent = lbl;
        });
        const socatMap = {
            'socat-all':t2.catAll,'socat-lullaby':t2.catLullaby,'socat-alphabet':t2.catAlphabet,
            'socat-animals':t2.catAnimals,'socat-dance':t2.catDance,'socat-games':t2.catGamesLabel
        };
        Object.entries(socatMap).forEach(([id,lbl]) => {
            const el = document.getElementById(id); if (el) el.textContent = lbl;
        });

        // Re-render content grids in new language
        app.storyManager?.render();
        app.songManager?.render();
```

- [ ] **Step 6: Commit all index.html changes**

```bash
git add app/src/main/assets/www/index.html
git commit -m "feat(index.html): add stories/songs tabs, sections, player, i18n, bridge functions"
```

---

## Task 9: Build APK and install on device

**Files:** none (build artifacts)

- [ ] **Step 1: Increment versionCode in app/build.gradle**

Change `versionCode 8` to `versionCode 9`.

- [ ] **Step 2: Build debug APK**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL` — output at `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: Install on connected device**

```bash
/c/Users/Komiljon/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r -t app/build/outputs/apk/debug/app-debug.apk
```

Expected: `Success`

- [ ] **Step 4: Launch app**

```bash
/c/Users/Komiljon/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n "uz.kidzone.app.debug/uz.kidzone.app.MainActivity"
```

---

## Task 10: Test on device

- [ ] **Step 1: Verify tab bar appears** — Three tabs visible at top: 🎮 O'yinlar | 📖 Ertaklar | 🎵 Qo'shiqlar

- [ ] **Step 2: Games tab still works** — O'yinlar tab is default, all games visible, existing functionality intact

- [ ] **Step 3: Stories tab** — Tap "Ertaklar"; search input and category chips appear; 20 story cards render

- [ ] **Step 4: Category filter** — Tap "Hayvonlar"; only animal stories appear (4 cards)

- [ ] **Step 5: Search** — Type "sher"; only "Sher va Sichqon" card shows

- [ ] **Step 6: Card tap** — Tap a story card; `#kz-player` appears above bottom nav with title; audio starts (or shows "Audio unavailable" toast if no MP3 file yet — both are correct)

- [ ] **Step 7: Player controls** — Tap ⏸ to pause → button becomes ▶; tap ▶ to resume; tap ■ to stop and hide player

- [ ] **Step 8: Songs tab** — Repeat steps 3–7 for Qo'shiqlar tab

- [ ] **Step 9: Language switch** — Switch language to RU; tab labels update; card titles update to Russian; category chip labels update

- [ ] **Step 10: Tab persistence** — Switch to Stories tab, background the app, reopen — Stories tab should still be active

- [ ] **Step 11: Bottom nav returns to games** — While in Stories tab, tap any bottom nav button (Home, Play, Learn) — switches back to Games tab

- [ ] **Step 12: Commit final**

```bash
git add app/build.gradle
git commit -m "feat(stories-songs): complete implementation — versionCode 9"
```
