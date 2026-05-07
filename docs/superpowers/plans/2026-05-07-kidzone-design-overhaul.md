# KidZone Design Overhaul — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the visual styling of all 36 games and the main menu with a unified Toca Boca / Sago Mini aesthetic — warm cream backgrounds, colored game headers, 3D toy buttons, a mascot owl, and consistent level-up overlays — without touching any game mechanics or Android Java code.

**Architecture:** Two new shared files (`kids-theme.css`, `kids-ui.js`) are created. `kids-ui.js` injects the colored header into each game. `kids-theme.css` provides design tokens and overrides kzl overlay styles. `kz-ui.js` CSS string is updated so level-up cards become white Toca Boca style instead of orange gradient. `index.html` is fully rewritten (CSS + structure) while keeping all element IDs that `main.js` expects.

**Tech Stack:** Vanilla HTML/CSS/JS, no build step. All files in `app/src/main/assets/www/`. Existing `kz-ui.js` (KZL system) is kept intact — only its internal CSS string is updated. `main.js` is untouched.

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `kids-theme.css` | Design tokens, header styles, body override, overlay restyling |
| Create | `kids-ui.js` | `KidUI.init()` — injects colored header into any game |
| Modify | `kz-ui.js` | Update CSS string: level-up card → Toca Boca white |
| Rewrite | `index.html` | Toca Boca main menu (keep all IDs main.js relies on) |
| Modify (×5) | Batch 1 games | Link theme+ui, replace old header, call KidUI.init() |
| Modify (×10) | Batch 2 games | Same pattern |
| Modify (×10) | Batch 3 games | Same pattern |
| Modify (×9) | Batch 4 games | Same pattern |

---

## Task 1: Create `kids-theme.css`

**Files:**
- Create: `app/src/main/assets/www/kids-theme.css`

- [ ] **Step 1: Write the file**

```css
/* KidZone — Toca Boca Design System */
@import url('https://fonts.googleapis.com/css2?family=Fredoka+One&family=Nunito:wght@400;700;800;900&display=swap');

/* ── TOKENS ──────────────────────────────────────── */
:root {
  --kt-bg:           #FFF8F0;
  --kt-surface:      #FFFFFF;
  --kt-text:         #2D2D2D;
  --kt-text-dim:     #8A8A8A;
  --kt-accent:       #FF6B35;
  --kt-accent-dark:  #C94F20;
  --kt-radius-card:  24px;
  --kt-radius-btn:   20px;
  --kt-font-h:       'Fredoka One', cursive;
  --kt-font-b:       'Nunito', sans-serif;
  --kt-spring:       cubic-bezier(0.34, 1.56, 0.64, 1);
  --kt-hdr-h:        52px;
}

/* ── GAME BODY BASE ─────────────────────────────── */
body {
  background: var(--kt-bg);
  font-family: var(--kt-font-b);
}

/* ── GAME HEADER ────────────────────────────────── */
#kt-header {
  height: var(--kt-hdr-h);
  display: flex;
  align-items: center;
  padding: 0 12px;
  gap: 10px;
  flex-shrink: 0;
  /* background set inline per game */
}

.kt-back {
  width: 38px; height: 38px;
  background: rgba(255,255,255,0.28);
  border: none; border-radius: 50%;
  font-size: 18px; color: white;
  cursor: pointer; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.12s;
  touch-action: manipulation;
}
.kt-back:active { transform: scale(0.86); }

.kt-title {
  flex: 1;
  font-family: var(--kt-font-h);
  font-size: 17px; color: white;
  text-align: center;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.kt-lv {
  background: rgba(255,255,255,0.28);
  color: white;
  font-family: var(--kt-font-b);
  font-weight: 900; font-size: 11px;
  padding: 4px 11px;
  border-radius: 50px;
  flex-shrink: 0;
  white-space: nowrap;
}

/* ── OVERRIDE: kzl level-up card ────────────────── */
.kzl-lup-card {
  background: #FFFFFF !important;
  color: var(--kt-text) !important;
}
.kzl-lup-stars { color: #FFB300 !important; }
.kzl-lup-num   { color: var(--kt-accent) !important; }
.kzl-lup-txt   { color: var(--kt-text-dim) !important; }
.kzl-lup-btn {
  background: var(--kt-accent) !important;
  color: white !important;
  box-shadow: 0 4px 0 var(--kt-accent-dark) !important;
}
.kzl-lup-btn:active {
  transform: translateY(3px) !important;
  box-shadow: 0 1px 0 var(--kt-accent-dark) !important;
}

/* ── OVERRIDE: shape-match overlay ──────────────── */
.rd-card {
  background: var(--kt-surface) !important;
  background-image: none !important;
  color: var(--kt-text) !important;
}
.rd-title { color: var(--kt-accent) !important; }
.rd-sub   { color: var(--kt-text-dim) !important; }
.rd-btn {
  background: var(--kt-accent) !important;
  color: white !important;
  box-shadow: 0 4px 0 var(--kt-accent-dark) !important;
}
.rd-btn:active { transform: translateY(3px) scale(0.98) !important; }

/* ── OVERRIDE: maze level-up card ───────────────── */
#level-up-card {
  background: var(--kt-surface) !important;
  background-image: none !important;
  border: none !important;
  color: var(--kt-text) !important;
}
#level-up-em  { display: block; text-align: center; margin-bottom: 8px; }
#level-up-msg { color: var(--kt-accent) !important; }
#level-up-sub { color: var(--kt-text-dim) !important; }
#btn-next {
  background: var(--kt-accent) !important;
  border-color: var(--kt-accent) !important;
  box-shadow: 0 4px 0 var(--kt-accent-dark) !important;
}

/* ── OVERRIDE: memory-match container ───────────── */
#game-container {
  background: var(--kt-surface) !important;
  box-shadow: 0 6px 0 rgba(0,0,0,0.10) !important;
}
```

- [ ] **Step 2: Verify file exists**

```
dir app\src\main\assets\www\kids-theme.css
```
Expected: 1 file listed, size > 2000 bytes.

- [ ] **Step 3: Commit**

```
git add app/src/main/assets/www/kids-theme.css
git commit -m "feat(theme): add Toca Boca kids-theme.css design system"
```

---

## Task 2: Create `kids-ui.js`

**Files:**
- Create: `app/src/main/assets/www/kids-ui.js`

- [ ] **Step 1: Write the file**

```javascript
'use strict';
/* KidUI — injects a Toca Boca header into any game page.
   Call KidUI.init() after DOM is ready (or at top of <body>).
   Depends on: kids-theme.css being linked in <head>.
   Optional: if KZL is loaded, reads persisted level from it.
*/
const KidUI = (() => {
  const KT = {
    uz: { level: 'Daraja' },
    ru: { level: 'Уровень' },
    en: { level: 'Level' }
  };

  let _lang = 'en';

  /* Public API */
  function init({ title, icon, color, gameId, lang }) {
    _lang  = lang  || 'en';
    const lv = (typeof KZL !== 'undefined' && gameId)
               ? KZL.getLevel(gameId)
               : 1;

    const hdr = document.createElement('header');
    hdr.id = 'kt-header';
    hdr.style.background = color || '#FF6B35';
    hdr.innerHTML =
      `<button class="kt-back" onclick="history.back()">&#8592;</button>` +
      `<span class="kt-title">${icon} ${title}</span>` +
      `<span class="kt-lv" id="kt-lv">${_t('level')} ${lv}</span>`;

    /* Prepend to body so it sits above the flex children */
    document.body.prepend(hdr);
  }

  function updateLevel(n) {
    const el = document.getElementById('kt-lv');
    if (el) el.textContent = `${_t('level')} ${n}`;
  }

  function _t(k) {
    return (KT[_lang] || KT.en)[k];
  }

  return { init, updateLevel };
})();
```

- [ ] **Step 2: Verify**

```
dir app\src\main\assets\www\kids-ui.js
```
Expected: 1 file listed.

- [ ] **Step 3: Commit**

```
git add app/src/main/assets/www/kids-ui.js
git commit -m "feat(ui): add KidUI header-injection utility"
```

---

## Task 3: Update `kz-ui.js` — Toca Boca level-up card

**Files:**
- Modify: `app/src/main/assets/www/kz-ui.js` (lines 65–72, inside `const CSS = \`...\``)

The current level-up card CSS inside the CSS template string:
```
.kzl-lup-card{background:linear-gradient(135deg,#FF6B35,#FF9F1C);border-radius:32px;padding:36px 30px;text-align:center;color:#fff;width:min(300px,90vw);animation:kzlPop .4s cubic-bezier(.34,1.56,.64,1);}
.kzl-lup-stars{font-size:44px;margin-bottom:4px;}
.kzl-lup-num{font-size:72px;font-weight:900;line-height:1;}
.kzl-lup-txt{font-size:18px;font-weight:800;margin:8px 0 22px;}
.kzl-lup-btn{background:#fff;color:#FF6B35;border:none;border-radius:20px;padding:14px 34px;font-size:16px;font-weight:900;cursor:pointer;font-family:'Arial Rounded MT Bold',Arial,sans-serif;}
.kzl-lup-btn:active{transform:scale(.95);}
```

- [ ] **Step 1: Open `app/src/main/assets/www/kz-ui.js`**

Read the file and locate the `const CSS = \`...\`` string (starts around line 39).

- [ ] **Step 2: Replace the level-up card CSS lines**

Find this exact text inside the CSS string:
```
.kzl-lup-card{background:linear-gradient(135deg,#FF6B35,#FF9F1C);border-radius:32px;padding:36px 30px;text-align:center;color:#fff;width:min(300px,90vw);animation:kzlPop .4s cubic-bezier(.34,1.56,.64,1);}
```

Replace with:
```
.kzl-lup-card{background:#FFFFFF;border-radius:32px;padding:36px 30px;text-align:center;color:#2D2D2D;width:min(300px,90vw);animation:kzlPop .4s cubic-bezier(.34,1.56,.64,1);box-shadow:0 8px 32px rgba(0,0,0,0.14);}
```

Find:
```
.kzl-lup-btn{background:#fff;color:#FF6B35;border:none;border-radius:20px;padding:14px 34px;font-size:16px;font-weight:900;cursor:pointer;font-family:'Arial Rounded MT Bold',Arial,sans-serif;}
.kzl-lup-btn:active{transform:scale(.95);}
```

Replace with:
```
.kzl-lup-btn{background:#FF6B35;color:#fff;border:none;border-radius:20px;padding:14px 34px;font-size:16px;font-weight:900;cursor:pointer;font-family:'Arial Rounded MT Bold',Arial,sans-serif;box-shadow:0 4px 0 #C94F20;transform:translateY(0);transition:transform .08s,box-shadow .08s;}
.kzl-lup-btn:active{transform:translateY(4px);box-shadow:0 1px 0 #C94F20;}
```

- [ ] **Step 3: Commit**

```
git add app/src/main/assets/www/kz-ui.js
git commit -m "feat(theme): restyle kzl level-up card to Toca Boca white"
```

---

## Task 4: Rewrite `index.html`

**Files:**
- Modify: `app/src/main/assets/www/index.html`

**Important:** Keep every element ID that `main.js` uses. These IDs must survive the rewrite:
`#loader`, `#toast`, `#blobs`, `#age-modal`, `#lang-modal`, `#parent-modal`, `#reward-screen`, `#reward-card`, `#rw-stars`, `#rw-title`, `#rw-sub`, `#rw-pts`, `#gv`, `#gv-frame`, `#gv-top`, `#gv-back`, `#gv-title`, `#gv-fs`, `#gvb-txt`, `#shell`, `#hdr`, `#main`, `#logo-ball`, `#logo-name`, `#music-pill`, `#music-ic`, `#lang-pill`, `#lang-cur`, `#age-btn`, `#stars-pill`, `#stars-val`, `#hero`, `#hero-h`, `#hero-p`, `#hero-em`, `#challenge-bar`, `#level-card`, `#lv-name`, `#lv-sub`, `#lv-bar`, `#lv-pts`, `#cat-row`, `#featured-row`, `#game-grid`, `#parent-banner`, `#bnav`, `#ni-home`, `#ni-play`, `#ni-learn`, `#ni-stars`, `#ni-parent`, `#nb0`–`#nb4`, `#lc-en`, `#lc-ru`, `#lc-uz`, `#pd1`–`#pd4`, `#privacy-link`.

Also keep classes used by main.js: `.cat-chip`, `.gc`, `.gc-art`, `.gc-em`, `.gc-info`, `.gc-nm`, `.gc-stars`, `.feat-card`, `.feat-art`, `.feat-em`, `.feat-info`, `.feat-name`, `.feat-badge-hot`, `.age-card`, `.ac-em`, `.ac-range`, `.ac-desc`, `.ni`, `.ni-pill`, `.ni-ic`, `.ni-lb`, `.pin-dot`, `.pin-key`.

- [ ] **Step 1: Read the current index.html fully** (to preserve GAMES array, T object, and all script bridges verbatim)

- [ ] **Step 2: Write the new index.html**

Replace the entire file. Keep `<script src="main.js"></script>` and all inline `<script>` content (GAMES, T, bridge functions) exactly as-is. Only rewrite the `<style>` block and HTML structure.

New `<style>` block (replace the entire existing `<style>` block):

```css
/* ── RESET ── */
*{margin:0;padding:0;box-sizing:border-box;touch-action:manipulation;-webkit-tap-highlight-color:transparent}

/* ── TOKENS ── */
:root{
  --bg:#FFF8F0; --surface:#FFFFFF; --text:#2D2D2D; --dim:#8A8A8A;
  --accent:#FF6B35; --accent-dark:#C94F20;
  --spring:cubic-bezier(0.34,1.56,0.64,1);
  --smooth:cubic-bezier(0.4,0,0.2,1);
  --bh:0px;
  --cols:3; --gap:12px; --pad:14px;
}
@media(min-width:480px){:root{--cols:4;--gap:14px}}
@media(min-width:600px){:root{--cols:4;--gap:16px;--pad:20px}}
@media(min-width:768px){:root{--cols:5;--gap:18px;--pad:24px}}
@media(min-width:1024px){:root{--cols:6;--gap:20px}}

body{background:var(--bg);font-family:'Nunito',sans-serif;color:var(--text);overflow:hidden}

/* ── LOADER ── */
#loader{
  position:fixed;inset:0;z-index:500;
  background:var(--accent);
  display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16px;
}
#loader.h{display:none}
.ld-logo{font-size:80px;animation:ldB 1s ease-in-out infinite}
@keyframes ldB{0%,100%{transform:translateY(0) scale(1)}50%{transform:translateY(-15px) scale(1.08)}}
.ld-name{font-family:'Fredoka One',cursive;font-size:32px;color:white}
.ld-dots{display:flex;gap:8px}
.ld-dot{width:12px;height:12px;background:rgba(255,255,255,.5);border-radius:50%;animation:ldD 1.2s ease-in-out infinite}
.ld-dot:nth-child(2){animation-delay:.2s}
.ld-dot:nth-child(3){animation-delay:.4s}
@keyframes ldD{0%,80%,100%{transform:scale(.8);opacity:.5}40%{transform:scale(1.2);opacity:1}}

/* ── SHELL ── */
#shell{position:relative;z-index:1;display:flex;flex-direction:column;height:100vh;height:100dvh}
#main{flex:1;overflow-y:auto;overflow-x:hidden;padding:0 0 calc(var(--bh)+68px+16px);scroll-behavior:smooth}
#main::-webkit-scrollbar{display:none}

/* ── HEADER ── */
#hdr{
  background:var(--bg);
  height:58px;
  display:flex;align-items:center;
  padding:0 var(--pad);gap:10px;
  flex-shrink:0;
}
#logo-wrap{display:flex;align-items:center;gap:8px}
#logo-ball{
  width:40px;height:40px;
  background:var(--accent);
  border-radius:14px;
  display:flex;align-items:center;justify-content:center;
  font-size:22px;
  box-shadow:0 4px 0 var(--accent-dark);
  flex-shrink:0;
}
#logo-text{display:flex;flex-direction:column;line-height:1}
#logo-name{font-family:'Fredoka One',cursive;font-size:20px;color:var(--text)}
#hdr-spacer{flex:1}
#music-pill{
  background:var(--surface);
  border:none;border-radius:50%;
  width:36px;height:36px;
  display:flex;align-items:center;justify-content:center;
  font-size:17px;cursor:pointer;
  box-shadow:0 3px 0 rgba(0,0,0,0.10);
}
#lang-pill{
  background:var(--surface);
  border:none;border-radius:20px;
  padding:6px 12px;
  font-size:12px;font-weight:800;cursor:pointer;
  box-shadow:0 3px 0 rgba(0,0,0,0.10);
  display:flex;align-items:center;gap:4px;
  font-family:'Nunito',sans-serif;color:var(--text);
}
#age-btn{
  background:var(--surface);border:none;border-radius:20px;
  padding:6px 10px;font-size:11px;font-weight:800;cursor:pointer;
  box-shadow:0 3px 0 rgba(0,0,0,0.10);
  font-family:'Nunito',sans-serif;color:var(--text);
}
#stars-pill{
  background:var(--surface);border-radius:20px;
  padding:5px 12px;
  display:flex;align-items:center;gap:5px;
  box-shadow:0 3px 0 rgba(0,0,0,0.10);
}
#stars-val{font-size:14px;font-weight:900;color:var(--accent);font-family:'Fredoka One',cursive}
@media(max-width:400px){#age-btn{display:none}}

/* ── MASCOT ZONE ── */
#kt-mascot{
  text-align:center;padding:12px var(--pad) 8px;
  display:flex;flex-direction:column;align-items:center;gap:4px;
}
#kt-owl{animation:owlBob 2s ease-in-out infinite}
@keyframes owlBob{0%,100%{transform:translateY(0)}50%{transform:translateY(-8px)}}
#kt-mascot p{font-family:'Fredoka One',cursive;font-size:16px;color:var(--text)}

/* ── HERO (kept for main.js compat; minimal visual) ── */
#hero{
  background:var(--surface);
  border-radius:20px;margin:0 var(--pad) 14px;
  padding:14px 16px;
  display:flex;align-items:center;gap:12px;
  box-shadow:0 4px 0 rgba(0,0,0,0.08);
}
#hero-text{flex:1}
#hero-h{font-family:'Fredoka One',cursive;font-size:clamp(16px,4vw,22px);color:var(--text);line-height:1.2}
#hero-p{color:var(--dim);font-size:12px;font-weight:700;margin-top:4px}
#hero-em{font-size:clamp(40px,10vw,60px);flex-shrink:0}

/* ── CHALLENGE BAR (hidden in new design — kept for main.js) ── */
#challenge-bar{display:none}

/* ── LEVEL CARD ── */
#level-card{
  background:var(--surface);
  border-radius:20px;padding:14px 16px;
  margin:0 var(--pad) 14px;
  display:flex;align-items:center;gap:12px;
  box-shadow:0 4px 0 rgba(0,0,0,0.08);
}
.lv-icon{font-size:36px;flex-shrink:0;animation:lvRock 3s ease-in-out infinite}
@keyframes lvRock{0%,100%{transform:rotate(-8deg)}50%{transform:rotate(8deg)}}
.lv-text{flex:1}
.lv-name{font-family:'Fredoka One',cursive;font-size:15px;color:var(--text)}
.lv-sub{color:var(--dim);font-size:11px;font-weight:700;margin-top:2px}
.lv-bar-wrap{background:rgba(0,0,0,0.07);border-radius:8px;height:7px;margin-top:7px;overflow:hidden}
.lv-bar{height:100%;border-radius:8px;background:var(--accent);transition:width 1s var(--smooth)}
.lv-pts{background:var(--accent);border-radius:16px;padding:8px 12px;text-align:center;flex-shrink:0;box-shadow:0 3px 0 var(--accent-dark)}
.lv-pts .pts-v{font-family:'Fredoka One',cursive;font-size:20px;color:white}
.lv-pts .pts-l{font-size:9px;color:rgba(255,255,255,.8);font-weight:700}

/* ── SECTION HEADERS ── */
.sec-hdr{display:flex;align-items:center;gap:8px;margin:0 var(--pad) 10px}
.sec-icon{font-size:20px}
.sec-title{font-family:'Fredoka One',cursive;font-size:17px;color:var(--text);flex:1}
.sec-badge{background:rgba(255,107,53,.12);color:var(--accent);font-size:10px;font-weight:900;border-radius:20px;padding:3px 10px}
.sec-more{background:none;border:none;color:var(--accent);font-size:12px;font-weight:800;cursor:pointer;font-family:'Nunito',sans-serif}

/* ── CATEGORIES ── */
#cat-row{
  display:flex;gap:8px;overflow-x:auto;padding:0 var(--pad) 14px;
  scrollbar-width:none;
}
#cat-row::-webkit-scrollbar{display:none}
.cat-chip{
  display:flex;align-items:center;gap:5px;
  background:var(--surface);border:none;
  border-radius:20px;padding:8px 14px;
  font-size:12px;font-weight:800;white-space:nowrap;cursor:pointer;
  box-shadow:0 3px 0 rgba(0,0,0,0.10);
  font-family:'Nunito',sans-serif;color:var(--text);
  transition:transform .12s var(--spring),box-shadow .12s;
  touch-action:manipulation;
}
.cat-chip:active{transform:translateY(3px);box-shadow:none}
.cat-chip.active{background:var(--accent);color:white;box-shadow:0 3px 0 var(--accent-dark)}

/* ── FEATURED ROW ── */
#featured-row{display:flex;gap:12px;overflow-x:auto;padding:0 var(--pad) 14px;scrollbar-width:none}
#featured-row::-webkit-scrollbar{display:none}
.feat-card{
  flex-shrink:0;width:clamp(140px,38vw,200px);
  border-radius:20px;overflow:hidden;cursor:pointer;
  box-shadow:0 5px 0 rgba(0,0,0,0.12);
  background:var(--surface);
  transition:transform .12s var(--spring);
}
.feat-card:active{transform:translateY(4px);box-shadow:0 1px 0 rgba(0,0,0,0.10)}
.feat-art{
  height:110px;display:flex;align-items:center;justify-content:center;
  font-size:56px;position:relative;overflow:hidden;
}
.feat-em{filter:drop-shadow(0 4px 8px rgba(0,0,0,.2));animation:featFloat 3s ease-in-out infinite}
@keyframes featFloat{0%,100%{transform:translateY(0)}50%{transform:translateY(-7px)}}
.feat-badge-hot{
  position:absolute;top:8px;left:8px;z-index:3;
  background:white;color:var(--accent);
  font-size:9px;font-weight:900;border-radius:10px;padding:2px 8px;
}
.feat-badge-new{
  position:absolute;top:8px;left:8px;z-index:3;
  background:white;color:#4CAF50;
  font-size:9px;font-weight:900;border-radius:10px;padding:2px 8px;
}
.feat-info{padding:9px 10px 11px;background:var(--surface)}
.feat-name{font-family:'Fredoka One',cursive;font-size:14px;color:var(--text)}
.feat-meta{display:flex;align-items:center;gap:5px;margin-top:4px}
.feat-tag{font-size:9px;font-weight:900;border-radius:10px;padding:2px 7px}
.feat-age{font-size:9px;color:var(--dim);font-weight:700}
.feat-stars{display:flex;align-items:center;gap:1px;margin-top:4px}
.star-filled{color:#FFB300;font-size:11px}
.star-empty{color:#DDD;font-size:11px}
.feat-plays{font-size:9px;color:#AAA;font-weight:700;margin-left:4px}

/* ── GAME GRID ── */
#game-grid{
  display:grid;
  grid-template-columns:repeat(var(--cols),1fr);
  gap:var(--gap);
  padding:0 var(--pad) 12px;
}
.gc{
  background:var(--surface);
  border-radius:20px;overflow:hidden;cursor:pointer;
  box-shadow:0 5px 0 rgba(0,0,0,0.12);
  transition:transform .12s var(--spring);
  position:relative;
}
.gc:active{transform:translateY(4px);box-shadow:0 1px 0 rgba(0,0,0,0.10)}
.gc-art{
  aspect-ratio:1;display:flex;align-items:center;justify-content:center;
  position:relative;overflow:hidden;
}
.gc-em{
  font-size:clamp(30px,7vw,46px);position:relative;z-index:1;
  filter:drop-shadow(0 3px 6px rgba(0,0,0,.15));
  animation:gcFloat 3s ease-in-out infinite;
}
@keyframes gcFloat{0%,100%{transform:translateY(0)}50%{transform:translateY(-4px)}}
.gc:nth-child(2n) .gc-em{animation-delay:-.6s}
.gc:nth-child(3n) .gc-em{animation-delay:-1.2s}
.gc-lock{
  position:absolute;inset:0;background:rgba(255,248,240,.75);
  display:flex;align-items:center;justify-content:center;
  font-size:22px;z-index:5;border-radius:18px;
}
.gc-info{padding:5px 8px 8px;text-align:center}
.gc-nm{font-family:'Fredoka One',cursive;font-size:11px;line-height:1.2;color:var(--text)}
@media(min-width:600px){.gc-nm{font-size:13px}}
.gc-stars{display:flex;justify-content:center;gap:1px;margin-top:2px}
.gs-f{color:#FFB300;font-size:8px}
.gs-e{color:#DDD;font-size:8px}
.gc-badge{
  position:absolute;top:5px;right:5px;z-index:4;
  font-size:7px;font-weight:900;border-radius:8px;padding:2px 5px;color:white;
}
.gb-new{background:#4CAF50}
.gb-hot{background:#FF5252}
.gb-pro{background:#9C27B0}
.gb-free{background:#2EC4B6}
.gc-age{
  position:absolute;top:5px;left:5px;z-index:4;
  font-size:8px;font-weight:900;border-radius:8px;padding:2px 5px;
  background:rgba(0,0,0,.25);color:white;
}
.gc-progress{position:absolute;bottom:5px;right:5px;z-index:4}
.progress-ring-svg{width:20px;height:20px}
.progress-ring-bg{fill:none;stroke:rgba(255,255,255,.3);stroke-width:3}
.progress-ring-fg{
  fill:none;stroke:white;stroke-width:3;stroke-linecap:round;
  stroke-dasharray:56.5;transform:rotate(-90deg);transform-origin:50% 50%;
  transition:stroke-dashoffset 1s var(--smooth);
}

/* ── PARENT BANNER ── */
#parent-banner{
  background:var(--surface);border-radius:20px;
  padding:12px 14px;margin:0 var(--pad) 16px;
  display:flex;align-items:center;gap:10px;
  box-shadow:0 4px 0 rgba(0,0,0,0.08);
}
.pb-icon{font-size:26px;flex-shrink:0}
.pb-text{flex:1}
.pb-title{font-weight:900;font-size:13px;color:var(--text)}
.pb-sub{font-size:10px;color:var(--dim);font-weight:700;margin-top:2px}
.pb-btn{
  background:var(--accent);color:white;border:none;border-radius:16px;
  padding:8px 12px;font-size:11px;font-weight:900;cursor:pointer;
  font-family:'Nunito',sans-serif;
  box-shadow:0 3px 0 var(--accent-dark);
}

/* ── BOTTOM NAV ── */
#bnav{
  position:fixed;bottom:var(--bh);left:0;right:0;height:64px;
  background:var(--surface);
  border-top:none;
  display:flex;align-items:stretch;
  box-shadow:0 -3px 0 rgba(0,0,0,0.06);
  z-index:20;
}
.ni{
  flex:1;display:flex;flex-direction:column;
  align-items:center;justify-content:center;gap:3px;
  cursor:pointer;border:none;background:transparent;
  touch-action:manipulation;
}
.ni:active{background:rgba(255,107,53,0.07)}
.ni-pill{
  display:flex;align-items:center;justify-content:center;
  width:56px;height:28px;border-radius:14px;
  transition:background .2s var(--smooth);
}
.ni.on .ni-pill{background:rgba(255,107,53,0.15)}
.ni-ic{font-size:20px}
.ni-lb{font-size:10px;font-weight:800;color:var(--dim);font-family:'Nunito',sans-serif}
.ni.on .ni-lb{color:var(--accent)}

/* ── MODALS ── */
.modal-overlay{
  position:fixed;inset:0;z-index:200;
  background:rgba(0,0,0,.4);backdrop-filter:blur(4px);
  display:flex;align-items:flex-end;justify-content:center;
  animation:ovIn .2s ease;
}
.modal-overlay.h{display:none}
@keyframes ovIn{from{opacity:0}to{opacity:1}}
.modal-sheet{
  background:var(--surface);border-radius:28px 28px 0 0;
  width:100%;max-width:520px;padding:20px 20px 32px;
  animation:shUp .3s var(--spring);
  max-height:85vh;overflow-y:auto;
}
@keyframes shUp{from{transform:translateY(100%)}to{transform:translateY(0)}}
.modal-handle{width:36px;height:4px;border-radius:2px;background:#EEE;margin:0 auto 14px}
.modal-title{font-family:'Fredoka One',cursive;font-size:20px;color:var(--text);margin-bottom:14px;text-align:center}
.age-options{display:grid;grid-template-columns:1fr 1fr;gap:10px}
.age-card{
  background:rgba(255,107,53,0.06);border:2px solid transparent;
  border-radius:16px;padding:12px;text-align:center;cursor:pointer;
  transition:all .2s var(--spring);
}
.age-card:active{transform:scale(.94)}
.age-card.sel{background:rgba(255,107,53,0.12);border-color:var(--accent)}
.age-card .ac-em{font-size:30px;margin-bottom:5px}
.age-card .ac-range{font-family:'Fredoka One',cursive;font-size:15px;color:var(--accent)}
.age-card .ac-desc{font-size:10px;color:var(--dim);font-weight:700;margin-top:2px}
.btn-primary{
  background:var(--accent);color:white;border:none;border-radius:20px;
  padding:14px 28px;font-size:15px;font-weight:900;cursor:pointer;width:100%;
  font-family:'Nunito',sans-serif;box-shadow:0 4px 0 var(--accent-dark);margin-top:10px;
}
.btn-primary:active{transform:translateY(3px);box-shadow:0 1px 0 var(--accent-dark)}
.btn-outlined{
  background:transparent;color:var(--accent);border:2px solid var(--accent);
  border-radius:20px;padding:12px 24px;font-size:14px;font-weight:900;
  cursor:pointer;width:100%;font-family:'Nunito',sans-serif;margin-top:6px;
}

/* ── PARENT MODAL ── */
.parent-lock{display:flex;flex-direction:column;align-items:center;gap:12px;padding:8px 0}
.lock-icon{font-size:46px}
.lock-title{font-family:'Fredoka One',cursive;font-size:20px;text-align:center}
.lock-desc{font-size:12px;color:var(--dim);font-weight:700;text-align:center;line-height:1.5}
.pin-row{display:flex;gap:10px;justify-content:center;margin:6px 0}
.pin-dot{
  width:46px;height:46px;background:rgba(255,107,53,0.08);
  border:2px solid rgba(0,0,0,0.10);border-radius:14px;
  display:flex;align-items:center;justify-content:center;font-size:22px;
  transition:all .2s var(--spring);
}
.pin-dot.filled{background:var(--accent);border-color:var(--accent);color:white}
.pin-pad{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;width:240px;margin:0 auto}
.pin-key{
  background:rgba(255,107,53,0.06);border:2px solid transparent;border-radius:14px;
  padding:13px;font-size:20px;font-weight:900;cursor:pointer;
  transition:all .15s var(--spring);text-align:center;font-family:'Nunito',sans-serif;
}
.pin-key:active{transform:scale(.9);background:rgba(255,107,53,0.15)}

/* ── REWARD SCREEN ── */
#reward-screen{
  position:fixed;inset:0;z-index:400;
  background:rgba(0,0,0,.7);backdrop-filter:blur(6px);
  display:flex;align-items:center;justify-content:center;
  animation:ovIn .3s ease;
}
#reward-screen.h{display:none}
#reward-card{
  background:var(--surface);border-radius:28px;
  padding:30px 24px;text-align:center;
  width:min(320px,90vw);
  animation:rwPop .4s var(--spring);
  box-shadow:0 8px 0 rgba(0,0,0,0.12);
}
@keyframes rwPop{from{transform:scale(.6);opacity:0}to{transform:scale(1);opacity:1}}
.rw-stars{font-size:44px;margin-bottom:8px}
.rw-title{font-family:'Fredoka One',cursive;font-size:24px;color:var(--accent);margin-bottom:4px}
.rw-sub{font-size:13px;color:var(--dim);font-weight:700;margin-bottom:14px}
.rw-pts{
  background:rgba(255,107,53,0.10);border-radius:16px;
  padding:10px 20px;display:inline-block;
  font-family:'Fredoka One',cursive;font-size:26px;color:var(--accent);margin-bottom:14px;
}

/* ── GAME VIEWER ── */
#gv{
  position:fixed;inset:0;z-index:100;
  background:var(--bg);display:flex;flex-direction:column;
  animation:gvIn .25s var(--spring);
}
#gv.h{display:none}
@keyframes gvIn{from{transform:translateY(100%);opacity:0}to{transform:translateY(0);opacity:1}}
#gv-top{
  height:52px;background:var(--accent);
  display:flex;align-items:center;padding:0 12px;gap:10px;flex-shrink:0;
}
#gv-back{
  background:rgba(255,255,255,0.25);border:none;border-radius:12px;
  padding:7px 12px;color:white;font-size:13px;font-weight:900;
  cursor:pointer;font-family:'Nunito',sans-serif;
  display:flex;align-items:center;gap:4px;
}
#gv-title{font-family:'Fredoka One',cursive;font-size:17px;color:white;flex:1;text-align:center}
#gv-fs{
  background:rgba(255,255,255,0.25);border:none;border-radius:12px;
  padding:7px;color:white;font-size:15px;cursor:pointer;
}
#gv-frame{flex:1;width:100%;border:none}

/* ── TOAST ── */
#toast{
  position:fixed;bottom:calc(var(--bh)+70px);left:50%;
  transform:translateX(-50%);
  background:rgba(0,0,0,.82);color:white;
  font-size:13px;font-weight:800;border-radius:20px;
  padding:10px 20px;z-index:300;
  opacity:0;transition:opacity .25s,transform .25s;
  pointer-events:none;white-space:nowrap;font-family:'Nunito',sans-serif;
}
#toast.s{opacity:1;transform:translateX(-50%) translateY(-4px)}

/* ── BLOBS (subtle, Toca Boca pastel) ── */
#blobs{position:fixed;inset:0;z-index:0;pointer-events:none;overflow:hidden}
.blob{position:absolute;border-radius:50%;opacity:.04;animation:blobD ease-in-out infinite}
@keyframes blobD{0%,100%{transform:translate(0,0) scale(1)}33%{transform:translate(20px,-15px) scale(1.04)}66%{transform:translate(-15px,10px) scale(.97)}}

/* ── BANNER OFFSET ── */
window.updateBannerOffset=function(h){document.documentElement.style.setProperty('--bh',h+'px')}
```

New HTML body (replace `<body>...</body>`, before closing `</body>` keep the existing `<script>` tags):

```html
<body>

<!-- LOADER -->
<div id="loader">
  <div class="ld-logo">🌈</div>
  <div class="ld-name">KidZone</div>
  <div class="ld-dots">
    <div class="ld-dot"></div><div class="ld-dot"></div><div class="ld-dot"></div>
  </div>
</div>

<!-- BLOBS -->
<div id="blobs">
  <div class="blob" style="width:380px;height:380px;background:#FF6B35;left:-80px;top:-80px;animation-duration:9s"></div>
  <div class="blob" style="width:280px;height:280px;background:#4CAF50;right:-50px;top:35%;animation-duration:11s;animation-delay:-4s"></div>
  <div class="blob" style="width:320px;height:320px;background:#2196F3;left:20%;bottom:-80px;animation-duration:13s;animation-delay:-7s"></div>
</div>

<div id="toast"></div>

<!-- AGE MODAL -->
<div class="modal-overlay h" id="age-modal" onclick="closeModal('age-modal')">
  <div class="modal-sheet" onclick="event.stopPropagation()">
    <div class="modal-handle"></div>
    <div class="modal-title">🎂 Age Selection</div>
    <div class="age-options">
      <div class="age-card sel" onclick="setAge('2-4',this)">
        <div class="ac-em">👶</div>
        <div class="ac-range">2–4 years</div>
        <div class="ac-desc">Colors, shapes, animals</div>
      </div>
      <div class="age-card" onclick="setAge('4-6',this)">
        <div class="ac-em">🧒</div>
        <div class="ac-range">4–6 years</div>
        <div class="ac-desc">Numbers, letters, music</div>
      </div>
      <div class="age-card" onclick="setAge('6-8',this)">
        <div class="ac-em">👦</div>
        <div class="ac-range">6–8 years</div>
        <div class="ac-desc">Math, logic, tests</div>
      </div>
      <div class="age-card" onclick="setAge('8+',this)">
        <div class="ac-em">🧑</div>
        <div class="ac-range">8+ years</div>
        <div class="ac-desc">All games unlocked</div>
      </div>
    </div>
    <button class="btn-primary" onclick="closeModal('age-modal')">✅ Save</button>
  </div>
</div>

<!-- LANG MODAL -->
<div class="modal-overlay h" id="lang-modal" onclick="closeModal('lang-modal')">
  <div class="modal-sheet" onclick="event.stopPropagation()">
    <div class="modal-handle"></div>
    <div class="modal-title">🌍 Select Language</div>
    <div class="age-options">
      <div class="age-card" id="lc-en" onclick="setLang('en')">
        <div class="ac-em">🇬🇧</div>
        <div class="ac-range">English</div>
      </div>
      <div class="age-card" id="lc-ru" onclick="setLang('ru')">
        <div class="ac-em">🇷🇺</div>
        <div class="ac-range">Русский</div>
      </div>
      <div class="age-card" id="lc-uz" onclick="setLang('uz')">
        <div class="ac-em">🇺🇿</div>
        <div class="ac-range">O'zbekcha</div>
      </div>
    </div>
    <button class="btn-primary" onclick="closeModal('lang-modal')">✅ Close</button>
  </div>
</div>

<!-- PARENT MODAL -->
<div class="modal-overlay h" id="parent-modal">
  <div class="modal-sheet">
    <div class="modal-handle"></div>
    <div class="parent-lock">
      <div class="lock-icon">🔐</div>
      <div class="lock-title">Ota-ona rejimi</div>
      <div class="lock-desc">Farzandingiz o'yin vaqtini va reklamalarni boshqaring</div>
      <div class="pin-row">
        <div class="pin-dot" id="pd1"></div>
        <div class="pin-dot" id="pd2"></div>
        <div class="pin-dot" id="pd3"></div>
        <div class="pin-dot" id="pd4"></div>
      </div>
      <div class="pin-pad">
        <div class="pin-key" onclick="pinTap(1)">1</div>
        <div class="pin-key" onclick="pinTap(2)">2</div>
        <div class="pin-key" onclick="pinTap(3)">3</div>
        <div class="pin-key" onclick="pinTap(4)">4</div>
        <div class="pin-key" onclick="pinTap(5)">5</div>
        <div class="pin-key" onclick="pinTap(6)">6</div>
        <div class="pin-key" onclick="pinTap(7)">7</div>
        <div class="pin-key" onclick="pinTap(8)">8</div>
        <div class="pin-key" onclick="pinTap(9)">9</div>
        <div class="pin-key" onclick="pinTap('←')">←</div>
        <div class="pin-key" onclick="pinTap(0)">0</div>
        <div class="pin-key" onclick="closeModal('parent-modal')">✕</div>
      </div>
      <div style="text-align:center;margin-top:18px">
        <a href="https://KomiljonTurayev.github.io/KidZone/privacy/kidzone-privacy.html"
           id="privacy-link" target="_blank"
           style="color:var(--accent);font-size:12px;font-weight:800;text-decoration:none">
          🔒 Privacy Policy
        </a>
      </div>
    </div>
  </div>
</div>

<!-- REWARD SCREEN -->
<div id="reward-screen" class="h">
  <div id="reward-card">
    <div class="rw-stars" id="rw-stars">⭐⭐⭐</div>
    <div class="rw-title" id="rw-title">Ajoyib!</div>
    <div class="rw-sub" id="rw-sub">Zo'r o'yndingiz!</div>
    <div class="rw-pts" id="rw-pts">+50 ⭐</div>
    <button class="btn-primary" onclick="closeReward()">🎮 Davom etish</button>
  </div>
</div>

<!-- GAME VIEWER -->
<div id="gv" class="h">
  <div id="gv-top">
    <button id="gv-back" onclick="closeGame()">← <span id="gvb-txt">Orqaga</span></button>
    <div id="gv-title">O'yin</div>
    <button id="gv-fs" onclick="toggleFS()">⛶</button>
  </div>
  <iframe id="gv-frame" sandbox="allow-scripts allow-same-origin allow-modals"></iframe>
</div>

<!-- MAIN SHELL -->
<div id="shell">
  <!-- HEADER -->
  <div id="hdr">
    <div id="logo-wrap">
      <div id="logo-ball">🌈</div>
      <div id="logo-text">
        <div id="logo-name">KidZone</div>
      </div>
    </div>
    <div id="hdr-spacer"></div>
    <div id="music-pill" onclick="toggleMusic()"><span id="music-ic">🔊</span></div>
    <div id="lang-pill" onclick="openModal('lang-modal')"><span id="lang-cur">🇬🇧 EN</span></div>
    <button id="age-btn" onclick="openModal('age-modal')">👶 2–4 ▾</button>
    <div id="stars-pill">⭐<span id="stars-val">0</span></div>
  </div>

  <!-- MASCOT -->
  <div id="kt-mascot">
    <svg id="kt-owl" viewBox="0 0 80 80" width="72" height="72" role="img" aria-label="KidZone mascot owl">
      <ellipse cx="40" cy="54" rx="22" ry="21" fill="#F5A623"/>
      <circle cx="40" cy="28" r="20" fill="#F5A623"/>
      <polygon points="26,12 20,2 32,10" fill="#E8961A"/>
      <polygon points="54,12 60,2 48,10" fill="#E8961A"/>
      <circle cx="33" cy="26" r="8" fill="white"/>
      <circle cx="47" cy="26" r="8" fill="white"/>
      <circle cx="34" cy="27" r="5" fill="#2D2D2D"/>
      <circle cx="48" cy="27" r="5" fill="#2D2D2D"/>
      <circle cx="35.5" cy="25.5" r="1.8" fill="white"/>
      <circle cx="49.5" cy="25.5" r="1.8" fill="white"/>
      <polygon points="40,32 36,38 44,38" fill="#FF9800"/>
      <ellipse cx="21" cy="57" rx="9" ry="12" fill="#E8961A" transform="rotate(-15,21,57)"/>
      <ellipse cx="59" cy="57" rx="9" ry="12" fill="#E8961A" transform="rotate(15,59,57)"/>
      <ellipse cx="40" cy="56" rx="13" ry="15" fill="#FFD180"/>
    </svg>
    <p id="kt-mascot-msg">Let's play! 🎉</p>
  </div>

  <!-- MAIN -->
  <div id="main">

    <!-- HERO -->
    <div id="hero">
      <div id="hero-text">
        <div id="hero-h">Play &amp; Learn! 🎯</div>
        <div id="hero-p">36 games · 3 languages · UZ · RU · EN</div>
      </div>
      <div id="hero-em">🎮</div>
    </div>

    <!-- CHALLENGE BAR (hidden, kept for main.js) -->
    <div id="challenge-bar" style="display:none">
      <div class="ch-icon"></div>
      <div class="ch-text">
        <div class="ch-title"></div>
        <div class="ch-sub"></div>
      </div>
      <button class="ch-btn"></button>
    </div>

    <!-- LEVEL CARD -->
    <div id="level-card">
      <div class="lv-icon">🌟</div>
      <div class="lv-text">
        <div class="lv-name" id="lv-name">New Player</div>
        <div class="lv-sub" id="lv-sub">Collect 0 / 100 points</div>
        <div class="lv-bar-wrap"><div class="lv-bar" id="lv-bar" style="width:0%"></div></div>
      </div>
      <div class="lv-pts">
        <div class="pts-v" id="lv-pts">0</div>
        <div class="pts-l">PTS</div>
      </div>
    </div>

    <!-- CATEGORIES -->
    <div class="sec-hdr" style="margin-bottom:8px">
      <span class="sec-icon">🏷️</span>
      <span class="sec-title" id="cat-title">Categories</span>
    </div>
    <div id="cat-row">
      <div class="cat-chip active" onclick="filterCat('all',this)"><span>🎮</span> All</div>
      <div class="cat-chip" onclick="filterCat('learn',this)"><span>📚</span> Learn</div>
      <div class="cat-chip" onclick="filterCat('math',this)"><span>🔢</span> Math</div>
      <div class="cat-chip" onclick="filterCat('colors',this)"><span>🎨</span> Colors</div>
      <div class="cat-chip" onclick="filterCat('music',this)"><span>🎵</span> Music</div>
      <div class="cat-chip" onclick="filterCat('puzzle',this)"><span>🧩</span> Puzzles</div>
      <div class="cat-chip" onclick="filterCat('sport',this)"><span>⚽</span> Sport</div>
      <div class="cat-chip" onclick="filterCat('nature',this)"><span>🌿</span> Nature</div>
    </div>

    <!-- FEATURED -->
    <div class="sec-hdr">
      <span class="sec-icon">🔥</span>
      <span class="sec-title">Most Popular</span>
      <span class="sec-badge">Top 5</span>
    </div>
    <div id="featured-row"></div>

    <!-- ALL GAMES -->
    <div class="sec-hdr">
      <span class="sec-icon">🎮</span>
      <span class="sec-title" id="grid-title">All Games</span>
      <span class="sec-badge" id="grid-count">36</span>
    </div>
    <div id="game-grid"></div>

    <!-- PARENT BANNER -->
    <div id="parent-banner">
      <div class="pb-icon">👨‍👩‍👧</div>
      <div class="pb-text">
        <div class="pb-title">Parental Control</div>
        <div class="pb-sub">Play time · Ads · Stats</div>
      </div>
      <button class="pb-btn" onclick="openModal('parent-modal')">Enter 🔐</button>
    </div>

  </div><!-- /#main -->
</div><!-- /#shell -->

<!-- BOTTOM NAV -->
<div id="bnav">
  <button class="ni on" id="ni-home" onclick="navTo('home')">
    <div class="ni-pill"><span class="ni-ic">🏠</span></div>
    <span class="ni-lb" id="nb0">Home</span>
  </button>
  <button class="ni" id="ni-play" onclick="navTo('play')">
    <div class="ni-pill"><span class="ni-ic">🎮</span></div>
    <span class="ni-lb" id="nb1">Play</span>
  </button>
  <button class="ni" id="ni-learn" onclick="navTo('learn')">
    <div class="ni-pill"><span class="ni-ic">📚</span></div>
    <span class="ni-lb" id="nb2">Learn</span>
  </button>
  <button class="ni" id="ni-stars" onclick="navTo('stars')">
    <div class="ni-pill"><span class="ni-ic">⭐</span></div>
    <span class="ni-lb" id="nb3">Stars</span>
  </button>
  <button class="ni" id="ni-parent" onclick="openModal('parent-modal')">
    <div class="ni-pill"><span class="ni-ic">👨‍👩‍👧</span></div>
    <span class="ni-lb" id="nb4">Parents</span>
  </button>
</div>
```

- [ ] **Step 3: Verify in browser (or device)**

Open `index.html` in a browser. Expected:
- Cream background (`#FFF8F0`)
- Orange logo box top-left
- Owl mascot bobbing below header
- Game cards have colored top strip + white bottom + 3D shadow
- Language pills in header
- Bottom nav has 5 items, no gradient

- [ ] **Step 4: Commit**

```
git add app/src/main/assets/www/index.html
git commit -m "feat(index): Toca Boca redesign — mascot, cream bg, 3D toy cards"
```

---

## Task 5: Apply to Batch 1 — maze, piano, soccer, shape-match, math-kids

**Files:**
- Modify: `app/src/main/assets/www/maze.html`
- Modify: `app/src/main/assets/www/piano.html`
- Modify: `app/src/main/assets/www/soccer.html`
- Modify: `app/src/main/assets/www/shape-match.html`
- Modify: `app/src/main/assets/www/math-kids.html`

**Migration pattern (apply to each game):**

**A. In `<head>`:** Add after the last `<meta>` tag:
```html
<link rel="stylesheet" href="kids-theme.css">
<script src="kz-ui.js"></script>
<script src="kids-ui.js"></script>
```
(If `kz-ui.js` is already loaded, don't add it again — just add `kids-theme.css` and `kids-ui.js`.)

**B. In `<body>`:** Find and DELETE the existing header HTML block. Common patterns:
- `<div id="header">...</div>` — delete entire block
- `<div id="topbar">...</div>` — delete entire block
- `<div id="hdr">...</div>` — delete entire block

**C. In `<script>`:** At the very start of the main game script (after `const lang = ...` or similar), add:
```javascript
KidUI.init({ title: TITLE, icon: ICON, color: COLOR, gameId: GAMEID, lang: lang });
```

Values for each game:

| Game file | title | icon | color | gameId |
|-----------|-------|------|-------|--------|
| `maze.html` | `"Maze"` | `"🌀"` | `"#009688"` | `"maze"` |
| `piano.html` | `"Mini Piano"` | `"🎹"` | `"#5C6BC0"` | `"piano"` |
| `soccer.html` | `"Mini Soccer"` | `"⚽"` | `"#43A047"` | `"soccer"` |
| `shape-match.html` | `"Shape Match"` | `"🔷"` | `"#7C4DFF"` | `"shape_match"` |
| `math-kids.html` | `"Math Kids"` | `"🔢"` | `"#1E88E5"` | `"math_kids"` |

**D. Maze-specific:** The old topbar had 3 `.pill` divs for score/level. These are now in the game body, not the topbar. Verify the maze still shows score inside the game area (it uses `#player`, `#goal`, `#controls` — these are fine, they're below the new header). If the old topbar was positioned using CSS `position:fixed` or affecting layout, ensure `body` is still `display:flex; flex-direction:column; height:100vh`.

**E. Soccer-specific:** `soccer.html` already loads `<script src="kz-ui.js"></script>`. Do not add it again.

- [ ] **Step 1: Apply maze.html**

1. Add `<link rel="stylesheet" href="kids-theme.css">` and `<script src="kids-ui.js"></script>` to `<head>`.
2. Delete `<div id="topbar">...</div>` from body.
3. Find where `lang` is determined (already exists in maze.html). After that line, add:
   ```javascript
   KidUI.init({ title:"Maze", icon:"🌀", color:"#009688", gameId:"maze", lang:lang });
   ```
4. Remove the CSS for `#topbar` and `.pill` from the `<style>` block.

- [ ] **Step 2: Apply piano.html**

1. Add links to `<head>` (both `kids-theme.css` and `kz-ui.js` and `kids-ui.js`).
2. Delete existing header HTML (look for `<div` with back button at top of body).
3. At the start of the game's `<script>`, after `const lang = ...`, add:
   ```javascript
   KidUI.init({ title:"Mini Piano", icon:"🎹", color:"#5C6BC0", gameId:"piano", lang });
   ```
4. Remove CSS for old header from the `<style>` block.

- [ ] **Step 3: Apply soccer.html**

1. Add `kids-theme.css` link to `<head>` (kz-ui.js is already there; add `kids-ui.js` too).
2. Delete `<div id="topbar">...</div>`.
3. After `const _SOC_LANG = ...`, add:
   ```javascript
   KidUI.init({ title:"Mini Soccer", icon:"⚽", color:"#43A047", gameId:"soccer", lang:_SOC_LANG });
   ```
4. Remove CSS for `#topbar`, `.pill` from `<style>`.

- [ ] **Step 4: Apply shape-match.html**

1. Add links to `<head>` (`kids-theme.css`, `kz-ui.js`, `kids-ui.js`).
2. Delete `<div id="header">...</div>` (back button, game-title, lv-badge).
3. The existing `#round-done` overlay is already restyled by `kids-theme.css` (`.rd-card` override). No JS changes needed for the overlay.
4. In `<script>`, after `const lang = ...`, add:
   ```javascript
   KidUI.init({ title:"Shape Match", icon:"🔷", color:"#7C4DFF", gameId:"shape_match", lang });
   ```
5. Remove CSS for `#header`, `#back-btn`, `#lv-badge`, `#game-title` from `<style>`.

- [ ] **Step 5: Apply math-kids.html**

1. Add links to `<head>`.
2. Delete existing header HTML from body.
3. After `const lang = ...`, add:
   ```javascript
   KidUI.init({ title:"Math Kids", icon:"🔢", color:"#1E88E5", gameId:"math_kids", lang });
   ```
4. Remove old header CSS.

- [ ] **Step 6: Commit**

```
git add app/src/main/assets/www/maze.html app/src/main/assets/www/piano.html app/src/main/assets/www/soccer.html app/src/main/assets/www/shape-match.html app/src/main/assets/www/math-kids.html
git commit -m "feat(theme): apply Toca Boca header to Batch 1 — maze, piano, soccer, shape-match, math-kids"
```

---

## Task 6: Apply to Batch 2 — 10 games

**Files:** `memory-match.html`, `memory-game.html`, `animals.html`, `abc-game.html`, `bubble-pop.html`, `paint.html`, `tic-tac-toe.html`, `jigsaw.html`, `jump-rope.html`, `match.html`

Apply the same 4-step pattern (add links → delete header → KidUI.init() → remove old header CSS) to each:

| File | title | icon | color | gameId |
|------|-------|------|-------|--------|
| `memory-match.html` | `"Memory Match"` | `"🧩"` | `"#E91E63"` | `"memory_match"` |
| `memory-game.html` | `"Memory Game"` | `"🧠"` | `"#8E24AA"` | `"memory_game"` |
| `animals.html` | `"Animals"` | `"🐾"` | `"#00897B"` | `"animals"` |
| `abc-game.html` | `"ABC Game"` | `"🔤"` | `"#EF5350"` | `"abc_game"` |
| `bubble-pop.html` | `"Bubble Pop"` | `"🫧"` | `"#039BE5"` | `"bubble_pop"` |
| `paint.html` | `"Paint"` | `"🖌️"` | `"#FB8C00"` | `"paint"` |
| `tic-tac-toe.html` | `"Tic Tac Toe"` | `"✖️"` | `"#F4511E"` | `"tic_tac_toe"` |
| `jigsaw.html` | `"Jigsaw"` | `"🧩"` | `"#AB47BC"` | `"jigsaw"` |
| `jump-rope.html` | `"Jump Rope"` | `"🪢"` | `"#7CB342"` | `"jump_rope"` |
| `match.html` | `"Match Pair"` | `"🔍"` | `"#C2185B"` | `"match"` |

**Note for `memory-match.html`:** The `body` background is a gradient (`#87CEEB → #5F9EA0`). With `kids-theme.css` the body gets `--kt-bg` override. The inner `#game-container` also gets the override via CSS. Verify the game still looks correct.

- [ ] **Step 1: Apply all 10 files** (one at a time, read → edit → verify header disappears from HTML)

- [ ] **Step 2: Commit**

```
git add app/src/main/assets/www/memory-match.html app/src/main/assets/www/memory-game.html app/src/main/assets/www/animals.html app/src/main/assets/www/abc-game.html app/src/main/assets/www/bubble-pop.html app/src/main/assets/www/paint.html app/src/main/assets/www/tic-tac-toe.html app/src/main/assets/www/jigsaw.html app/src/main/assets/www/jump-rope.html app/src/main/assets/www/match.html
git commit -m "feat(theme): apply Toca Boca header to Batch 2 — 10 games"
```

---

## Task 7: Apply to Batch 3 — 10 games

**Files:** `clock.html`, `catch-the-apple.html`, `dino-match.html`, `family-adventure.html`, `instrument.html`, `co-op-draw.html`, `color-rush.html`, `colors-shapes.html`, `sing.html`, `puzzle-slider.html`

| File | title | icon | color | gameId |
|------|-------|------|-------|--------|
| `clock.html` | `"Clock"` | `"⏰"` | `"#00ACC1"` | `"clock"` |
| `catch-the-apple.html` | `"Catch Apple"` | `"🍎"` | `"#E53935"` | `"catch_apple"` |
| `dino-match.html` | `"Dino Match"` | `"🦕"` | `"#2E7D32"` | `"dino_match"` |
| `family-adventure.html` | `"Family Adventure"` | `"🌈"` | `"#FF6D00"` | `"family_adv"` |
| `instrument.html` | `"Instruments"` | `"🎸"` | `"#3949AB"` | `"instrument"` |
| `co-op-draw.html` | `"Co-op Draw"` | `"🎨"` | `"#00BCD4"` | `"coop_draw"` |
| `color-rush.html` | `"Color Rush"` | `"🌀"` | `"#00796B"` | `"color_rush"` |
| `colors-shapes.html` | `"Colors & Shapes"` | `"🌈"` | `"#F9A825"` | `"colors_shapes"` |
| `sing.html` | `"Sing Songs"` | `"🎤"` | `"#D81B60"` | `"sing"` |
| `puzzle-slider.html` | `"Puzzle Slider"` | `"🧩"` | `"#E64A19"` | `"puzzle_slider"` |

- [ ] **Step 1: Apply all 10 files**

- [ ] **Step 2: Commit**

```
git add app/src/main/assets/www/clock.html app/src/main/assets/www/catch-the-apple.html app/src/main/assets/www/dino-match.html app/src/main/assets/www/family-adventure.html app/src/main/assets/www/instrument.html app/src/main/assets/www/co-op-draw.html app/src/main/assets/www/color-rush.html app/src/main/assets/www/colors-shapes.html app/src/main/assets/www/sing.html app/src/main/assets/www/puzzle-slider.html
git commit -m "feat(theme): apply Toca Boca header to Batch 3 — 10 games"
```

---

## Task 8: Apply to Batch 4 — 9 games

**Files:** `fruits.html`, `transport.html`, `weather.html`, `plants.html`, `body.html`, `shadow.html`, `abc-uz.html`, `abc-ru.html`, `abc-en.html`

| File | title | icon | color | gameId |
|------|-------|------|-------|--------|
| `fruits.html` | `"Fruits"` | `"🍎"` | `"#7CB342"` | `"fruits"` |
| `transport.html` | `"Transport"` | `"🚗"` | `"#1565C0"` | `"transport"` |
| `weather.html` | `"Weather"` | `"🌤️"` | `"#0288D1"` | `"weather"` |
| `plants.html` | `"Plants"` | `"🌱"` | `"#388E3C"` | `"plants"` |
| `body.html` | `"Body Parts"` | `"👁️"` | `"#D84315"` | `"body"` |
| `shadow.html` | `"Find Shadow"` | `"👤"` | `"#6A1B9A"` | `"shadow"` |
| `abc-uz.html` | `"Uzbek ABC"` | `"🔤"` | `"#FF5722"` | `"abc_uz"` |
| `abc-ru.html` | `"Russian ABC"` | `"А"` | `"#F44336"` | `"abc_ru"` |
| `abc-en.html` | `"English ABC"` | `"🔡"` | `"#3F51B5"` | `"abc_en"` |

- [ ] **Step 1: Apply all 9 files**

- [ ] **Step 2: Commit**

```
git add app/src/main/assets/www/fruits.html app/src/main/assets/www/transport.html app/src/main/assets/www/weather.html app/src/main/assets/www/plants.html app/src/main/assets/www/body.html app/src/main/assets/www/shadow.html app/src/main/assets/www/abc-uz.html app/src/main/assets/www/abc-ru.html app/src/main/assets/www/abc-en.html
git commit -m "feat(theme): apply Toca Boca header to Batch 4 — 9 games"
```

---

## Task 9: Build and test on USB tablet

**Prerequisites:** USB debugging enabled on tablet, connected via USB, ADB available.

- [ ] **Step 1: Verify ADB detects device**

```
adb devices
```
Expected: device listed with `device` status (not `unauthorized`).

- [ ] **Step 2: Build debug APK**

In Android Studio: Build → Build Bundle(s)/APK(s) → Build APK(s)
Or via terminal:
```
.\gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL` and APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Install on tablet**

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Expected: `Success`

- [ ] **Step 4: Verify main screen**

Open KidZone on tablet. Check:
- [ ] Cream background visible
- [ ] Owl mascot bobbing
- [ ] Game cards have colored top strips (each game its own color)
- [ ] 3D shadow on cards (visible depth)
- [ ] Language buttons in header switch language
- [ ] Game cards open games correctly

- [ ] **Step 5: Verify game screens (test 5 representative games)**

Open: Maze, Piano, Soccer, Shape Match, Memory Game. In each check:
- [ ] Accent-colored header at top (correct color per game)
- [ ] Back button returns to main menu
- [ ] Level badge visible (LV N)
- [ ] Game mechanics work as before
- [ ] Level-up overlay is white card (not orange gradient)

- [ ] **Step 6: Verify language switching**

Switch to UZ, RU, EN on main screen. In each:
- [ ] Main menu text updates
- [ ] Open a game → level-up overlay text is in correct language

- [ ] **Step 7: Commit final**

```
git add -A
git commit -m "build: verified Toca Boca overhaul on USB tablet — all 36 games pass"
```

---

## Self-Review Notes

**Spec coverage:**
- ✅ `kids-theme.css` — Task 1
- ✅ `kids-ui.js` — Task 2
- ✅ `kz-ui.js` level-up card — Task 3
- ✅ `index.html` — Task 4
- ✅ 34 game files (36 total minus index.html and game-engine.html which is a template) — Tasks 5–8
- ✅ Device test — Task 9
- ✅ All element IDs main.js depends on preserved in Task 4

**Gaps addressed:**
- `game-engine.html` is a dev template, not in the GAMES array — intentionally skipped.
- `kids-theme.css` overrides `memory-match.html` body gradient and game-container background directly via CSS.
- `kz-ui.js` is already loaded in `soccer.html`; plan notes not to double-load.
- `lang` variable name differs per game (`lang`, `_SOC_LANG`, etc.) — Task 5 notes this.
