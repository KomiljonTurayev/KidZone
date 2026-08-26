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

/**
 * KidZone Main Application Logic
 * Refactored using SOLID principles.
 */

class TranslationManager {
    constructor(translations) {
        this.translations = translations;
        this.lang = localStorage.getItem(profileManager.key('lang')) || "en";
    }

    setLanguage(lang) {
        this.lang = lang;
        localStorage.setItem(profileManager.key('lang'), lang);
    }

    get(key) {
        return (this.translations[this.lang] && this.translations[this.lang][key]) || key;
    }

    getBatch(key) {
        return (this.translations[this.lang] && this.translations[this.lang][key]);
    }
}

class UIManager {
    constructor() {
    }

    showToast(msg) {
        const t = document.getElementById("toast");
        if (t) {
            t.textContent = msg;
            t.classList.add("s");
            setTimeout(() => t.classList.remove("s"), 2500);
        }
    }

    updateMusicUI(isMuted) {
        const el = document.getElementById("music-ic");
        if (el) el.textContent = isMuted ? "🔇" : "🔊";
    }

    updateLevelUI(pts, lv) {
        const nameEl = document.getElementById("lv-name");
        const subEl = document.getElementById("lv-sub");
        const barEl = document.getElementById("lv-bar");
        const ptsEl = document.getElementById("lv-pts");
        const valEl = document.getElementById("stars-val");

        if (nameEl) nameEl.textContent = lv.name;
        if (subEl) subEl.textContent = lv.sub || `${pts} / ${lv.next} ball`;
        if (barEl) barEl.style.width = lv.pct + "%";
        if (ptsEl) ptsEl.textContent = pts;
        if (valEl) valEl.textContent = pts;
    }

    openModal(id) {
        const el = document.getElementById(id);
        if (el) el.classList.remove("h");
    }

    closeModal(id) {
        const el = document.getElementById(id);
        if (el) el.classList.add("h");
    }

    switchTab(tab) {
        localStorage.setItem('kz-tab', tab);
        document.querySelectorAll('.kz-tab').forEach(t => t.classList.remove('active'));
        const tabEl = document.getElementById('tab-' + tab);
        if (tabEl) tabEl.classList.add('active');
        ['games', 'stories', 'songs'].forEach(s => {
            const el = document.getElementById(s + '-section');
            if (el) el.classList.toggle('h', s !== tab);
        });
        // Mascot only on games tab — free up space on content tabs
        const mascot = document.getElementById('kt-mascot');
        if (mascot) mascot.style.display = tab === 'games' ? '' : 'none';
        // Scroll to top on every tab switch
        const main = document.getElementById('main');
        if (main) main.scrollTop = 0;
    }
}

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
        return !!(this._audio && !this._audio.paused && !this._paused);
    }

    isPaused() {
        return this._paused;
    }
}

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

    load() {
        return new Promise(resolve => {
            const xhr = new XMLHttpRequest();
            xhr.open('GET', 'content.json', true);
            xhr.onload = () => {
                try {
                    const data = JSON.parse(xhr.responseText);
                    this.items = data[this.type] || [];
                } catch (_) { this.items = []; }
                this.filtered = [...this.items];
                resolve();
            };
            xhr.onerror = () => { this.items = []; this.filtered = []; resolve(); };
            xhr.send();
        });
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
        const countId = this.type === 'stories' ? 'stories-count' : 'songs-count';
        const grid = document.getElementById(gridId);
        if (!grid) return;
        const lang = this.translator.lang;

        // Update count display
        const countEl = document.getElementById(countId);
        if (countEl) {
            const n = this.filtered.length;
            const word = this.type === 'stories'
                ? (lang==='uz'?'ta ertak':lang==='ru'?'сказок':'stories')
                : (lang==='uz'?"ta qo'shiq":lang==='ru'?'песен':'songs');
            countEl.textContent = n > 0 ? n + ' ' + word : '';
        }

        grid.innerHTML = '';
        if (this.filtered.length === 0) {
            grid.style.display = 'block';
            grid.innerHTML =
                '<div class="empty-state">' +
                '<div class="empty-state-icon">' + (this.type==='stories'?'📖':'🎵') + '</div>' +
                '<div class="empty-state-title">' + (lang==='uz'?'Hech narsa topilmadi':lang==='ru'?'Ничего не найдено':'Nothing found') + '</div>' +
                '<div class="empty-state-sub">' + (lang==='uz'?"Boshqa so'z sinab ko'ring":lang==='ru'?'Попробуйте другой запрос':'Try a different search') + '</div>' +
                '</div>';
            return;
        }
        grid.style.display = '';

        // Category labels map
        const catLabels = {
            animals: lang==='uz'?'Hayvonlar':lang==='ru'?'Животные':'Animals',
            nature:  lang==='uz'?'Tabiat':lang==='ru'?'Природа':'Nature',
            heroes:  lang==='uz'?'Qahramonlar':lang==='ru'?'Герои':'Heroes',
            family:  lang==='uz'?'Oila':lang==='ru'?'Семья':'Family',
            space:   lang==='uz'?'Koinot':lang==='ru'?'Космос':'Space',
            lullaby: lang==='uz'?'Alla':lang==='ru'?'Колыбельная':'Lullaby',
            alphabet:lang==='uz'?'Alifbo':lang==='ru'?'Азбука':'Alphabet',
            dance:   lang==='uz'?'Raqs':lang==='ru'?'Танец':'Dance',
            games:   lang==='uz'?"O'yin":lang==='ru'?'Игры':'Games'
        };

        this.filtered.forEach(item => {
            const card = document.createElement('div');
            card.className = 'content-card' + (item.id === this.currentId ? ' playing' : '');
            const title = item.title[lang] || item.title.en;
            const catLabel = catLabels[item.category] || item.category;
            const isPlaying = item.id === this.currentId;
            card.innerHTML =
                '<div class="cc-art">' + item.emoji + '</div>' +
                '<div class="cc-title">' + title + '</div>' +
                '<div class="cc-cat">' + catLabel + '</div>' +
                '<div class="cc-ic">' + (isPlaying ? (this.player.isPlaying() ? '⏸' : '▶') : '▶') + '</div>';
            card.onclick = () => this._play(item);
            grid.appendChild(card);
        });
    }

    _play(item) {
        const lang = this.translator.lang;
        const src = item.audio[lang] || item.audio.en;
        const title = item.title[lang] || item.title.en;

        this.currentId = item.id;
        this.render();
        this._showPlayer(item.emoji + ' ' + title);

        this.player.play(
            src, title,
            (cur, dur) => this._onTimeUpdate(cur, dur),
            () => this._onEnded(),
            () => {
                this._onAudioError();
                this._hidePlayer();
                this.currentId = null;
                this.render();
            }
        );
        const playBtn = document.getElementById('kzp-play');
        if (playBtn) playBtn.textContent = '⏸';
    }

    _onAudioError() {
        this.ui.showToast(this.translator.get('noAudio') || 'Audio unavailable');
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
            const btn = document.getElementById('kzp-play');
            if (btn) btn.textContent = '▶';
            this.render();
        } else if (this.player.isPaused()) {
            this.player.resume();
            const btn = document.getElementById('kzp-play');
            if (btn) btn.textContent = '⏸';
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

    _play(item) {
        if (window.badgeManager) badgeManager.onStoryPlayed();
        const lang = this.translator.lang;
        const title = item.title[lang] || item.title.en;
        const text = item.text ? (item.text[lang] || item.text.en || '') : '';

        // Show text viewer and speak immediately (user gesture still active)
        if (text) {
            document.getElementById('aiv-story-title').textContent = item.emoji + ' ' + title;
            document.getElementById('aiv-content').textContent = text;
            document.getElementById('aiv-content-wrap').classList.remove('h');
            document.getElementById('aiv-loading').classList.add('h');
            document.getElementById('ai-viewer').classList.remove('h');
            document.getElementById('aiv-title-text').textContent = title;
            // Speak directly — no timeout so Android gesture window stays active
            if (window.app?._doSpeak) window.app._doSpeak.call(window.app);
        }

        // Also try audio; hide player on error
        const src = item.audio ? (item.audio[lang] || item.audio.en) : null;
        if (src) {
            this.currentId = item.id;
            this.render();
            this._showPlayer(item.emoji + ' ' + title);
            this.player.play(
                src, title,
                (cur, dur) => this._onTimeUpdate(cur, dur),
                () => this._onEnded(),
                () => { this._hidePlayer(); this.currentId = null; this.render(); }
            );
            const playBtn = document.getElementById('kzp-play');
            if (playBtn) playBtn.textContent = '⏸';
        }
    }
}

class SongManager extends ContentManager {
    constructor(player, translator, ui) {
        super('songs', player, translator, ui);
    }

    _play(item) {
        if (window.badgeManager) badgeManager.onSongPlayed();
        const lang = this.translator.lang;
        const title = item.title?.[lang] || item.title?.en || '';
        const text = item.text ? (item.text[lang] || item.text.en || '') : '';
        if (text) {
            document.getElementById('lv-title').textContent = item.emoji + ' ' + title;
            document.getElementById('lv-song-title').textContent = item.emoji + ' ' + title;
            document.getElementById('lv-lyrics').textContent = text;
            document.getElementById('lyrics-viewer').classList.remove('h');
        }
        super._play(item);
    }

    _onAudioError() {
        const text = document.getElementById('lv-lyrics')?.textContent;
        if (text) this._speakLyrics(text, this.translator.lang);
    }

    _speakLyrics(text, lang) {
        if (!window.speechSynthesis) return;
        window.speechSynthesis.cancel();
        const langMap = { uz: 'uz-UZ', ru: 'ru-RU', en: 'en-US' };
        const targetLang = langMap[lang] || 'en-US';
        const voices = window.speechSynthesis.getVoices();
        const voice = voices.find(v => v.lang === targetLang)
                   || voices.find(v => v.lang.startsWith(targetLang.split('-')[0]))
                   || (lang === 'uz' ? voices.find(v => v.lang.startsWith('ru')) : null)
                   || null;
        const msg = new SpeechSynthesisUtterance(text);
        if (voice) msg.voice = voice;
        msg.lang = voice ? voice.lang : targetLang;
        msg.rate = 0.9;
        window.speechSynthesis.speak(msg);
    }
}

class GameManager {
    constructor(games, ui, translator) {
        this.games = games;
        this.ui = ui;
        this.translator = translator;
        this.pts = parseInt(localStorage.getItem(profileManager.key('pts')) || "0");
        this.age = localStorage.getItem(profileManager.key('age')) || "2-4";
        this.isMuted = localStorage.getItem("kz-muted") === "true";
        this.cat = "all";
        this.pinEntry = "";
        this.currentGameId = null;
        if (window.AndroidChallenge) {
            const list = JSON.stringify(this.games.map(function(g) {
                return {
                    id: g.id,
                    title: typeof g.name === 'object' ? (g.name.uz || g.name.en) : g.name
                };
            }));
            window.AndroidChallenge.onGamesLoaded(list);
        }
    }

    addPoints(n) {
        this.pts += n;
        localStorage.setItem(profileManager.key('pts'), this.pts);
        this.updateProgress();
        if (window.badgeManager) badgeManager.onPointsUpdated(this.pts);
    }

    updateProgress() {
        const lv = this.getLevelData(this.pts);
        const lvNextText = this.translator.get('lvNext');
        lv.sub = `${this.pts} / ${lv.next} ${lvNextText}`;
        this.ui.updateLevelUI(this.pts, lv);
    }

    getLevelData(p) {
        const lvNames = this.translator.getBatch('lvNames') || ["New", "New", "New", "New", "New", "New"];
        const levels = [
            { min: 0, max: 100, idx: 0, icon: "🌱" },
            { min: 100, max: 300, idx: 1, icon: "🌟" },
            { min: 300, max: 600, idx: 2, icon: "🏅" },
            { min: 600, max: 1000, idx: 3, icon: "🥇" },
            { min: 1000, max: 2000, idx: 4, icon: "👑" },
            { min: 2000, max: 9999, idx: 5, icon: "🧠" },
        ];
        const lv = levels.find(l => p >= l.min && p < l.max) || levels[levels.length - 1];
        const pct = Math.min(100, ((p - lv.min) / (lv.max - lv.min)) * 100);
        return { ...lv, name: lv.icon + " " + lvNames[lv.idx], pct, next: lv.max };
    }

    setLang(l) {
        this.translator.setLanguage(l);
        if (window.updateLangUI) window.updateLangUI();

        // Sync with Native
        if (window.AndroidBridge && window.AndroidBridge.updateLanguage) {
            window.AndroidBridge.updateLanguage(l);
        }

        document.querySelectorAll('#lang-modal .age-card').forEach(c => c.classList.remove('sel'));
        document.getElementById('lc-' + l)?.classList.add('sel');

        const flags = {en:"🇬🇧 EN", ru:"🇷🇺 RU", uz:"🇺🇿 UZ"};
        const curEl = document.getElementById("lang-cur");
        if (curEl) curEl.textContent = flags[l];

        if (window.buildFeatured) window.buildFeatured();
        if (window.buildGrid) window.buildGrid(this.cat);

        this.ui.showToast(this.translator.get('langSelected') || l);
    }

    setAge(a, el) {
        this.age = a;
        localStorage.setItem(profileManager.key('age'), a);
        document.querySelectorAll(".age-card").forEach(x => x.classList.remove("sel"));
        if (el) el.classList.add("sel");
        const ageBtn = document.getElementById("age-btn");
        if (ageBtn) ageBtn.textContent = `👶 ${a} ▾`;
        if (window.buildGrid) window.buildGrid(this.cat);
    }

    toggleMusic() {
        this.isMuted = !this.isMuted;
        localStorage.setItem("kz-muted", this.isMuted);
        this.ui.updateMusicUI(this.isMuted);
        if (window.AndroidBridge?.toggleMusic) {
            window.AndroidBridge.toggleMusic(this.isMuted);
        }
    }

    filterCat(c, el) {
        this.cat = c;
        document.querySelectorAll(".cat-chip").forEach(x => x.classList.remove("active"));
        if (el) el.classList.add("active");
        if (window.buildGrid) window.buildGrid(c);
    }

    navTo(pg) {
        if (window.app?.ui?.switchTab) app.ui.switchTab('games');
        document.querySelectorAll(".ni").forEach(n => n.classList.remove("on"));
        const niEl = document.getElementById("ni-" + pg);
        if (niEl) niEl.classList.add("on");

        const chips = document.querySelectorAll('.cat-chip');
        if (pg === 'home') {
            const main = document.getElementById('main');
            if (main) main.scrollTo({ top: 0, behavior: 'smooth' });
            this.filterCat('all', chips[0]);
        } else if (pg === 'play') {
            this.filterCat('all', chips[0]);
            const featured = document.getElementById('featured-row');
            if (featured) featured.scrollIntoView({ behavior: 'smooth' });
            this.ui.showToast("🎮 " + this.translator.get('playMode'));
        } else if (pg === 'learn') {
            this.filterCat('learn', chips[1]);
            const grid = document.getElementById('game-grid');
            if (grid) grid.scrollIntoView({ behavior: 'smooth' });
            this.ui.showToast("📚 " + this.translator.get('learnMode'));
        } else if (pg === 'stars') {
            const lc = document.getElementById('level-card');
            if (lc) lc.scrollIntoView({ behavior: 'smooth' });
            this.ui.showToast("⭐ " + this.translator.get('yourStars') + ": " + this.pts);
        }
    }

    openGame(g, locked = false) {
        if (locked) {
            this.ui.showToast(this.translator.get('locked'));
            return;
        }

        if (!g.file) {
            this.ui.showToast(this.translator.get('soon'));
            this.addPoints(5);
            return;
        }

        this.currentGameId = g.id;

        const titleEl = document.getElementById("gv-title");
        const backEl = document.getElementById("gvb-txt");
        const gameName = typeof g.name === 'object' ? (g.name[this.translator.lang] || g.name.en) : g.name;
        if (titleEl) titleEl.textContent = g.em + " " + gameName;
        if (backEl) backEl.textContent = this.translator.get('back').replace("← ", "");

        const gameUrl = g.file.includes("?") ? `${g.file}&lang=${this.translator.lang}` : `${g.file}?lang=${this.translator.lang}`;
        const frameEl = document.getElementById("gv-frame");
        if (frameEl) frameEl.src = gameUrl;
        const gvEl = document.getElementById("gv");
        if (gvEl) gvEl.classList.remove("h");

        if (window.AndroidBridge) {
            window.AndroidBridge.hideBanner();
        }
        if (window.AndroidChallenge) {
            window.AndroidChallenge.onGameOpened(g.id);
        }
    }

    closeGame() {
        const gvEl = document.getElementById("gv");
        const frameEl = document.getElementById("gv-frame");
        if (gvEl) gvEl.classList.add("h");
        if (frameEl) frameEl.src = "";
        if (window.AndroidBridge) window.AndroidBridge.showBanner();

        const game = this.games.find(g => g.id === this.currentGameId);
        this.currentGameId = null;
        if (game) {
            const earned = game.pts || 20;
            this.addPoints(earned);
            if (window.badgeManager) badgeManager.onGamePlayed(game.cat);
            this.showRewardScreen(earned);
        }
        if (game && window.AndroidChallenge) {
            window.AndroidChallenge.onGameClosed(game.id);
        }
    }

    showRewardScreen(ptsEarned) {
        const stars = ptsEarned >= 40 ? "⭐⭐⭐" : ptsEarned >= 25 ? "⭐⭐" : "⭐";
        const starsEl = document.getElementById("rw-stars");
        const titleEl = document.getElementById("rw-title");
        const subEl = document.getElementById("rw-sub");
        const ptsRewardEl = document.getElementById("rw-pts");
        const rewardEl = document.getElementById("reward-screen");

        if (starsEl) starsEl.textContent = stars;
        if (titleEl) titleEl.textContent = this.translator.get('rewardTitle');
        if (subEl) subEl.textContent = this.translator.get('rewardSub');
        if (ptsRewardEl) ptsRewardEl.textContent = "+" + ptsEarned + " ⭐";
        if (rewardEl) rewardEl.classList.remove("h");
        this.spawnFireworks();
    }

    spawnFireworks() {
        for (let i = 0; i < 8; i++) {
            setTimeout(() => {
                const em = ["🎉", "⭐", "✨", "🌟", "🎊"][Math.floor(Math.random() * 5)];
                const el = document.createElement("div");
                el.style.cssText = `position:fixed;z-index:500;font-size:28px;pointer-events:none;
          left:${20 + Math.random() * 60}vw;top:${10 + Math.random() * 50}vh;
          animation:fall .8s ease-out forwards`;
                el.textContent = em;
                document.body.appendChild(el);
                setTimeout(() => el.remove(), 800);
            }, i * 100);
        }
    }

    pinTap(v) {
        const PIN = "1234";
        if (v === "←") {
            this.pinEntry = this.pinEntry.slice(0, -1);
        } else {
            if (this.pinEntry.length >= 4) return;
            this.pinEntry += v;
        }
        for (let i = 1; i <= 4; i++) {
            const d = document.getElementById("pd" + i);
            if (d) {
                d.classList.toggle("filled", i <= this.pinEntry.length);
                d.textContent = i <= this.pinEntry.length ? "●" : "";
            }
        }
        if (this.pinEntry.length === 4) {
            setTimeout(() => {
                if (this.pinEntry === PIN) {
                    this.ui.closeModal("parent-modal");
                    this.ui.showToast("🔓 " + this.translator.get('parentModeUnlocked'));
                } else {
                    this.ui.showToast("❌ " + this.translator.get('wrongPin'));
                    this.pinEntry = "";
                    for (let i = 1; i <= 4; i++) {
                        const d = document.getElementById("pd" + i);
                        if (d) {
                            d.classList.remove("filled"); d.textContent = "";
                        }
                    }
                }
            }, 300);
        }
    }

    toggleFS() {
        if (!document.fullscreenElement) document.documentElement.requestFullscreen?.();
        else document.exitFullscreen?.();
    }

    // ── AI STUDIO LOGIC ──

    generateAiStory() {
        if (this._aiStoryPending) return;

        const viewer = document.getElementById("ai-viewer");
        const loading = document.getElementById("aiv-loading");
        const content = document.getElementById("aiv-content-wrap");

        document.getElementById('aiv-regen-btn').classList.remove('h');
        viewer.classList.remove("h");
        loading.classList.remove("h");
        content.classList.add("h");

        if (window.AndroidBridge && window.AndroidBridge.generateStory) {
            this._aiStoryPending = true;
            window.AndroidBridge.generateStory(this.translator.lang, this.age);
            return;
        }

        // No native bridge (e.g. a plain browser preview) — use the offline pool directly.
        setTimeout(() => this._showAiStory(this.getAiStory(this.translator.lang)), 500);
    }

    _showAiStory(story) {
        const loading = document.getElementById("aiv-loading");
        const content = document.getElementById("aiv-content-wrap");
        document.getElementById("aiv-story-title").textContent = story.title;
        document.getElementById("aiv-content").textContent = story.text;

        loading.classList.add("h");
        content.classList.remove("h");
        this.addPoints(10);
        this._doSpeak();
    }

    closeAi() {
        document.getElementById("ai-viewer").classList.add("h");
        document.getElementById('aiv-regen-btn').classList.add('h');
        window.speechSynthesis?.cancel();
        const btn = document.getElementById("ai-read-btn");
        if (btn) btn.querySelector('span').textContent = "🔊";
    }

    speakStory() {
        const btn = document.getElementById("ai-read-btn");
        if (window.speechSynthesis?.speaking) {
            window.speechSynthesis.cancel();
            if (btn) btn.querySelector('span').textContent = "🔊";
            return;
        }
        this._doSpeak();
    }

    _doSpeak() {
        if (!window.speechSynthesis) return;
        window.speechSynthesis.cancel();
        const text = document.getElementById("aiv-content")?.textContent;
        if (!text || text.trim().length < 3) return;
        const btn = document.getElementById("ai-read-btn");
        const lang = this.translator.lang;
        const langMap = { uz: "uz-UZ", ru: "ru-RU", en: "en-US" };
        const targetLang = langMap[lang] || "en-US";

        const doSpeak = () => {
            const msg = new SpeechSynthesisUtterance(text);
            const voices = window.speechSynthesis.getVoices();
            // Priority: exact → prefix → Russian fallback for Uzbek → default
            const voice = voices.find(v => v.lang === targetLang)
                       || voices.find(v => v.lang.startsWith(targetLang.split('-')[0]))
                       || (lang === 'uz' ? voices.find(v => v.lang.startsWith('ru')) : null)
                       || voices.find(v => v.default) || null;
            if (voice) msg.voice = voice;
            msg.lang = voice ? voice.lang : targetLang;
            msg.rate = 0.86;
            msg.pitch = 1.0;
            msg.volume = 1.0;
            msg.onstart = () => { if (btn) btn.querySelector('span').textContent = "⏹️"; };
            msg.onend   = () => { if (btn) btn.querySelector('span').textContent = "🔊"; };
            msg.onerror = (e) => {
                if (btn) btn.querySelector('span').textContent = "🔊";
                if (e.error !== 'interrupted') this.ui?.showToast("🔊 Ovoz ishlamadi");
            };
            window.speechSynthesis.speak(msg);
        };

        const voices = window.speechSynthesis.getVoices();
        if (voices.length > 0) {
            doSpeak();
        } else {
            // Wait for voices to load (Android WebView async)
            let fired = false;
            window.speechSynthesis.onvoiceschanged = () => {
                if (fired) return;
                fired = true;
                window.speechSynthesis.onvoiceschanged = null;
                doSpeak();
            };
            setTimeout(() => { if (!fired) { fired = true; doSpeak(); } }, 500);
        }
    }

    openAiMusic() {
        this.openGame({id:"piano", em:"🎹", name:this.translator.get("aiBtnMusic"), file:"instrument.html", pts:15});
    }

    getAiStory(lang) {
        const stories = {
            uz: [
                {
                    title: "Sehrli O'rmon",
                    text: "Bir bor ekan, bir yo'q ekan, uzoq bir o'lkada sehrli o'rmon bor ekan. Bu o'rmonda daraxtlar shokoladdan, barglar esa shirinliklardan iborat ekan. Gullar qulupnay hidini taratardi, irmoqlar esa limonad bilan oqardi.\n\nBir kuni kichkina filcha o'rmonda sayr qilib yurib, oltin kalit topib olibdi. U qaysi eshikka tegishli ekanini bilmay, uzoq o'yladi. Shunda unga rangli kapalak uchib keldi.\n\n'Men bilaman bu kalit nima uchun,' dedi kapalak. 'Keling, ko'rsataman!' Ular birga yo'lga chiqishdi. Limonad daryosini kechib o'tib, shokolad daraxtlari orasidan o'tib, o'rmon o'rtasiga yetib kelishdi.\n\nU yerda chiroyli kichkina uy turardi. Eshigida qulf bor edi. Filcha kalitni soldi — eshik ochildi! Ichida kitoblar, o'yinchoqlar va shirinliklar to'la edi.\n\n'Bu — Bilim va Baxt uyi,' dedi kapalak. 'Bu yerda mehribon va aqlli bolalar o'ynaydi.' Filcha kitob o'qiy boshladi. Har kuni yangi narsalar o'rgandi.\n\nTez orada uning ko'p do'stlari bo'ldi. Ular birga o'qir, o'ynar va bir-birlariga yordam berishardi. Chunki bilim va do'stlik — sehrli o'rmonning eng katta boyligi."
                },
                {
                    title: "Koinot Sayohati",
                    text: "Jasur ismli bolakay har kecha yulduzlarga qarab uxlashni yaxshi ko'rardi. U doim orzular qilardi: 'Bir kun yulduzlarga borarman!'\n\nBir kuni uning derazasiga kichkina uchuvchi tarelka qo'ndi. Undan chiqqan mitti robot: 'Qani Jasur, ketdik yulduzlar sari!' dedi. Jasur qo'rqmadi va robotga ergashdi.\n\nUchuvchi tarelkada ular osmonga ko'tarildi. Pastda shahar chiroqlari yulduzlardek yiltirar, yuqorida esa haqiqiy yulduzlar porlardi. Avval Oyga to'xtashdi — u oq kumush kabi yaltirardi.\n\nKeyin Quyash sistemasini aylanib chiqdilar. Saturn sayyorasini ko'rdilar — uning halqalari kamalak ranglarida tovlanardi. 'Qanday go'zal!' deb hayqirdi Jasur. Mars esa qizil rangda, toshlar bilan qoplangan edi.\n\n'Koinot juda katta,' dedi robot. 'Bilim ham xuddi shunday — chegarasiz. Qattiq o'qi, Jasur, va kelajakda haqiqiy kosmonavt bo'larsan.'\n\nErtalab Jasur o'z to'shagida uyg'ondi. Lekin qo'lida Marsdan keltirilgan kichkina qizil tosh bor edi. O'shandan beri Jasur ilm olishga yanada mehr qo'ydi. Hamma aytardi: bu bola albatta yulduzlarga yetib boradi!"
                },
                {
                    title: "Aqlli Toshbaqa",
                    text: "Bir vaqtlar ko'l yoqasida Sulton ismli toshbaqa yashardi. Sulton juda sekin yurardi, lekin u juda ko'p narsalar bilardi.\n\nBir kuni quyon unga masxara qilib: 'Hey, toshbaqa! Sen shu qadar sekinsan — hayotda hech narsaga ulgurmaysan!' dedi. Sulton jimgina javob berdi: 'Ko'ramiz.'\n\nErtalab ular poyga o'tkazishga qaror qilishdi. Barcha hayvonlar tomosha qilish uchun kelishdi. Poyga boshlandi. Quyon shiddat bilan yugurib ketdi, Sulton esa sekin-sekin yura boshladi.\n\nQuyon o'rtada bir daraxt tagida dam olishga yotdi. 'Men tezman, hali ham yetib olaman,' deb uxlab qoldi. Sulton esa to'xtamay, qadamba qadam oldinga bordi.\n\nQuyosh botayotganda Sulton finishga birinchi bo'lib yetib keldi. Quyon uyg'onib, orqada qolganini ko'rdi. Uyat bo'ldi.\n\nSulton quyonga dedi: 'Do'stim, tezlik emas, matonat muhim. Kim to'xtamay harakat qilsa — u g'alaba qozonadi.' Quyon bu darsni umrga yodida saqladi."
                },
                {
                    title: "Mehribon Ayiqcha",
                    text: "Tog' etagidagi o'rmonda Booz ismli kichkina ayiqcha yashar ekan. Kuzning so'nggi kunlarida u eng shirin qulupnaylarni topib, butun sabatini to'ldiribdi.\n\nUyiga qaytayotib, yo'lda holdan toygan kichkina quyonchani ko'rib qoldi — qish yaqinlashib, o'rmonda oziq kamayib qolgan edi. Booz bir lahza o'yladi, so'ng sabatining yarmini unga uzatdi. 'Ol, do'stim, birga yeymiz!' dedi u kulib.\n\nUlar yeb o'tirishganida och qolgan kichkina sincap ham kelib qoldi. Booz unga ham ulashdi. Tez orada uchovlon birgalikda yangi qulupnaylar terib, uni teng bo'lib yeyishdi.\n\nKechqurun Booz uyiga bo'sh sabat bilan qaytdi, lekin yuragi juda to'la edi — u ikkita yangi do'st topgan edi. Onasi mehr bilan dedi: 'Ulashish hech qachon kamaytirmaydi — u faqat ko'paytiradi.'\n\nO'shandan beri Booz har doim boshqalar bilan ulashishni yaxshi ko'rardi, chunki bilardi: mehribonlik eng shirin mevadan ham totliroq."
                },
                {
                    title: "Dengiz Sirlari",
                    text: "Marjon rifida Lola ismli kichkina baliqcha yashar ekan. U juda qiziquvchan edi va har kuni yangi joylarni kashf qilishni yaxshi ko'rardi.\n\nBir kuni Lola oddiy suzish yo'lidan chetga chiqib, tanish bo'lmagan g'orga kirib ketdi. Ichkarida u yo'lini yo'qotdi — atrofi qorong'i va notanish edi. Lola qo'rqib ketdi.\n\nShu payt unga mehribon dengiz toshbaqasi Karim duch keldi. 'Nima bo'ldi, kichkintoy?' deb so'radi u. Lola yig'lab, yo'lini yo'qotganini aytdi. Karim unga yordam berishga qaror qildi.\n\nUlar birga suzib, yorug' suv o'simliklariga qarab yo'l topishdi. Yo'lda ular sakkiz oyoqli Rustam va dengiz yulduzi Zebo bilan uchrashishdi — hammasi Lolaga uyiga qaytishda yordam berishdi.\n\nUyiga yetib kelgach, Lola tushundi: yolg'iz qo'rqishdan ko'ra, yordam so'rash va do'stlarga ishonish ancha yaxshi ekan. O'shandan beri u har doim do'stlari bilan birga suzardi — dengiz ham, hayot ham birgalikda go'zalroq edi."
                },
                {
                    title: "Kichkina Bog'bon",
                    text: "Nilufar ismli qizcha bobosidan bir dona kichkina urug' oldi. 'Buni ekib parvarish qil, u senga ajoyib sovg'a bo'ladi,' dedi bobosi kulib.\n\nNilufar urug'ni bog'ga ekdi va har kuni suv berdi. Lekin bir necha kun o'tsa ham hech narsa unmadi. U bezovtalanib: 'Bobojon, nega hech narsa chiqmayapti?' deb so'radi.\n\n'Sabr qil, qizim,' dedi bobosi. 'Har bir go'zal narsa vaqt talab qiladi.' Nilufar davom etib, har kuni g'amxo'rlik qilishda davom etdi — suv berdi, quyoshga chiqardi, begona o'tlardan tozaladi.\n\nBir necha hafta o'tgach, tuproqdan yashil nihol boshini chiqardi! Nilufar juda xursand bo'ldi. U nihol o'sishini har kuni kuzatib bordi, toki u chiroyli qizil gulga aylanguncha.\n\nO'sha kuni Nilufar tushundi: sabr va g'amxo'rlik bilan eng kichik urug' ham eng go'zal gulga aylanishi mumkin ekan."
                },
                {
                    title: "Yorqin Yulduzcha",
                    text: "Osmon uzra yashagan kichkina yulduzcha Chiroq o'zini boshqa katta va yorqin yulduzlar orasida arzimas his qilardi. 'Men juda kichkinaman, mening nurim hech kim ko'rmaydi,' deb qayg'urardi u har kuni.\n\nBir kecha yerdagi kichkina qiz osmonga qarab, eng yorqin emas, balki eng yaqin va mehribon ko'rinuvchi yulduzni topmoqchi bo'ldi. Uning ko'zi Chiroqqa tushdi.\n\n'Sen ajoyib porlab turibsan!' dedi qizcha shivirlab, ko'z yumib tilak tilarkan. Chiroq hayron bo'ldi — u o'zini shunchalik kichkina deb o'ylagandi, biroq kimdir uni ko'rgan va yoqtirgan edi.\n\nO'sha kechadan boshlab Chiroq tushundi: katta yoki kichik bo'lishning ahamiyati yo'q — muhimi, har bir yulduz o'z nuri bilan kimningdir kechasini yoritishi mumkin.\n\nShundan beri Chiroq har kecha g'urur bilan porlaydi, chunki biladi: uning kichkina nuri ham kimgadir juda katta ahamiyatga ega."
                },
                {
                    title: "Do'stlik Ko'prigi",
                    text: "Katta daryoning ikki tomonida ikkita hayvonlar oilasi yashar edi — bir tomonda quyonlar, ikkinchi tomonda sincoblar. Ular bir-birlarini ko'rar, lekin daryo ularni ajratib turardi.\n\nBir kuni kichkina quyon Aziz daryo bo'yida o'ynab, narigi tomondagi sincobcha Malika bilan tanishdi. Ular gaplashib, birga o'ynashni juda xohlashdi, lekin daryodan o'tib bo'lmasdi.\n\n'Nega ko'prik qurmaymiz?' deb taklif qildi Aziz. Malika bu g'oyani juda yoqtirdi. Ular hayvonlarni yig'ib, birga ishlashga chaqirishdi — quyonlar tosh tashidi, sincoblar novdalardan mahkam bog'ladi, qunduzlar esa ustalik bilan qurishga yordam berishdi.\n\nBir necha kundan so'ng mustahkam va chiroyli ko'prik tayyor bo'ldi! Endi ikkala tomondagi oilalar erkin o'tib, birga o'ynay olardi.\n\nAziz va Malika tushunishdi: birgalikda ishlasang, hech qanday to'siq — hatto katta daryo ham — do'stlikka to'sqinlik qila olmaydi."
                },
                {
                    title: "Botir Kichkina Qush",
                    text: "Baland daraxt uyasida kichkina qaldirg'och Sardor yashardi. Uning qanotlari allaqachon kuchli edi, lekin u birinchi marta uchishga juda qo'rqardi. 'Ya birdan yiqilsam-chi?' deb o'ylardi u har kuni.\n\nOnasi uni yumshoqlik bilan turtardi: 'Sardor, har bir qush avval qo'rqadi. Lekin uchishni his qilmaguningcha, bilib bo'lmaydi qanchalik go'zal ekanini.'\n\nBir kuni shamol kuchli esdi va Sardor uyadan beixtiyor sirg'alib tushib ketdi. Yuragi shiddat bilan urdi — lekin to'satdan u qanotlarini yozdi va... uchib ketdi! Avval noqulay, keyin esa erkin va yengil.\n\nU osmonda parvoz qilib, pastdagi o'rmonni, daryoni va uyini ko'rdi. Bu qanchalik ajoyib edi! Sardor xursandchilikdan sayrab yubordi.\n\nUyaga qaytganida u tushundi: qo'rquv — bu faqat birinchi qadam oldidagi hissiyot, lekin jasorat bilan tashlangan qadam har doim yangi dunyoni ochadi."
                },
                {
                    title: "To'rt Fasl Sayohati",
                    text: "Bahorda kichkina urug' tuproqdan bosh ko'tarib, yosh niholga aylandi. Quyosh iliq nurlarini sochib, uni parvarish qildi. 'Salom, dunyo!' dedi nihol quvonch bilan birinchi bargini yozarkan.\n\nYoz kelganda nihol baland daraxtga aylandi. Uning shoxlarida qushlar uya qurdi, soyasida bolalar o'ynadi. Kunlar issiq va uzun edi, daraxt esa har kuni kuchliroq bo'lib bordi.\n\nKuz kelganida daraxtning barglari sariq, qizil va to'q sariq ranglarga bo'yaldi. Shamol esganda barglar raqsga tushib, asta-sekin yerga tushardi. Daraxt bu go'zal manzarani kuzatib, o'z go'zalligiga guvoh bo'ldi.\n\nQish kelganida daraxt qorga burkanib, jimgina dam oldi. Sovuq shamollarga qaramay, uning ildizlari chuqurda mahkam va issiq edi — u bahorni sabr bilan kutardi.\n\nVa mana, yana bahor keldi, va daraxt yana yangi barglar chiqardi. U tushundiki: har bir fasl o'ziga xos go'zallikka ega, va o'zgarish — hayotning eng tabiiy va go'zal qismidir."
                }
            ],
            ru: [
                {
                    title: "Волшебный Лес",
                    text: "Жил-был в далёкой стране волшебный лес. В этом лесу деревья были из шоколада, а листья — из конфет. Цветы пахли клубникой, а ручеёк журчал лимонадом.\n\nОднажды маленький слонёнок гулял по лесу и нашёл золотой ключ. Он не знал, к какой двери подходит этот ключ. Вдруг к нему подлетела красивая бабочка.\n\n'Я знаю, что открывает этот ключ,' сказала бабочка. 'Пойдём, я покажу!' Они пошли вместе — через реку из лимонада, мимо деревьев из шоколада.\n\nВ центре леса стоял красивый домик. На двери был замок. Слонёнок вставил ключ — и дверь открылась! Внутри были книги, игрушки и много сладостей.\n\n'Это Дом знаний,' сказала бабочка. 'Здесь живут добрые и умные дети.' Слонёнок сел читать книги. Каждый день он узнавал что-то новое.\n\nСкоро у него появилось много друзей. Все они читали вместе, играли и помогали друг другу. Потому что знание и дружба — настоящие сокровища волшебного леса."
                },
                {
                    title: "Космическое Приключение",
                    text: "Мальчик по имени Максим очень любил смотреть на звёзды. Каждую ночь он сидел у окна и мечтал: 'Когда же я полечу в космос?'\n\nОднажды ночью к его окну прилетела маленькая летающая тарелка. Из неё вышел крошечный робот. 'Максим, пойдём к звёздам!' сказал он.\n\nМаксим не испугался и залез в тарелку. Они взлетели вверх. Город внизу стал маленьким, как игрушка. Вскоре они достигли Луны — белой и круглой, как мяч.\n\nДальше они полетели к Сатурну. Его кольца переливались всеми цветами радуги. 'Красота!' воскликнул Максим. Потом был Марс — красная планета, покрытая камнями и пылью.\n\n'Наша Вселенная очень большая,' сказал робот. 'И знания о ней безграничны. Учись хорошо, Максим, и ты станешь настоящим космонавтом.'\n\nУтром Максим проснулся в своей кровати. Но в руке у него был маленький красный камешек с Марса. С тех пор он стал учиться ещё лучше. И все говорили: этот мальчик точно долетит до звёзд!"
                },
                {
                    title: "Мудрая Черепаха",
                    text: "На берегу озера жила черепаха по имени Тоша. Тоша ходила очень медленно, но знала очень много всего на свете.\n\nОднажды к ней подбежал заяц и засмеялся: 'Эй, черепаха! Ты такая медленная — в жизни ничего не успеешь!' Тоша спокойно ответила: 'Посмотрим.'\n\nНа следующее утро они решили устроить гонку. Все звери пришли посмотреть. Гонка началась. Заяц помчался со всех ног, а Тоша медленно, но уверенно шагала вперёд.\n\nНа полпути заяц решил отдохнуть под деревом. 'Я такой быстрый, всегда успею догнать,' подумал он и уснул. А Тоша шла и шла, не останавливаясь.\n\nКогда солнце клонилось к закату, Тоша первой пересекла финишную черту. Заяц проснулся и увидел, что отстал. Ему стало очень стыдно.\n\nТоша сказала ему: 'Дружок, важна не скорость, а настойчивость. Тот, кто не останавливается, всегда добьётся своего.' Заяц запомнил этот урок на всю жизнь."
                },
                {
                    title: "Добрый Медвежонок",
                    text: "В лесу у подножия гор жил маленький медвежонок по имени Буся. В последние осенние дни он нашёл самые сладкие ягоды и наполнил ими целую корзинку.\n\nПо дороге домой он увидел обессиленного зайчонка — зима приближалась, и еды в лесу становилось всё меньше. Буся на мгновение задумался, а потом протянул зайчонку половину своих ягод. 'Держи, дружок, разделим поровну!' — сказал он с улыбкой.\n\nПока они ели, к ним подбежала голодная белочка. Буся поделился и с ней. Вскоре все трое вместе собирали новые ягоды и делили их поровну.\n\nВечером Буся вернулся домой с пустой корзинкой, но с очень полным сердцем — ведь он нашёл двух новых друзей. Мама ласково сказала ему: 'Делиться — значит не терять, а приобретать ещё больше.'\n\nС тех пор Буся всегда делился с другими, ведь знал: доброта слаще любой самой сладкой ягоды."
                },
                {
                    title: "Тайны Океана",
                    text: "На коралловом рифе жила любопытная маленькая рыбка по имени Лола. Она обожала каждый день открывать для себя что-то новое.\n\nОднажды Лола отплыла в сторону от привычного пути и заплыла в незнакомую пещеру. Внутри она заблудилась — вокруг было темно и всё казалось чужим. Лола очень испугалась.\n\nВдруг ей повстречалась добрая морская черепаха Карим. 'Что случилось, малышка?' — спросил он. Лола, всхлипывая, рассказала, что потерялась. Карим решил помочь ей.\n\nОни вместе поплыли к свету, ориентируясь на морские растения. По пути к ним присоединились осьминог Рустам и морская звезда Зебо — все вместе помогли Лоле найти дорогу домой.\n\nДобравшись до дома, Лола поняла: гораздо лучше попросить о помощи и довериться друзьям, чем бояться в одиночку. С тех пор она всегда плавала вместе с друзьями — ведь и океан, и жизнь красивее вместе."
                },
                {
                    title: "Маленький Садовник",
                    text: "Девочка по имени Нилуфар получила от дедушки маленькое семечко. 'Посади его и заботься о нём, и оно станет для тебя чудесным подарком,' — сказал дедушка с улыбкой.\n\nНилуфар посадила семечко в саду и каждый день поливала его. Но проходили дни, а ничего не появлялось. Она забеспокоилась: 'Дедушка, почему ничего не растёт?'\n\n'Наберись терпения, доченька,' — ответил дедушка. 'Всё прекрасное требует времени.' Нилуфар продолжала заботиться о семечке — поливала, выносила на солнце, убирала сорняки.\n\nЧерез несколько недель из земли показался зелёный росток! Нилуфар очень обрадовалась. Она наблюдала за ним каждый день, пока он не превратился в прекрасный алый цветок.\n\nВ тот день Нилуфар поняла: с терпением и заботой даже самое маленькое семечко может стать самым красивым цветком."
                },
                {
                    title: "Маленькая Яркая Звёздочка",
                    text: "На небе жила маленькая звёздочка по имени Огонёк. Среди больших и ярких звёзд она чувствовала себя совсем незаметной. 'Я такая маленькая, мой свет никто не видит,' — грустила она каждую ночь.\n\nОднажды маленькая девочка на земле смотрела в небо, ища не самую яркую, а самую близкую и добрую на вид звезду. Её взгляд остановился на Огоньке.\n\n'Ты сияешь просто чудесно!' — прошептала девочка, закрыв глаза и загадав желание. Огонёк удивился — она считала себя такой маленькой, а кто-то всё же заметил её и полюбил.\n\nС той ночи Огонёк поняла: неважно, большая ты звезда или маленькая — важно, что своим светом ты можешь осветить чью-то ночь.\n\nС тех пор Огонёк сияет каждую ночь с гордостью, зная: даже маленький свет может значить очень много для кого-то."
                },
                {
                    title: "Мост Дружбы",
                    text: "На двух берегах большой реки жили две семьи зверей — на одном берегу зайцы, на другом белки. Они видели друг друга издалека, но река их разделяла.\n\nОднажды маленький зайчонок Азиз играл у реки и познакомился с белочкой Маликой с другого берега. Они разговорились и очень захотели играть вместе, но переплыть реку было невозможно.\n\n'А давай построим мост?' — предложил Азиз. Малике идея очень понравилась. Они собрали зверей и позвали всех работать вместе — зайцы носили камни, белки крепко связывали ветки, а бобры помогали строить с мастерством.\n\nЧерез несколько дней прочный и красивый мост был готов! Теперь семьи с обоих берегов могли свободно переходить и играть вместе.\n\nАзиз и Малика поняли: если работать сообща, никакая преграда — даже большая река — не сможет помешать дружбе."
                },
                {
                    title: "Храбрая Птичка",
                    text: "На высоком дереве в гнезде жил маленький птенец ласточки по имени Сардор. Его крылья уже окрепли, но он очень боялся своего первого полёта. 'А вдруг я упаду?' — думал он каждый день.\n\nМама нежно подбадривала его: 'Сардор, каждая птица сначала боится. Но пока ты не почувствуешь полёт, не узнаешь, как это прекрасно.'\n\nОднажды подул сильный ветер, и Сардор невольно соскользнул с гнезда. Его сердце забилось быстро-быстро — но вдруг он расправил крылья и... полетел! Сначала неуверенно, а потом свободно и легко.\n\nОн парил в небе и видел внизу лес, реку и своё гнездо. Это было потрясающе! Сардор радостно защебетал.\n\nВернувшись в гнездо, он понял: страх — это лишь чувство перед первым шагом, но шаг, сделанный смело, всегда открывает новый мир."
                },
                {
                    title: "Путешествие по Четырём Временам Года",
                    text: "Весной маленькое семечко пробилось из земли и превратилось в молодой росток. Солнце согревало его тёплыми лучами. 'Здравствуй, мир!' — радостно сказал росток, раскрывая первый листок.\n\nК лету росток превратился в высокое дерево. На его ветвях свили гнёзда птицы, а в его тени играли дети. Дни были тёплыми и долгими, а дерево становилось всё сильнее.\n\nОсенью листья дерева окрасились в жёлтый, красный и оранжевый цвета. Когда дул ветер, листья кружились в танце и медленно опускались на землю. Дерево наблюдало за этой красотой и гордилось собой.\n\nЗимой дерево укуталось снегом и тихо отдыхало. Несмотря на холодные ветра, его корни глубоко в земле оставались крепкими и тёплыми — оно терпеливо ждало весны.\n\nИ вот снова пришла весна, и дерево вновь покрылось молодыми листьями. Оно поняло: у каждого времени года есть своя красота, а перемены — самая естественная и прекрасная часть жизни."
                }
            ],
            en: [
                {
                    title: "Magic Forest",
                    text: "Once upon a time, in a faraway land, there was a magic forest. In this forest, trees were made of chocolate and leaves were made of candy. The flowers smelled like strawberries, and a little stream flowed with lemonade.\n\nOne day, a little elephant was walking through the forest and found a golden key. He didn't know which door it belonged to. Suddenly, a butterfly flew down to him.\n\n'I know what this key opens,' said the butterfly. 'Come, I'll show you!' They walked together through the forest, past rivers of lemonade and trees of chocolate.\n\nIn the center of the forest stood a beautiful little house. There was a lock on the door. The elephant put in the key — and the door opened! Inside were books, toys, and lots of sweets.\n\n'This is the House of Knowledge,' said the butterfly. 'Kind and clever children live here.' The elephant sat down to read the books. Every day he learned something new.\n\nSoon he had many friends. They all read together, played, and helped each other. Because knowledge and friendship are the greatest treasures of the magic forest."
                },
                {
                    title: "Space Adventure",
                    text: "A boy named Leo loved looking at the stars every night. He would sit by his window and dream: 'One day I'll fly through space!'\n\nOne night, a tiny flying saucer landed on his windowsill. A little robot stepped out. 'Come on, Leo, let's go to the stars!' it said.\n\nLeo wasn't afraid. He climbed into the saucer and they flew up into the sky. The city below looked like a tiny toy. Soon they reached the Moon — white and round like a ball.\n\nNext, they flew to Saturn. Its rings shimmered with all the colors of the rainbow. 'Beautiful!' cried Leo. Then came Mars — a red planet covered in rocks and dust.\n\n'Our universe is very big,' said the robot. 'And the knowledge about it is endless. Study hard, Leo, and you will become a real astronaut one day.'\n\nIn the morning, Leo woke up in his own bed. But in his hand was a small red stone from Mars. From that day on, he studied harder than ever. And everyone said: that boy will surely fly to the stars!"
                },
                {
                    title: "The Wise Tortoise",
                    text: "By the shore of a beautiful lake lived a tortoise named Tilly. Tilly walked very slowly, but she knew a great many things about the world.\n\nOne day, a rabbit ran up to her and laughed. 'Hey, tortoise! You're so slow — you'll never get anywhere in life!' Tilly smiled calmly and said: 'We shall see.'\n\nThe next morning, they decided to have a race. All the animals came to watch. The race began. The rabbit shot off like a rocket, while Tilly walked slowly but steadily.\n\nHalfway through, the rabbit decided to rest under a shady tree. 'I'm so fast, I can easily catch up,' he thought, and fell asleep. But Tilly kept walking, one step at a time.\n\nAs the sun was setting, Tilly crossed the finish line first. The rabbit woke up and realized he had lost. He felt very ashamed.\n\nTilly said to him kindly: 'Dear friend, it's not speed that matters — it's persistence. Whoever never gives up will always reach their goal.' The rabbit remembered this lesson for the rest of his life."
                },
                {
                    title: "The Kind Little Bear",
                    text: "In a forest at the foot of the mountains lived a little bear cub named Buzzy. On the last days of autumn, he found the sweetest berries and filled his whole basket.\n\nOn his way home, he saw a tired little rabbit — winter was coming, and food in the forest was growing scarce. Buzzy thought for a moment, then held out half of his berries. 'Here, friend, let's share!' he said with a smile.\n\nWhile they were eating, a hungry little squirrel came along too. Buzzy shared with her as well. Soon the three of them were picking new berries together and sharing them equally.\n\nThat evening, Buzzy came home with an empty basket, but a very full heart — he had made two new friends. His mother said warmly, 'Sharing never makes you have less — it only makes you have more.'\n\nFrom that day on, Buzzy always loved sharing with others, because he knew: kindness is sweeter than even the sweetest berry."
                },
                {
                    title: "Ocean Secrets",
                    text: "On a coral reef lived a curious little fish named Lola. She loved discovering something new every single day.\n\nOne day, Lola swam off her usual path and into an unfamiliar cave. Inside, she got lost — everything around her was dark and strange. Lola became very frightened.\n\nJust then, she met a kind sea turtle named Karim. 'What's wrong, little one?' he asked. Lola, sobbing, told him she was lost. Karim decided to help her.\n\nTogether they swam toward the light, following the glowing sea plants. Along the way, an octopus named Rustam and a starfish named Zebo joined them — all helping Lola find her way home.\n\nWhen she finally reached home, Lola realized: it's much better to ask for help and trust your friends than to be scared all alone. From then on, she always swam together with her friends — because the ocean, and life, are more beautiful together."
                },
                {
                    title: "The Little Gardener",
                    text: "A girl named Nilufar received a tiny seed from her grandfather. 'Plant it and take care of it, and it will become a wonderful gift for you,' he said with a smile.\n\nNilufar planted the seed in the garden and watered it every day. But days passed, and nothing grew. Worried, she asked, 'Grandpa, why isn't anything growing?'\n\n'Be patient, my dear,' he said. 'Every beautiful thing takes time.' Nilufar kept caring for it — watering it, giving it sunlight, and clearing away weeds.\n\nAfter a few weeks, a little green sprout finally poked through the soil! Nilufar was overjoyed. She watched it grow every day until it became a beautiful red flower.\n\nThat day, Nilufar understood: with patience and care, even the smallest seed can become the most beautiful flower."
                },
                {
                    title: "The Little Bright Star",
                    text: "High in the sky lived a little star named Sparkle. Among all the big, bright stars, she felt completely unnoticed. 'I'm so small, no one will ever see my light,' she sighed every night.\n\nOne night, a little girl on Earth looked up at the sky, searching not for the brightest star, but for the one that looked closest and kindest. Her eyes landed on Sparkle.\n\n'You're shining so beautifully!' the girl whispered, closing her eyes to make a wish. Sparkle was amazed — she had always thought herself too small, yet someone had noticed and loved her light.\n\nFrom that night on, Sparkle understood: it doesn't matter if you're big or small — what matters is that your light can brighten someone's night.\n\nEver since, Sparkle shines proudly every night, knowing that even a small light can mean a great deal to someone."
                },
                {
                    title: "The Bridge of Friendship",
                    text: "On two sides of a great river lived two animal families — rabbits on one side, squirrels on the other. They could see each other from afar, but the river kept them apart.\n\nOne day, a little rabbit named Aziz was playing by the river and met a squirrel named Malika from the other side. They talked and both wished they could play together, but crossing the river seemed impossible.\n\n'Why don't we build a bridge?' Aziz suggested. Malika loved the idea. They gathered all the animals to work together — rabbits carried stones, squirrels tied branches tightly, and beavers helped build it with skill.\n\nAfter a few days, a strong, beautiful bridge was finished! Now families from both sides could cross freely and play together.\n\nAziz and Malika learned something important: when everyone works together, no obstacle — not even a great river — can stand in the way of friendship."
                },
                {
                    title: "The Brave Little Bird",
                    text: "High in a tree lived a little swallow named Sardor. His wings were already strong, but he was terrified of his very first flight. 'What if I fall?' he worried every day.\n\nHis mother gently encouraged him: 'Sardor, every bird is scared at first. But until you feel yourself flying, you won't know how beautiful it truly is.'\n\nOne day, a strong wind blew and Sardor slipped from the nest without meaning to. His heart raced — but suddenly he spread his wings and... he flew! Wobbly at first, then free and light.\n\nHe soared through the sky and saw the forest, the river, and his home below. It was amazing! Sardor chirped with joy.\n\nWhen he returned to the nest, he understood: fear is only the feeling before the first step, but a step taken with courage always opens a new world."
                },
                {
                    title: "Journey Through the Four Seasons",
                    text: "In spring, a tiny seed pushed up from the earth and became a young sprout. The sun warmed it with gentle rays. 'Hello, world!' the sprout said joyfully, unfurling its very first leaf.\n\nBy summer, the sprout had grown into a tall tree. Birds built nests in its branches, and children played in its shade. The days were warm and long, and the tree grew stronger every day.\n\nWhen autumn came, the tree's leaves turned yellow, red, and orange. When the wind blew, the leaves danced and slowly drifted to the ground. The tree watched this beautiful sight, proud of itself.\n\nIn winter, the tree wrapped itself in snow and rested quietly. Despite the cold winds, its roots stayed deep, strong, and warm — patiently waiting for spring.\n\nAnd then spring came again, and the tree grew fresh new leaves once more. It understood: every season has its own beauty, and change is the most natural and wonderful part of life."
                }
            ]
        };
        const list = stories[lang] || stories.en;
        let pool = list;
        if (list.length > 1 && this._lastAiStoryTitle) {
            const filtered = list.filter(s => s.title !== this._lastAiStoryTitle);
            if (filtered.length) pool = filtered;
        }
        const pick = pool[Math.floor(Math.random() * pool.length)];
        this._lastAiStoryTitle = pick.title;
        return pick;
    }
}

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

class BadgeManager {
    constructor() {
        this._badges = JSON.parse(localStorage.getItem(profileManager.key('badges')) || '[]');
        this._queue = [];
        this._busy = false;
    }

    isEarned(id) { return this._badges.includes(id); }

    awardBadge(id) {
        if (this.isEarned(id)) return;
        this._badges.push(id);
        localStorage.setItem(profileManager.key('badges'), JSON.stringify(this._badges));
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
        const n = parseInt(localStorage.getItem(profileManager.key('game-count')) || '0') + 1;
        localStorage.setItem(profileManager.key('game-count'), String(n));

        const cats = JSON.parse(localStorage.getItem(profileManager.key('cats-tried')) || '[]');
        if (catId && !cats.includes(catId)) {
            cats.push(catId);
            localStorage.setItem(profileManager.key('cats-tried'), JSON.stringify(cats));
        }

        if (n === 1)  this.awardBadge('game_first');
        if (n === 5)  this.awardBadge('game_5');
        if (n === 25) this.awardBadge('game_25');
        if (n === 50) this.awardBadge('game_50');
        if (cats.length >= 3) this.awardBadge('cat_3');
        if (cats.length >= 7) this.awardBadge('cat_all');
    }

    onStoryPlayed() {
        const n = parseInt(localStorage.getItem(profileManager.key('story-count')) || '0') + 1;
        localStorage.setItem(profileManager.key('story-count'), String(n));
        if (n === 1)  this.awardBadge('story_first');
        if (n === 10) this.awardBadge('story_10');
    }

    onSongPlayed() {
        const n = parseInt(localStorage.getItem(profileManager.key('song-count')) || '0') + 1;
        localStorage.setItem(profileManager.key('song-count'), String(n));
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
        const days = JSON.parse(localStorage.getItem(profileManager.key('streak-days')) || '[]');
        if (!days.includes(today)) {
            days.push(today);
            if (days.length > 30) days.shift();
            localStorage.setItem(profileManager.key('streak-days'), JSON.stringify(days));
        }
        const streak = this._streak(days);
        if (streak >= 3) this.awardBadge('streak_3');
        if (streak >= 7) this.awardBadge('streak_7');
    }

    _streak(days) {
        if (!days.length) return 0;
        const sorted = [...days].sort();
        let cur = 1;
        for (let i = sorted.length - 1; i > 0; i--) {
            const b = new Date(sorted[i].replace(/(\d{4})(\d{2})(\d{2})/,'$1-$2-$3'));
            const a = new Date(sorted[i-1].replace(/(\d{4})(\d{2})(\d{2})/,'$1-$2-$3'));
            if ((b - a) === 86400000) cur++;
            else break;
        }
        return cur;
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

// Initialize application
let app;
window.addEventListener("load", () => {
    window.profileManager = new ProfileManager();
    // The native app passes the user's chosen language/age as URL query params
    // (e.g. index.html?lang=uz&age=2-4) so they're available synchronously here,
    // before any localStorage read. Seed the active profile's keys from them so
    // TranslationManager (constructed right below) picks up the right language
    // instead of always defaulting to "en".
    const urlParams = new URLSearchParams(location.search);
    const urlLang = urlParams.get('lang');
    if (urlLang) localStorage.setItem(profileManager.key('lang'), urlLang);
    const urlAge = urlParams.get('age');
    if (urlAge) localStorage.setItem(profileManager.key('age'), urlAge);
    const translator = new TranslationManager(T);
    const ui = new UIManager();
    app = new GameManager(GAMES, ui, translator);

    const audioPlayer = new AudioPlayer();
    app.storyManager = new StoryManager(audioPlayer, translator, ui);
    app.audioPlayer  = audioPlayer;

    // Initial state
    app.updateProgress();
    window.badgeManager = new BadgeManager();
    badgeManager.checkStreak();
    badgeManager.onPointsUpdated(app.pts);
    if (window.updateLangUI) window.updateLangUI();
    ui.updateMusicUI(app.isMuted);

    if (app.isMuted && window.AndroidBridge?.toggleMusic) {
        window.AndroidBridge.toggleMusic(true);
    }

    // Load content and render
    app.songManager = new SongManager(audioPlayer, translator, ui);
    app.songManager.load().then(() => app.songManager.render());

    app.storyManager.load().then(() => app.storyManager.render());

    // Win-bonus callback used by some mini-games (e.g. puzzle-slider, game-engine.html)
    // via window.parent.onRewardGranted(amount) — not tied to any ad.
    window.onRewardGranted = function(amount) {
        app.addPoints(amount || 50);
        app.ui.showToast('🎁 +' + (amount || 50) + ' ⭐');
    };

    // AndroidBridge.generateStory(...) callbacks (see generateAiStory in GameManager) —
    // error falls back to the offline story pool so a network hiccup never dead-ends the UI.
    window.onAiStoryReady = function(payloadJson) {
        app._aiStoryPending = false;
        try {
            app._showAiStory(JSON.parse(payloadJson));
        } catch (e) {
            app._showAiStory(app.getAiStory(app.translator.lang));
        }
    };
    window.onAiStoryError = function() {
        app._aiStoryPending = false;
        app._showAiStory(app.getAiStory(app.translator.lang));
    };

    // Restore last active tab
    const validTabs = ['games', 'stories', 'songs'];
    const rawTab = localStorage.getItem('kz-tab') || 'games';
    const savedTab = validTabs.includes(rawTab) ? rawTab : 'games';
    ui.switchTab(savedTab);

    setTimeout(() => {
        const loader = document.getElementById("loader");
        if (loader) loader.classList.add("h");
    }, 1400);
    if (window.profileManager && profileManager.getAll().length > 1) openProfilePicker();
});
