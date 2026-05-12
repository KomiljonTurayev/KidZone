/**
 * KidZone Main Application Logic
 * Refactored using SOLID principles.
 */

class TranslationManager {
    constructor(translations) {
        this.translations = translations;
        this.lang = localStorage.getItem("kz-lang") || "en";
    }

    setLanguage(lang) {
        this.lang = lang;
        localStorage.setItem("kz-lang", lang);
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
        const title = item.title[lang] || item.title.en;

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
        const playBtn = document.getElementById('kzp-play');
        if (playBtn) playBtn.textContent = '⏸';
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
}

class SongManager extends ContentManager {
    constructor(player, translator, ui) {
        super('songs', player, translator, ui);
    }
}

class GameManager {
    constructor(games, ui, translator) {
        this.games = games;
        this.ui = ui;
        this.translator = translator;
        this.pts = parseInt(localStorage.getItem("kz-pts") || "0");
        this.age = localStorage.getItem("kz-age") || "2-4";
        this.isMuted = localStorage.getItem("kz-muted") === "true";
        this.cat = "all";
        this.pinEntry = "";
    }

    addPoints(n) {
        this.pts += n;
        localStorage.setItem("kz-pts", this.pts);
        this.updateProgress();
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
        if (window.AndroidAdMob && window.AndroidAdMob.updateLanguage) {
            window.AndroidAdMob.updateLanguage(l);
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
        localStorage.setItem("kz-age", a);
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
        if (window.AndroidAdMob?.toggleMusic) {
            window.AndroidAdMob.toggleMusic(this.isMuted);
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

        if (window.AndroidAdMob) {
            window.AndroidAdMob.showInterstitial();
            window.AndroidAdMob.hideBanner();
        }
    }

    closeGame() {
        const gvEl = document.getElementById("gv");
        const frameEl = document.getElementById("gv-frame");
        if (gvEl) gvEl.classList.add("h");
        if (frameEl) frameEl.src = "";
        if (window.AndroidAdMob) window.AndroidAdMob.showBanner();

        const titleEl = document.getElementById("gv-title");
        const titleText = titleEl ? titleEl.textContent : "";
        const game = this.games.find(g => titleText.includes(g.em));
        if (game) {
            const earned = game.pts || 20;
            this.addPoints(earned);
            this.showRewardScreen(earned);
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
        const viewer = document.getElementById("ai-viewer");
        const loading = document.getElementById("aiv-loading");
        const content = document.getElementById("aiv-content-wrap");

        viewer.classList.remove("h");
        loading.classList.remove("h");
        content.classList.add("h");

        // Simulate AI thinking
        setTimeout(() => {
            const story = this.getAiStory(this.translator.lang);
            document.getElementById("aiv-story-title").textContent = story.title;
            document.getElementById("aiv-content").textContent = story.text;

            loading.classList.add("h");
            content.classList.remove("h");
            this.addPoints(10);
        }, 2000);
    }

    closeAi() {
        document.getElementById("ai-viewer").classList.add("h");
        window.speechSynthesis?.cancel();
    }

    speakStory() {
        const text = document.getElementById("aiv-content").textContent;
        const btn = document.getElementById("ai-read-btn");

        if (window.speechSynthesis.speaking) {
            window.speechSynthesis.cancel();
            btn.querySelector('span').textContent = "🔊";
            return;
        }

        const msg = new SpeechSynthesisUtterance(text);
        const langMap = { uz: "uz-UZ", ru: "ru-RU", en: "en-US" };
        msg.lang = langMap[this.translator.lang] || "en-US";
        msg.onend = () => btn.querySelector('span').textContent = "🔊";

        window.speechSynthesis.speak(msg);
        btn.querySelector('span').textContent = "⏹️";
    }

    openAiMusic() {
        this.ui.showToast(this.translator.get("aiMusicToast"));
        // For simplicity, we toggle music or open a specific music app game
        setTimeout(() => {
            this.openGame({id:"piano", em:"🎹", name:this.translator.get("aiBtnMusic"), file:"instrument.html", pts:15});
        }, 1000);
    }

    getAiStory(lang) {
        const stories = {
            uz: [
                { title: "Sehrli O'rmon", text: "Bir bor ekan, bir yo'q ekan, uzoq bir o'lkada sehrli o'rmon bor ekan. Bu o'rmonda daraxtlar shokoladdan, barglar esa shirinliklardan iborat ekan. Bir kuni kichkina filcha o'rmonda sayr qilib yurib, oltin kalit topib olibdi..." },
                { title: "Koinot sayohati", text: "Jasur ismli bolakay har kecha yulduzlarga qarab uxlashni yaxshi ko'rardi. Bir kuni uning derazasiga kichkina uchuvchi tarelka qo'ndi. Undan chiqqan mitti robot: 'Qani Jasur, ketdik yulduzlar sari!' dedi..." }
            ],
            ru: [
                { title: "Волшебный Лес", text: "Жил-был в далекой стране волшебный лес. В этом лесу деревья были из шоколада, а листья — из конфет. Однажды маленький слоненок гулял по лесу и нашел золотой ключ..." },
                { title: "Космическое приключение", text: "Мальчик по имени Максим любил смотреть на звезды. Однажды к его окну приземлилась маленькая летающая тарелка. Робот внутри сказал: 'Пойдем, Максим, к звездам!'..." }
            ],
            en: [
                { title: "Magic Forest", text: "Once upon a time, in a faraway land, there was a magic forest. In this forest, trees were made of chocolate and leaves were made of candy. One day, a little elephant was walking and found a golden key..." },
                { title: "Space Adventure", text: "A boy named Leo loved looking at the stars. One night, a small flying saucer landed on his window. A tiny robot said: 'Come on Leo, let's go to the stars!'..." }
            ]
        };
        const list = stories[lang] || stories.en;
        return list[Math.floor(Math.random() * list.length)];
    }
}

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

    // Restore last active tab
    const savedTab = localStorage.getItem('kz-tab') || 'games';
    ui.switchTab(savedTab);

    setTimeout(() => {
        const loader = document.getElementById("loader");
        if (loader) loader.classList.add("h");
    }, 1400);
});
