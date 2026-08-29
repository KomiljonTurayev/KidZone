/**
 * KidZone Shared UI Library v2.0
 * Provides: 20-Level system, How-to-Play overlay, Hint, Lives, Timer
 */
(function () {
'use strict';

/* ══ 20 LEVEL CONFIGS ══════════════════════════════════════════════════
   q = questions per round
   c = number of choices shown
   hints = hint uses available
   timer = seconds per question (0 = no timer)
   pool = fraction of full item pool to use (0..1)
════════════════════════════════════════════════════════════════════════ */
const CONFIGS = [
  {n:1,  q:5,  c:2, hints:3, timer:0,  pool:0.20, stars:"⭐"},
  {n:2,  q:5,  c:2, hints:3, timer:0,  pool:0.22, stars:"⭐"},
  {n:3,  q:6,  c:3, hints:3, timer:0,  pool:0.28, stars:"⭐⭐"},
  {n:4,  q:6,  c:3, hints:2, timer:0,  pool:0.33, stars:"⭐⭐"},
  {n:5,  q:7,  c:3, hints:2, timer:60, pool:0.38, stars:"⭐⭐"},
  {n:6,  q:7,  c:4, hints:2, timer:55, pool:0.45, stars:"⭐⭐⭐"},
  {n:7,  q:8,  c:4, hints:2, timer:50, pool:0.50, stars:"⭐⭐⭐"},
  {n:8,  q:8,  c:4, hints:1, timer:45, pool:0.55, stars:"⭐⭐⭐"},
  {n:9,  q:9,  c:4, hints:1, timer:40, pool:0.62, stars:"🌟🌟"},
  {n:10, q:10, c:4, hints:1, timer:35, pool:0.67, stars:"🌟🌟"},
  {n:11, q:10, c:4, hints:1, timer:30, pool:0.72, stars:"🌟🌟🌟"},
  {n:12, q:11, c:4, hints:1, timer:28, pool:0.77, stars:"🌟🌟🌟"},
  {n:13, q:11, c:4, hints:0, timer:25, pool:0.82, stars:"🏅"},
  {n:14, q:12, c:4, hints:0, timer:22, pool:0.87, stars:"🏅"},
  {n:15, q:12, c:4, hints:0, timer:20, pool:0.90, stars:"🏅🏅"},
  {n:16, q:13, c:4, hints:0, timer:18, pool:0.93, stars:"🥇"},
  {n:17, q:13, c:4, hints:0, timer:15, pool:0.95, stars:"🥇"},
  {n:18, q:14, c:4, hints:0, timer:12, pool:0.97, stars:"👑"},
  {n:19, q:15, c:4, hints:0, timer:10, pool:1.00, stars:"👑"},
  {n:20, q:15, c:4, hints:0, timer:8,  pool:1.00, stars:"🧠"},
];

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
.kzl-close-btn{width:100%;padding:14px;border:none;border-radius:20px;background:#FF6B35;color:#fff;font-size:15px;font-weight:900;cursor:pointer;margin-top:10px;font-family:'Arial Rounded MT Bold',Arial,sans-serif;touch-action:manipulation;}
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
.kzl-lup-btn{background:#FF6B35;color:#fff;border:none;border-radius:20px;padding:14px 34px;font-size:16px;font-weight:900;cursor:pointer;font-family:'Arial Rounded MT Bold',Arial,sans-serif;box-shadow:0 4px 0 #C94F20;transform:translateY(0);transition:transform .08s,box-shadow .08s;}
.kzl-lup-btn:active{transform:translateY(4px);box-shadow:0 1px 0 #C94F20;}
`;

/* ══ PUBLIC API ════════════════════════════════════════════════════════ */
window.KZL = {
  CONFIGS,

  /* ─── Level persistence ─── */
  getLevel(id) {
    return Math.min(20, Math.max(1, parseInt(localStorage.getItem('kzl-' + id) || '1')));
  },
  setLevel(id, n) {
    localStorage.setItem('kzl-' + id, Math.min(20, Math.max(1, n)));
  },
  nextLevel(id) {
    const n = this.getLevel(id);
    const next = Math.min(20, n + 1);
    this.setLevel(id, next);
    return next;
  },
  cfg(id) {
    return CONFIGS[this.getLevel(id) - 1];
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
    floatBtn.setAttribute('aria-label', 'How to play');
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
    const cfg = CONFIGS[Math.min(19, newLevel - 1)];

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
    const cfg = CONFIGS[Math.min(19, level - 1)];
    const count = Math.max(cfg.c + 1, Math.ceil(arr.length * cfg.pool));
    return arr.slice(0, Math.min(arr.length, count));
  },
};

})();
