/**
 * KidZone Shared UI Library v3.0
 * Provides: endless age-aware levels, How-to-Play overlay, Hint, Lives, Timer
 */
(function () {
'use strict';

/* ══ ENDLESS LEVEL CONFIG ══════════════════════════════════════════════
   q = questions per round
   c = number of choices shown
   hints = hint uses available
   timer = seconds per question (0 = no timer)
   pool = fraction of full item pool to use (0..1)
   tier = floor((n-1)/10); games may use it for extra variety past level 20
   Age bands: "2-4" never gets a timer, "5-7" never below 15 s, "8+" down to 8 s.
   ease = the child failed this level twice in a row: softer round, same level.
════════════════════════════════════════════════════════════════════════ */
const AGE_BAND = {'2-4': 0, '5-7': 1, '8+': 2};
const TIER_STARS = ["⭐", "⭐⭐", "🌟", "🌟🌟", "🏅", "🥇", "👑", "🧠", "💎", "🚀"];

function cfgFor(level, age, ease) {
  const n = Math.max(1, Math.floor(level) || 1);
  const band = AGE_BAND[age] !== undefined ? AGE_BAND[age] : 1;
  const q = Math.min(15, 5 + Math.floor((n - 1) / 2));
  let c = n < 3 ? 2 : n < 6 ? 3 : 4;
  if (band === 2 && n >= 25) c = n >= 40 ? 6 : 5;
  const pool = Math.min(1, 0.2 + 0.04 * (n - 1));
  let hints = n < 4 ? 3 : n < 9 ? 2 : n < 13 ? 1 : 0;
  if (band === 0) hints = Math.max(1, hints);
  let timer = 0;
  if (band === 1 && n >= 8) timer = Math.max(15, 40 - (n - 8));
  if (band === 2 && n >= 5) timer = Math.max(8, 60 - 3 * (n - 5));
  if (ease) {
    timer = timer ? Math.round(timer * 1.5) : 0;
    c = Math.max(2, c - 1);
    hints += 1;
  }
  const tier = Math.floor((n - 1) / 10);
  const stars = TIER_STARS[Math.min(TIER_STARS.length - 1, tier)];
  return {n, q, c, hints, timer, pool, stars, tier, ease: !!ease};
}

/* ══ PROFILE + AGE ══════════════════════════════════════════════════════
   The native app tells index.html which child is playing (?profile=…, then
   kz-native-profile); games run same-origin in an iframe and read it here. */
function profileId() {
  return localStorage.getItem('kz-native-profile')
      || localStorage.getItem('kz-active-profile') || 'p1';
}
function currentAge() {
  const q = new URLSearchParams(location.search).get('age');
  if (q && AGE_BAND[q] !== undefined) return q;
  const a = localStorage.getItem('kz-' + (localStorage.getItem('kz-active-profile') || 'p1') + '-age');
  return AGE_BAND[a] !== undefined ? a : '5-7';
}
function key(kind, id) { return 'kzl-' + kind + '-' + profileId() + '-' + id; }
function readInt(k) { const v = parseInt(localStorage.getItem(k)); return v > 0 ? v : 0; }

/* Levels used to live in kzl-<gameId>, shared by every child on the phone.
   The first child to open the game after the update inherits them. */
function migrate(id) {
  const old = 'kzl-' + id;
  const lv = readInt(old);
  if (!lv) return;
  if (!readInt(key('lv', id))) {
    localStorage.setItem(key('lv', id), lv);
    localStorage.setItem(key('best', id), lv);
  }
  localStorage.removeItem(old);
}

/* ══ SHARED CSS ═══════════════════════════════════════════════════════ */
const CSS = `
.kzl-htp-btn{position:fixed;bottom:82px;right:14px;z-index:900;width:46px;height:46px;border-radius:50%;background:rgba(255,255,255,0.92);border:3px solid #FF6B35;font-size:20px;cursor:pointer;box-shadow:0 4px 16px rgba(0,0,0,.22);display:flex;align-items:center;justify-content:center;animation:kzlPulse 2.4s ease-in-out infinite;touch-action:manipulation;}
@keyframes kzlPulse{0%,100%{transform:scale(1)}50%{transform:scale(1.09)}}
.kzl-overlay{position:fixed;inset:0;z-index:1000;background:rgba(0,0,0,.68);backdrop-filter:blur(5px);display:flex;align-items:center;justify-content:center;padding:16px;}
.kzl-overlay.h{display:none!important;}
.kzl-sheet{background:#fff;border-radius:28px;width:min(390px,96vw);padding:26px 22px 22px;box-shadow:0 20px 60px rgba(0,0,0,.3);animation:kzlPop .34s cubic-bezier(.34,1.56,.64,1);max-height:88vh;overflow-y:auto;}
@keyframes kzlPop{from{transform:scale(.65);opacity:0}to{transform:scale(1);opacity:1}}
.kzl-sheet-title{font-size:21px;font-weight:900;text-align:center;color:#FF6B35;margin-bottom:16px;}
.kzl-step{display:flex;align-items:flex-start;gap:11px;margin-bottom:13px;}
.kzl-step-icon{font-size:26px;flex-shrink:0;width:36px;text-align:center;}
.kzl-step-text{font-size:13.5px;color:#333;line-height:1.5;font-weight:700;}
.kzl-close-btn{width:100%;padding:14px;border:none;border-radius:20px;background:#FF6B35;color:#fff;font-size:15px;font-weight:900;cursor:pointer;margin-top:10px;font-family:'Nunito','KZ Cyrillic',sans-serif;touch-action:manipulation;}
.kzl-close-btn:active{transform:scale(.96);}
.kzl-lvl-row{display:flex;align-items:center;gap:8px;justify-content:center;margin-bottom:4px;}
.kzl-lvl-badge{display:inline-flex;align-items:center;gap:5px;background:rgba(255,255,255,.22);border:2px solid rgba(255,255,255,.4);border-radius:20px;padding:5px 13px;font-size:13px;font-weight:900;color:#fff;}
.kzl-hint-btn{background:rgba(255,220,50,.92);border:none;border-radius:20px;padding:8px 13px;font-size:12.5px;font-weight:900;cursor:pointer;color:#555;display:inline-flex;align-items:center;gap:4px;box-shadow:0 3px 10px rgba(0,0,0,.14);touch-action:manipulation;}
.kzl-hint-btn:disabled{opacity:.38;pointer-events:none;}
.kzl-hint-btn:active{transform:scale(.92);}
.kzl-lives{display:flex;gap:3px;align-items:center;}
.kzl-heart{font-size:21px;transition:transform .18s;}
.kzl-heart.lost{opacity:.22;transform:scale(.78);}
.kzl-timer-ring{display:flex;align-items:center;justify-content:center;}
.kzl-timer-svg{transform:rotate(-90deg);}
.kzl-timer-bg{fill:none;stroke:rgba(255,255,255,.2);stroke-width:4;}
.kzl-timer-fg{fill:none;stroke:#FFD600;stroke-width:4;stroke-linecap:round;transition:stroke-dashoffset .9s linear;}
.kzl-timer-txt{position:absolute;font-size:13px;font-weight:900;color:#FFD600;}
.kzl-level-up{position:fixed;inset:0;z-index:1001;background:rgba(0,0,0,.62);display:flex;align-items:center;justify-content:center;}
.kzl-level-up.h{display:none!important;}
.kzl-lup-card{background:#FFFFFF;border-radius:32px;padding:36px 30px;text-align:center;color:#2D2D2D;width:min(300px,90vw);animation:kzlPop .4s cubic-bezier(.34,1.56,.64,1);box-shadow:0 8px 32px rgba(0,0,0,0.14);}
.kzl-lup-stars{font-size:44px;margin-bottom:4px;}
.kzl-lup-num{font-size:72px;font-weight:900;line-height:1;}
.kzl-lup-txt{font-size:18px;font-weight:800;margin:8px 0 22px;}
.kzl-lup-btn{background:#FF6B35;color:#fff;border:none;border-radius:20px;padding:14px 34px;font-size:16px;font-weight:900;cursor:pointer;font-family:'Nunito','KZ Cyrillic',sans-serif;box-shadow:0 4px 0 #C94F20;transform:translateY(0);transition:transform .08s,box-shadow .08s;}
.kzl-lup-btn:active{transform:translateY(4px);box-shadow:0 1px 0 #C94F20;}
`;

/* ══ PUBLIC API ════════════════════════════════════════════════════════ */
window.KZL = {
  cfgFor,
  /** Most choices a game's layout can show; a game that handles more raises it. */
  maxChoices: 4,

  /* ─── Level persistence (per child, no upper limit) ─── */
  /** The level being played. Replaying an old level lowers it; [getBest] keeps the record. */
  getLevel(id) {
    migrate(id);
    return Math.max(1, readInt(key('lv', id)));
  },
  /** Highest level this child has unlocked in the game. */
  getBest(id) {
    migrate(id);
    return Math.max(1, readInt(key('best', id)), readInt(key('lv', id)));
  },
  setLevel(id, n) {
    const lv = Math.max(1, Math.floor(n) || 1);
    localStorage.setItem(key('lv', id), lv);
    if (lv > readInt(key('best', id))) {
      localStorage.setItem(key('best', id), lv);
      const bridge = window.AndroidBridge || (window.parent && window.parent.AndroidBridge);
      if (bridge && bridge.levelReached) bridge.levelReached(id, lv);
    }
  },
  nextLevel(id) {
    const next = this.getLevel(id) + 1;
    localStorage.removeItem(key('fails', id));
    this.setLevel(id, next);
    return next;
  },
  /** Games report every finished round; two misses in a row soften the level. */
  recordResult(id, passed) {
    const k = key('fails', id);
    if (passed) localStorage.removeItem(k);
    else localStorage.setItem(k, readInt(k) + 1);
  },
  cfg(id) {
    const c = cfgFor(this.getLevel(id), currentAge(), readInt(key('fails', id)) >= 2);
    c.c = Math.min(c.c, this.maxChoices);
    return c;
  },
  /** Level numbers for a picker: everything unlocked so far (latest 11) plus the next locked one. */
  levelWindow(id) {
    const best = this.getBest(id);
    const from = Math.max(1, best - 10);
    const out = [];
    for (let n = from; n <= best + 1; n++) {
      out.push({n, stars: cfgFor(n, currentAge()).stars, locked: n > best});
    }
    return out;
  },

  /* ─── Style injection (once) ─── */
  _css: false,
  _inject() {
    if (this._css) return;
    this._css = true;
    const s = document.createElement('style');
    s.textContent = CSS;
    document.head.appendChild(s);
  },

  /* ─── How-to-Play overlay ─── */
  howToPlay(cfg) {
    /*
      cfg = {
        id: 'animals',               // game id (used for "seen" flag)
        steps: {
          uz: [{icon:'👆', text:'...'}],
          ru: [...], en: [...]
        },
        title?: { uz, ru, en }       // optional custom title
      }
    */
    this._inject();
    const lang = new URLSearchParams(location.search).get('lang')
               || localStorage.getItem('kz-lang') || 'en';
    // Support both {uz:[...],ru:[...],en:[...]} object and a pre-resolved array
    let rawSteps = Array.isArray(cfg.steps) ? cfg.steps : (cfg.steps[lang] || cfg.steps.en || []);
    // Normalise: plain strings → {icon, text}
    const steps = rawSteps.map(s => (typeof s === 'string' ? {icon:'📌', text:s} : s));
    const DEF_TITLE = {uz:"Qanday o'ynash? 🎮", ru:"Как играть? 🎮", en:"How to Play? 🎮"};
    const BTN      = {uz:"Tushunarli! ✅", ru:"Понятно! ✅", en:"Got it! ✅"};
    // Support both plain string title and {uz,ru,en} object
    const title = typeof cfg.title === 'string' ? cfg.title
               : (cfg.title && (cfg.title[lang] || cfg.title.en)) || DEF_TITLE[lang] || DEF_TITLE.en;

    const overlay = document.createElement('div');
    overlay.className = 'kzl-overlay h';
    overlay.id = 'kzl-htp';
    overlay.innerHTML =
      `<div class="kzl-sheet">
        <div class="kzl-sheet-title">${title}</div>
        ${steps.map(s =>
          `<div class="kzl-step">
            <div class="kzl-step-icon">${s.icon}</div>
            <div class="kzl-step-text">${s.text}</div>
          </div>`).join('')}
        <button class="kzl-close-btn"
          onclick="document.getElementById('kzl-htp').classList.add('h')">
          ${BTN[lang] || BTN.en}
        </button>
      </div>`;
    document.body.appendChild(overlay);

    const floatBtn = document.createElement('button');
    floatBtn.className = 'kzl-htp-btn';
    floatBtn.textContent = '❓';
    floatBtn.setAttribute('aria-label', (DEF_TITLE[lang] || DEF_TITLE.en).replace(' 🎮', ''));
    floatBtn.onclick = () => overlay.classList.remove('h');
    document.body.appendChild(floatBtn);

    const key = 'kzl-seen-' + (cfg.id || 'game');
    if (!localStorage.getItem(key)) {
      localStorage.setItem(key, '1');
      setTimeout(() => overlay.classList.remove('h'), 700);
    }

    return {
      show: () => overlay.classList.remove('h'),
      hide: () => overlay.classList.add('h'),
    };
  },

  /* ─── Level-Up popup ─── */
  showLevelUp(id, newLevel, lang, onContinue) {
    this._inject();
    const l = lang || 'en';
    const SUB = {uz:"Tabriklaymiz! Yangi daraja 🎉", ru:"Поздравляем! Новый уровень 🎉", en:"Congratulations! New level 🎉"};
    const BTN = {uz:"Davom etish →", ru:"Продолжить →", en:"Continue →"};
    const cfg = cfgFor(newLevel, currentAge());

    let el = document.getElementById('kzl-lup');
    if (!el) {
      el = document.createElement('div');
      el.className = 'kzl-level-up h';
      el.id = 'kzl-lup';
      el.innerHTML =
        `<div class="kzl-lup-card">
          <div class="kzl-lup-stars" id="kzl-lup-s"></div>
          <div class="kzl-lup-num"   id="kzl-lup-n"></div>
          <div class="kzl-lup-txt"   id="kzl-lup-t"></div>
          <button class="kzl-lup-btn" id="kzl-lup-b"></button>
        </div>`;
      document.body.appendChild(el);
    }
    document.getElementById('kzl-lup-s').textContent = cfg ? cfg.stars : '🌟';
    document.getElementById('kzl-lup-n').textContent = newLevel;
    document.getElementById('kzl-lup-t').textContent = SUB[l] || SUB.en;
    const b = document.getElementById('kzl-lup-b');
    b.textContent = BTN[l] || BTN.en;
    b.onclick = () => { el.classList.add('h'); if (onContinue) onContinue(); };
    el.classList.remove('h');
  },

  /* ─── Hint system ─── */
  createHints(count) {
    this._inject();
    let rem = count;
    const btn = document.createElement('button');
    btn.className = 'kzl-hint-btn';
    btn.id = 'kzl-hint';
    btn.innerHTML = `💡 <span id="kzl-hint-c">${rem}</span>`;
    if (rem <= 0) btn.disabled = true;

    return {
      el: btn,
      use(cb) {
        if (rem <= 0) return;
        rem = Math.max(0, rem - 1);
        const el = document.getElementById('kzl-hint-c');
        if (el) el.textContent = rem;
        btn.disabled = rem <= 0;
        if (cb) cb();
      },
      reset(n) {
        rem = n;
        const el = document.getElementById('kzl-hint-c');
        if (el) el.textContent = n;
        btn.disabled = n <= 0;
      },
      get remaining() { return rem; }
    };
  },

  /* ─── Lives (hearts) system ─── */
  createLives(total) {
    this._inject();
    let rem = total;
    const wrap = document.createElement('div');
    wrap.className = 'kzl-lives';
    wrap.id = 'kzl-lives';

    const render = () => {
      wrap.innerHTML = '';
      for (let i = 0; i < total; i++) {
        const h = document.createElement('span');
        h.className = 'kzl-heart' + (i >= rem ? ' lost' : '');
        h.textContent = '❤️';
        wrap.appendChild(h);
      }
    };
    render();

    return {
      el: wrap,
      lose() { rem = Math.max(0, rem - 1); render(); return rem === 0; },
      reset() { rem = total; render(); },
      get left() { return rem; }
    };
  },

  /* ─── Countdown timer ─── */
  createTimer(seconds, onTick, onExpire) {
    this._inject();
    const R = 18, C = 2 * Math.PI * R;
    const wrap = document.createElement('div');
    wrap.style.cssText = 'position:relative;display:inline-flex;align-items:center;justify-content:center;';
    wrap.innerHTML =
      `<svg class="kzl-timer-svg" width="44" height="44" viewBox="0 0 44 44">
        <circle class="kzl-timer-bg" cx="22" cy="22" r="${R}"/>
        <circle class="kzl-timer-fg" cx="22" cy="22" r="${R}"
          style="stroke-dasharray:${C};stroke-dashoffset:0;" id="kzl-tfg"/>
      </svg>
      <span class="kzl-timer-txt" id="kzl-ttxt">${seconds}</span>`;

    let remaining = seconds;
    let interval = null;

    const update = () => {
      const fg = document.getElementById('kzl-tfg');
      const tx = document.getElementById('kzl-ttxt');
      if (fg) fg.style.strokeDashoffset = C * (1 - remaining / seconds);
      if (tx) tx.textContent = remaining;
      if (onTick) onTick(remaining);
    };

    return {
      el: wrap,
      start() {
        remaining = seconds;
        update();
        interval = setInterval(() => {
          remaining--;
          update();
          if (remaining <= 0) {
            clearInterval(interval);
            if (onExpire) onExpire();
          }
        }, 1000);
      },
      stop() { clearInterval(interval); },
      reset() {
        clearInterval(interval);
        remaining = seconds;
        update();
      },
      get left() { return remaining; }
    };
  },

  /* ─── Pool slicer: returns first `pct` fraction of array ─── */
  slicePool(arr, level) {
    const cfg = cfgFor(level, currentAge());
    const count = Math.max(cfg.c + 1, Math.ceil(arr.length * cfg.pool));
    return arr.slice(0, Math.min(arr.length, count));
  },
};

})();
