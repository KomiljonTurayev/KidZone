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
        if (subEl) subEl.textContent = `${pts} / ${lv.next} ball`;
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
        document.querySelectorAll(".ni").forEach(n => n.classList.remove("on"));
        const niEl = document.getElementById("ni-" + pg);
        if (niEl) niEl.classList.add("on");
        const msgs = {
            play: "🎮 " + this.translator.get('playMode'),
            learn: "📚 " + this.translator.get('learnMode'),
            stars: "⭐ " + this.translator.get('yourStars') + ": " + this.pts,
            home: ""
        };
        if (msgs[pg]) this.ui.showToast(msgs[pg]);
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
}

// Initialize application
let app;
window.addEventListener("load", () => {
    const translator = new TranslationManager(T);
    const ui = new UIManager();
    app = new GameManager(GAMES, ui, translator);

    // Initial state
    app.updateProgress();
    if (window.updateLangUI) window.updateLangUI();
    ui.updateMusicUI(app.isMuted);

    if (app.isMuted && window.AndroidAdMob?.toggleMusic) {
        window.AndroidAdMob.toggleMusic(true);
    }

    setTimeout(() => {
        const loader = document.getElementById("loader");
        if (loader) loader.classList.add("h");
    }, 1400);
});
