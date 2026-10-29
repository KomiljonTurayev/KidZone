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
function langOf() {
  return new URLSearchParams(location.search).get('lang') || localStorage.getItem('kz-lang') || 'en';
}

/* ══ STICKERS ══════════════════════════════════════════════════════════
   Every 5th level gives a sticker, every 10th also +100 ⭐, every 50th a gold one.
   The album (index.html, 🏅) reads kzl-stickers-<profile>. */
const STICKERS = "🦁🐯🐼🐨🐸🐵🦊🐰🐻🐷🐮🦄🐲🐙🦋🐞🐢🐬🐳🦖🦕🐧🦉🦜🌈⭐🌙☀️🌸🌻🍀🍎🍓🍉🍩🍦🧁🚀🛸🚂🚁⛵🎈🎨🎸🥁🎺🏰🗿🌋🏝️💎👑🎁🧸🪁🪀🔮🧩".match(/\p{Extended_Pictographic}️?/gu);
const GOLD = "🏆🥇👑💎🌟";
function stickerFor(id, level) {
  if (level % 50 === 0) return {e: [...GOLD][(level / 50 - 1) % [...GOLD].length], gold: true};
  let h = level;
  for (const ch of id) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return {e: STICKERS[h % STICKERS.length], gold: false};
}
function stickerKey() { return 'kzl-stickers-' + profileId(); }
function readStickers() {
  try { return JSON.parse(localStorage.getItem(stickerKey()) || '[]'); } catch (e) { return []; }
}
const L = {
  sticker: {uz: "Yangi stiker!", ru: "Новая наклейка!", en: "New sticker!"},
  bonus:   {uz: "Katta bayram! +100 ⭐", ru: "Большой праздник! +100 ⭐", en: "Big party! +100 ⭐"},
  map:     {uz: "Darajalar xaritasi", ru: "Карта уровней", en: "Level map"},
  next:    {uz: "Keyingi sovg'a", ru: "Следующий подарок", en: "Next gift"},
  level:   {uz: "daraja", ru: "уровень", en: "level"},
  ok:      {uz: "Zo'r! ✨", ru: "Ура! ✨", en: "Yay! ✨"},
};
function tr(o) { const l = langOf(); return o[l] || o.en; }
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
.kzl-map-btn{bottom:136px;}
.kzl-duo-btn{bottom:190px;font-size:18px;}
.kzl-duo-btn.on{background:#FFD600;}
.kzl-duo-banner{position:fixed;top:8px;left:50%;transform:translateX(-50%);z-index:950;display:flex;gap:12px;align-items:center;background:#fff;border:3px solid #2ECC71;border-radius:22px;padding:7px 14px;font-size:14px;font-weight:900;color:#2D2D2D;box-shadow:0 6px 18px rgba(0,0,0,.18);white-space:nowrap;font-family:'Nunito','KZ Cyrillic',sans-serif;animation:kzlPop .35s cubic-bezier(.34,1.56,.64,1);}
.kzl-duo-banner.parent{border-color:#7B1FA2;}
.kzl-duo-banner span{color:#888;font-size:13px;}
.kzl-duo-score{font-size:44px;font-weight:900;margin-bottom:6px;}
.kzl-duo-score span{color:#bbb;}
.kzl-duo-solo{display:block;margin:12px auto 0;background:none;border:none;color:#888;font-size:14px;font-weight:800;cursor:pointer;font-family:'Nunito','KZ Cyrillic',sans-serif;}
.kzl-stk-big{display:inline-block;font-size:64px;line-height:1.1;animation:kzlPop .5s cubic-bezier(.34,1.56,.64,1);}
.kzl-stk-big.gold{filter:drop-shadow(0 0 12px #FFD600);}
.kzl-stk-row{display:flex;align-items:center;justify-content:center;gap:10px;font-size:15px;font-weight:900;color:#7B1FA2;margin:-6px 0 14px;}
.kzl-stk-row .kzl-stk-big{font-size:44px;}
.kzl-stk-bonus{font-size:15px;font-weight:900;color:#FF6B35;margin:-6px 0 16px;}
.kzl-burst{position:fixed;top:-40px;z-index:1100;font-size:28px;pointer-events:none;animation:kzlFall 1.6s ease-in forwards;}
@keyframes kzlFall{to{transform:translateY(110vh) rotate(540deg);opacity:.6}}
.kzl-map{position:fixed;inset:0;z-index:1000;display:flex;flex-direction:column;background:linear-gradient(180deg,#B3E5FC 0%,#C8E6C9 55%,#FFF59D 100%);font-family:'Nunito','KZ Cyrillic',sans-serif;}
.kzl-map-hdr{display:flex;align-items:center;justify-content:space-between;padding:14px 16px 4px;font-size:20px;font-weight:900;color:#1B5E20;}
.kzl-map-x{border:none;background:rgba(255,255,255,.85);border-radius:50%;width:40px;height:40px;font-size:18px;font-weight:900;color:#555;cursor:pointer;}
.kzl-map-sub{padding:0 16px 8px;font-size:13px;font-weight:800;color:#33691E;}
.kzl-map-path{flex:1;overflow-y:auto;padding:24px 0 48px;}
.kzl-row{position:relative;height:84px;}
.kzl-map-path{position:relative;}
.kzl-map-line{position:absolute;left:0;top:24px;width:100%;pointer-events:none;}
.kzl-map-line polyline{fill:none;stroke:rgba(255,255,255,.85);stroke-width:7;stroke-linecap:round;stroke-dasharray:2 12;vector-effect:non-scaling-stroke;}
.kzl-node{position:absolute;top:12px;transform:translateX(-50%);width:60px;height:60px;border-radius:50%;border:4px solid #fff;background:#FF6B35;color:#fff;font-size:20px;font-weight:900;box-shadow:0 5px 0 #C94F20,0 8px 18px rgba(0,0,0,.18);cursor:pointer;z-index:1;font-family:inherit;touch-action:manipulation;}
.kzl-node.gift{background:#8E24AA;box-shadow:0 5px 0 #5E1675,0 8px 18px rgba(0,0,0,.18);}
.kzl-node.locked{background:#CFD8DC;box-shadow:0 5px 0 #90A4AE;font-size:18px;cursor:default;}
.kzl-node.cur{animation:kzlNode 1.4s ease-in-out infinite;outline:5px solid #FFD600;outline-offset:3px;}
@keyframes kzlNode{0%,100%{transform:translateX(-50%) scale(1)}50%{transform:translateX(-50%) scale(1.1)}}
.kzl-mark{position:absolute;top:-22px;right:-18px;font-size:30px;filter:drop-shadow(0 2px 2px rgba(0,0,0,.2));}
`;

/* ══ PUBLIC API ════════════════════════════════════════════════════════ */
window.KZL = {
  cfgFor,
  /** Most choices a game's layout can show; a game that handles more raises it. */
  maxChoices: 4,

  /* ─── Level persistence (per child, no upper limit) ─── */
  /** The level being played. Replaying an old level lowers it; [getBest] keeps the record. */
  getLevel(id) {
    this._gameId = this._gameId || id;
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
      // The dashboard names games by their catalog id (abc-uz, abc-ru… share one level id).
      const parentApp = window.parent && window.parent.app;
      const bridge = window.AndroidBridge || (window.parent && window.parent.AndroidBridge);
      if (bridge && bridge.levelReached) bridge.levelReached((parentApp && parentApp.currentGameId) || id, lv);
    }
  },
  nextLevel(id) {
    const next = this.getLevel(id) + 1;
    const isNewBest = next > this.getBest(id);
    localStorage.removeItem(key('fails', id));
    this.setLevel(id, next);
    if (isNewBest && next % 5 === 0) this._award(id, next);
    return next;
  },

  /** Stickers earned so far by the playing child: [{e, g, lv, gold}], oldest first. */
  stickers: readStickers,
  stickerFor,

  _award(id, level) {
    const s = Object.assign(stickerFor(id, level), {g: id, lv: level});
    const all = readStickers();
    all.push(s);
    localStorage.setItem(stickerKey(), JSON.stringify(all));
    s.bonus = level % 10 === 0;
    if (s.bonus) {
      const app = window.parent && window.parent.app;
      if (app && app.addPoints) app.addPoints(100);
    }
    // Games that show the level-up card fold the sticker into it; the rest get their own card.
    this._pending = s;
    setTimeout(() => { if (this._pending === s) { this._pending = null; this._stickerCard(s); } }, 1200);
  },

  _stickerCard(s, onDone) {
    this._inject();
    const el = document.createElement('div');
    el.className = 'kzl-level-up';
    el.innerHTML =
      `<div class="kzl-lup-card">
        <div class="kzl-stk-big${s.gold ? ' gold' : ''}">${s.e}</div>
        <div class="kzl-lup-txt">${tr(L.sticker)}</div>
        ${s.bonus ? `<div class="kzl-stk-bonus">${tr(L.bonus)}</div>` : ''}
        <button class="kzl-lup-btn">${tr(L.ok)}</button>
      </div>`;
    el.querySelector('button').onclick = () => { el.remove(); if (onDone) onDone(); };
    document.body.appendChild(el);
    if (s.bonus) this._burst();
  },

  _burst() {
    for (let i = 0; i < 24; i++) {
      const p = document.createElement('div');
      p.className = 'kzl-burst';
      p.textContent = ['🎉', '⭐', '✨', '🌟', '🎊'][i % 5];
      p.style.left = (5 + Math.random() * 90) + 'vw';
      p.style.animationDelay = (Math.random() * 0.6) + 's';
      document.body.appendChild(p);
      setTimeout(() => p.remove(), 2200);
    }
  },

  /* ─── Play together (Faza 18): child and parent take turns on one phone ───
     A game adds two lines: KZL.duo.answer(true) where it scores a right answer, and
     `if (KZL.duo.end()) return;` at the top of its result screen. Misses come from lives.lose(). */
  duo: {
    active: new URLSearchParams(location.search).get('duo') === '1',
    turn: 0,
    scores: [0, 0],
    _txt: {
      uz: {kid: "Bola", parent: "Ota-ona", turn: "navbati", btn: "Birga o'ynash", solo: "Yakka o'yin",
           again: "Yana bellashamiz", win: "g'olib! 🏆", tie: "Durang! Ikkalangiz ham zo'r! 🤝", bonus: "Birga o'ynaganingiz uchun +20 ⭐"},
      ru: {kid: "Ребёнок", parent: "Родитель", turn: "ходит", btn: "Играть вместе", solo: "Играть одному",
           again: "Ещё раз", win: "победил! 🏆", tie: "Ничья! Вы оба молодцы! 🤝", bonus: "+20 ⭐ за игру вместе"},
      en: {kid: "Kid", parent: "Parent", turn: "to play", btn: "Play together", solo: "Play alone",
           again: "Play again", win: "wins! 🏆", tie: "A tie! You're both great! 🤝", bonus: "+20 ⭐ for playing together"},
    },
    t() { return this._txt[langOf()] || this._txt.en; },
    players() {
      const pm = window.parent && window.parent.profileManager;
      const kid = (pm && pm.getActive && pm.getActive().name) || this.t().kid;
      return [{em: '🐣', name: kid}, {em: '🦁', name: this.t().parent}];
    },
    /** Called by the game on a right answer, and by lives.lose() on a miss. */
    answer(correct) {
      if (!this.active) return;
      if (correct) this.scores[this.turn] += 1;
      this.turn = 1 - this.turn;
      setTimeout(() => this._banner(), 650);
    },
    /** Shows who won instead of the game's own result. True when it did. */
    end() {
      if (!this.active) return false;
      window.KZL._inject();
      const t = this.t(), p = this.players(), [a, b] = this.scores;
      const head = a === b ? t.tie : `${(a > b ? p[0] : p[1]).em} ${(a > b ? p[0] : p[1]).name} ${t.win}`;
      const app = window.parent && window.parent.app;
      if (app && app.addPoints) app.addPoints(20);
      const el = document.createElement('div');
      el.className = 'kzl-level-up';
      el.innerHTML =
        `<div class="kzl-lup-card">
          <div class="kzl-duo-score">${p[0].em} ${a} <span>:</span> ${b} ${p[1].em}</div>
          <div class="kzl-lup-txt"></div>
          <div class="kzl-stk-bonus">${t.bonus}</div>
          <button class="kzl-lup-btn" data-a="again">${t.again}</button>
          <button class="kzl-duo-solo" data-a="solo">${t.solo}</button>
        </div>`;
      el.querySelector('.kzl-lup-txt').textContent = head;
      el.querySelector('[data-a=again]').onclick = () => location.reload();
      el.querySelector('[data-a=solo]').onclick = () => this._go(false);
      document.body.appendChild(el);
      window.KZL._burst();
      return true;
    },
    _go(on) {
      const q = new URLSearchParams(location.search);
      if (on) q.set('duo', '1'); else q.delete('duo');
      location.search = q.toString();
    },
    _banner() {
      if (!this.active) return;
      window.KZL._inject();
      let el = document.getElementById('kzl-duo');
      if (!el) {
        el = document.createElement('div');
        el.id = 'kzl-duo';
        el.className = 'kzl-duo-banner';
        document.body.appendChild(el);
      }
      const p = this.players(), cur = p[this.turn];
      el.innerHTML = `<b>${cur.em} ${cur.name} ${this.t().turn}</b><span>${p[0].em} ${this.scores[0]} : ${this.scores[1]} ${p[1].em}</span>`;
      el.classList.toggle('parent', this.turn === 1);
      // Grown-ups play without hints.
      const hint = document.getElementById('kzl-hint');
      if (hint) hint.style.visibility = this.turn === 1 ? 'hidden' : '';
    },
    /** The 👨‍👧 button that switches a game between solo and together. */
    _button() {
      const b = document.createElement('button');
      b.className = 'kzl-htp-btn kzl-duo-btn' + (this.active ? ' on' : '');
      b.textContent = this.active ? '🐣' : '👨‍👧';
      b.setAttribute('aria-label', this.active ? this.t().solo : this.t().btn);
      b.onclick = () => this._go(!this.active);
      document.body.appendChild(b);
      if (this.active) setTimeout(() => this._banner(), 300);
    },
  },

  /* ─── Level map: a winding path, gifts every 5 levels, castles every 10 ─── */
  showMap(id, onPick) {
    this._inject();
    id = id || this._gameId;
    if (!id) return;
    const best = this.getBest(id), cur = this.getLevel(id);
    const from = Math.max(1, best - 14), to = best + 5;
    const got = {};
    readStickers().filter(s => s.g === id).forEach(s => { got[s.lv] = s.e; });
    const nextGift = Math.ceil((best + 1) / 5) * 5;
    const X = [50, 74, 82, 74, 50, 26, 18, 26];
    let rows = '';
    const pts = [];
    for (let n = to; n >= from; n--) {
      pts.push(X[n % X.length] + ',' + ((to - n) * 84 + 42));
      const locked = n > best;
      const mark = n % 10 === 0 ? (got[n] || '🏰') : n % 5 === 0 ? (got[n] || '🎁') : '';
      const cls = 'kzl-node' + (locked ? ' locked' : '') + (n === cur ? ' cur' : '') + (mark ? ' gift' : '');
      rows += `<div class="kzl-row"><button class="${cls}" data-n="${n}" style="left:${X[n % X.length]}%">` +
              `${mark ? `<span class="kzl-mark">${mark}</span>` : ''}${locked ? '🔒' : n}</button></div>`;
    }
    const el = document.createElement('div');
    el.className = 'kzl-map';
    el.innerHTML =
      `<div class="kzl-map-hdr"><span>🗺️ ${tr(L.map)}</span><button class="kzl-map-x">✕</button></div>
       <div class="kzl-map-sub">⭐ ${best} ${tr(L.level)} • 🎁 ${tr(L.next)}: ${nextGift}</div>
       <div class="kzl-map-path"><svg class="kzl-map-line" viewBox="0 0 100 ${pts.length * 84}" preserveAspectRatio="none" style="height:${pts.length * 84}px"><polyline points="${pts.join(' ')}"/></svg>${rows}</div>`;
    el.querySelector('.kzl-map-x').onclick = () => el.remove();
    el.querySelectorAll('.kzl-node:not(.locked)').forEach(b => {
      b.onclick = () => {
        const n = +b.dataset.n;
        this.setLevel(id, n);
        el.remove();
        if (onPick) onPick(n); else location.reload();
      };
    });
    document.body.appendChild(el);
    const c = el.querySelector('.kzl-node.cur') || el.querySelector('.kzl-node:not(.locked)');
    if (c) c.scrollIntoView({block: 'center'});
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

    // Every game with a how-to also gets the level map, just above the ❓.
    const mapBtn = document.createElement('button');
    mapBtn.className = 'kzl-htp-btn kzl-map-btn';
    mapBtn.textContent = '🗺️';
    mapBtn.setAttribute('aria-label', tr(L.map));
    mapBtn.onclick = () => this.showMap(cfg.levelId || this._gameId || cfg.id);
    document.body.appendChild(mapBtn);
    if (cfg.duo) this.duo._button();

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
          <div id="kzl-lup-k"></div>
          <button class="kzl-lup-btn" id="kzl-lup-b"></button>
        </div>`;
      document.body.appendChild(el);
    }
    document.getElementById('kzl-lup-s').textContent = cfg ? cfg.stars : '🌟';
    document.getElementById('kzl-lup-n').textContent = newLevel;
    document.getElementById('kzl-lup-t').textContent = SUB[l] || SUB.en;
    const s = this._pending;
    this._pending = null;
    document.getElementById('kzl-lup-k').innerHTML = s
      ? `<div class="kzl-stk-row"><span class="kzl-stk-big${s.gold ? ' gold' : ''}">${s.e}</span>${tr(L.sticker)}</div>` +
        (s.bonus ? `<div class="kzl-stk-bonus">${tr(L.bonus)}</div>` : '')
      : '';
    if (s && s.bonus) this._burst();
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
      lose() {
        // Playing together: a miss is the other player's turn, nobody runs out of hearts.
        if (window.KZL.duo.active) { window.KZL.duo.answer(false); return false; }
        rem = Math.max(0, rem - 1); render(); return rem === 0;
      },
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
