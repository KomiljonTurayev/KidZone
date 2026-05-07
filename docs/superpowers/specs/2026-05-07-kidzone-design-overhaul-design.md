# KidZone Design Overhaul — Spec
**Date:** 2026-05-07
**Scope:** Sub-project 1 of 4 (Design → Levels → Language → Device Test)
**Approach:** Shared CSS + JS injection (Approach A)

---

## 1. Goal

Replace the current inconsistent per-game styling (Material 3 mixed with dark themes, purple gradients, plain white) with a unified **Toca Boca / Sago Mini** aesthetic across all 36 games and the main index screen.

**What changes:** Visual appearance only — game mechanics, level logic, and language URLs remain untouched.

---

## 2. New Files

### `app/src/main/assets/www/kids-theme.css`

All design tokens and utility classes. Every game links this file.

**Color tokens:**
```css
:root {
  --kt-bg:        #FFF8F0;   /* warm cream background */
  --kt-surface:   #FFFFFF;
  --kt-text:      #2D2D2D;
  --kt-text-dim:  #7B7B7B;
  --kt-radius-card:   28px;
  --kt-radius-btn:    20px;
  --kt-radius-badge:  50px;
  --kt-font-title: 'Fredoka One', cursive;
  --kt-font-body:  'Nunito', sans-serif;
  --kt-spring: cubic-bezier(0.34, 1.56, 0.64, 1);
  --kt-smooth: cubic-bezier(0.4, 0, 0.2, 1);
}
```

**3D toy button effect (utility class `.kt-btn`):**
```css
.kt-btn {
  border: none;
  border-radius: var(--kt-radius-btn);
  font-family: var(--kt-font-title);
  transform: translateY(0);
  box-shadow: 0 5px 0 var(--kt-accent-dark);
  transition: transform 0.1s, box-shadow 0.1s;
}
.kt-btn:active {
  transform: translateY(4px);
  box-shadow: 0 1px 0 var(--kt-accent-dark);
}
```

**Game card (index.html):**
```css
.kt-card {
  border-radius: var(--kt-radius-card);
  overflow: hidden;
  box-shadow: 0 5px 0 rgba(0,0,0,0.12);
  background: var(--kt-surface);
  cursor: pointer;
  transition: transform 0.15s var(--kt-spring);
}
.kt-card:active { transform: scale(0.94); }
.kt-card-top {
  /* 40% of card height, filled with accent color */
  background: var(--kt-accent);
  display: flex; align-items: center; justify-content: center;
  font-size: 40px;
}
.kt-card-bottom {
  padding: 8px 10px;
  font-family: var(--kt-font-title);
  font-size: 14px;
  color: var(--kt-text);
}
.kt-lv-badge {
  font-family: var(--kt-font-body);
  font-size: 11px;
  font-weight: 700;
  color: var(--kt-accent);
}
```

---

### `app/src/main/assets/www/kids-ui.js`

Injects the game header and overlays. All overlay text is trilingual.

**API:**
```javascript
KidUI.init({
  title: "Maze",          // game name (string, not translated — same in all langs)
  icon:  "🌀",           // emoji icon
  color: "#009688",       // accent color (hex)
  level: 1,              // starting level
  lang:  "uz"            // "uz" | "ru" | "en"
});

KidUI.showLevelUp({ level: 2, stars: 3, onNext: fn });
KidUI.updateLevel(n);
KidUI.showVictory({ totalStars: 45, onReplay: fn, onBack: fn });
```

**Injected header HTML:**
```html
<header id="kt-header" style="background: <color>">
  <button class="kt-back-btn" onclick="history.back()">←</button>
  <span class="kt-game-title"><icon> <title></span>
  <span class="kt-lv-pill">LV <n></span>
</header>
```

**Trilingual UI strings (inside kids-ui.js):**
```javascript
const KT = {
  uz: { back:"Orqaga", level:"Daraja", great:"Ajoyib!",
        cont:"Davom etish", replay:"Qayta", home:"Bosh sahifa",
        victory:"Barakalla!", allDone:"Barcha darajalar tugadi!" },
  ru: { back:"Назад",  level:"Уровень", great:"Отлично!",
        cont:"Продолжить", replay:"Снова", home:"Главная",
        victory:"Молодец!", allDone:"Все уровни пройдены!" },
  en: { back:"Back",   level:"Level",  great:"Amazing!",
        cont:"Continue", replay:"Replay", home:"Home",
        victory:"You did it!", allDone:"All levels complete!" }
};
```

**Level-up overlay:**
- Cream card (28px radius), dim backdrop
- 3 animated stars fly up (CSS keyframes)
- `great` text (Fredoka One, 28px), level number
- Single 3D toy button (accent color): `cont`

**Victory overlay:**
- Confetti burst (CSS particles, no library)
- `victory` text (Fredoka One, 32px)
- Total stars collected display (⭐ × n)
- Two 3D toy buttons: `replay` + `home`

---

## 3. Per-game Accent Colors

| Game | Color | Hex |
|------|-------|-----|
| maze | Teal | `#009688` |
| piano | Indigo | `#5C6BC0` |
| soccer | Green | `#43A047` |
| math-kids | Blue | `#1E88E5` |
| memory-match | Pink | `#E91E63` |
| memory-game | Purple | `#8E24AA` |
| shape-match | Violet | `#7C4DFF` |
| paint | Orange | `#FB8C00` |
| animals | Mint | `#00897B` |
| abc-game | Coral | `#EF5350` |
| abc-uz | Red-orange | `#FF5722` |
| abc-ru | Deep-orange | `#F4511E` |
| abc-en | Amber | `#FFB300` |
| bubble-pop | Sky Blue | `#039BE5` |
| tic-tac-toe | Deep Orange | `#F4511E` |
| jigsaw | Purple | `#AB47BC` |
| jump-rope | Lime | `#7CB342` |
| match | Amber | `#FFB300` |
| clock | Teal-blue | `#00ACC1` |
| catch-the-apple | Red | `#E53935` |
| dino-match | Emerald | `#2E7D32` |
| family-adventure | Warm Orange | `#FF6D00` |
| instrument | Deep Indigo | `#3949AB` |
| co-op-draw | Cyan | `#00BCD4` |
| color-rush | Deep Teal | `#00796B` |
| colors-shapes | Yellow | `#F9A825` |
| sing | Magenta | `#D81B60` |
| puzzle-slider | Deep Orange | `#E64A19` |
| fruits | Yellow-Green | `#9CCC65` → `#558B2F` |
| transport | Blue | `#1565C0` |
| weather | Sky | `#0288D1` |
| plants | Green | `#388E3C` |
| body | Coral | `#D84315` |
| shadow | Deep Purple | `#6A1B9A` |
| game-engine | Primary Orange | `#FF6B35` |

---

## 4. index.html Overhaul

**Layout:**
```
Header bar:   app name left  +  lang flags right  (krem, no shadow)
Mascot zone:  inline SVG owl, 80×80px, bob animation (2s, infinite)
              greeting text below: Fredoka One, 18px
Game grid:    CSS grid, responsive (3→4→5→6 cols)
              each cell = kt-card with accent top strip
```

**Mascot (inline SVG owl):**
- Simple geometric: circle head, triangle ears, dot eyes, wing shapes
- Bob animation: `translateY(0) → translateY(-8px)` over 2s ease-in-out
- Tap/click: eye-blink keyframe (scale scaleY(0.1) on eyes, 0.15s)
- No external image — fully inline SVG, ~60 lines

**Language selector:**
```html
<div class="kt-lang-bar">
  <button class="kt-lang-btn" data-lang="uz">🇺🇿 UZ</button>
  <button class="kt-lang-btn" data-lang="ru">🇷🇺 RU</button>
  <button class="kt-lang-btn" data-lang="en">🇬🇧 EN</button>
</div>
```
Active lang button: accent-color background, white text. Others: light grey.

---

## 5. Per-game Migration Pattern

Each of the 36 game HTML files gets this minimal change:

**Remove:** existing `<header>` / topbar HTML block  
**Add to `<head>`:**
```html
<link rel="stylesheet" href="kids-theme.css">
<script src="kids-ui.js"></script>
```
**Add to `<body>` (top):**
```html
<div id="kt-header-mount"></div>
```
**Add to `<script>` (after DOMContentLoaded or at top):**
```javascript
const lang = new URLSearchParams(location.search).get('lang') || 'en';
KidUI.init({ title:"Maze", icon:"🌀", color:"#009688", level:1, lang });
```

---

## 6. What Does NOT Change

- Game mechanics, rules, scoring logic
- KZL level progression system (`KidZoneGame` class)
- `main.js`, `game-engine.js`
- Java/Android source (`MainActivity`, managers)
- URL param convention (`?lang=uz&level=3`)
- `localStorage["kz-lang"]` for language persistence

---

## 7. Success Criteria

- [ ] All 36 games open with consistent Toca Boca header (accent color, white text)
- [ ] index.html shows owl mascot, warm cream background, game cards with colored tops
- [ ] Language switcher works: UZ/RU/EN updates all UI strings including overlay text
- [ ] Level-up overlay shows in correct language with spring animation
- [ ] Victory overlay shows with confetti
- [ ] No regressions in game mechanics
- [ ] Tested on USB-connected tablet (build + install)
