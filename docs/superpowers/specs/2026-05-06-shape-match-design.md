# Shape Match Game — Design Spec

**Date:** 2026-05-06  
**Target age:** 4–7 years  
**Status:** Approved

---

## Overview

A drag-and-drop shape matching game for KidZone. Colored shapes appear on screen alongside grey silhouette outlines. The player drags each shape onto its matching outline. All 20 KZL levels are supported, with difficulty scaling by increasing the number of shapes per round.

---

## Architecture

- **File:** `app/src/main/assets/www/shape-match.html` (single self-contained file)
- **Pattern:** Follows existing KidZone game conventions
  - Extends `KidZoneGame` from `game-engine.js`
  - Receives language via URL param: `?lang=uz|ru|en`
  - Local translations object: `const T = { uz: {…}, ru: {…}, en: {…} }`
  - KZL integration via `game.level`, `game.recordScore()`, `game.nextLevel()`
- **Rendering:** Inline SVG — no external image assets required, scales cleanly on all screen sizes

---

## Shapes

7 shapes total, introduced progressively across levels:

| Shape     | Color      |
|-----------|------------|
| Circle    | Red        |
| Square    | Blue       |
| Triangle  | Green      |
| Rectangle | Orange     |
| Diamond   | Purple     |
| Heart     | Pink       |
| Star      | Yellow     |

Each shape has one fixed color that is consistent across all levels. Outline slots are the same shape rendered as a light grey silhouette, slightly larger than the draggable shape to provide a clear drop target.

---

## Level Progression (KZL 1–20)

| Levels | Shapes on board | Shapes included                          |
|--------|-----------------|------------------------------------------|
| 1–4    | 2               | Circle, Square                           |
| 5–8    | 3               | + Triangle                               |
| 9–12   | 4               | + Rectangle                              |
| 13–15  | 5               | + Diamond                                |
| 16–18  | 6               | + Heart                                  |
| 19–20  | 7               | + Star (all shapes)                      |

Each level randomly positions shapes and their outline slots on screen. The same set of shapes is used throughout all rounds at a given level range — only positions change.

---

## Drag & Drop Mechanics

- **Input:** Touch and pointer events (works on Android WebView)
- **Dragging:** Shape follows the finger; all other shapes remain in place
- **Correct drop:** Shape snaps into the outline, locks in place, plays a soft "pop" success sound. Locked shapes are non-draggable.
- **Wrong drop:** Shape snaps back to its start position with a CSS shake animation and a short error beep. No penalty to score.
- **Empty space drop:** Treated as a wrong drop — shape returns to start.

---

## Level Completion

When all shapes are correctly matched:
1. All matched shapes pulse (scale animation) simultaneously
2. Win sound plays
3. `game.recordScore()` is called
4. `game.nextLevel()` is called after a short delay (~800ms)

---

## Visual Design

- **Shape size:** Minimum 80×80px (comfortable for 4-year-old fingers)
- **Outline slots:** Same SVG path, light grey (`#D0D0D0`), ~10% larger than the draggable shape
- **Layout:** Shapes and slots randomly distributed across the play area each level, with enough spacing to avoid overlap
- **Background:** Light, neutral — consistent with other KidZone games

---

## Translations

| Key       | UZ                        | RU                    | EN           |
|-----------|---------------------------|-----------------------|--------------|
| `title`   | Shakllarni Moslashtir     | Подбери форму         | Shape Match  |
| `circle`  | Doira                     | Круг                  | Circle       |
| `square`  | Kvadrat                   | Квадрат               | Square       |
| `triangle`| Uchburchak                | Треугольник           | Triangle     |
| `rectangle`| To'rtburchak             | Прямоугольник         | Rectangle    |
| `diamond` | Romb                      | Ромб                  | Diamond      |
| `heart`   | Yurak                     | Сердце                | Heart        |
| `star`    | Yulduz                    | Звезда                | Star         |

---

## Error Handling

- If `game-engine.js` fails to load, the game falls back to level 1 with 2 shapes (graceful degradation).
- Drag events are cancelled on scroll gestures to avoid conflicts with Android WebView scroll behavior.

---

## Out of Scope

- Color matching (shapes only)
- Time pressure / timer mechanic
- Sound name labels (no text shown for shape names during gameplay)
- 3D or shadow effects on shapes
