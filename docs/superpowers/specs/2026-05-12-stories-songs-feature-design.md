# Stories & Songs Feature Design
**Date:** 2026-05-12
**Status:** Approved

## Summary

Add an AI-style storytelling and music player feature to KidZone's main screen (`index.html`) using pre-recorded MP3 audio files. Users can browse content by category list or text search. The feature lives as two new tabs alongside the existing games tab.

---

## Section 1 — Architecture & Data Model

### File Structure

```
assets/www/
├── index.html           ← tab nav added
├── main.js              ← StoryManager, SongManager classes added
├── content.json         ← all content metadata (single file)
└── audio/
    ├── stories/
    │   ├── uz/story-001.mp3 ... story-020.mp3
    │   ├── ru/story-001.mp3 ... story-020.mp3
    │   └── en/story-001.mp3 ... story-020.mp3
    └── songs/
        ├── uz/song-001.mp3 ... song-020.mp3
        ├── ru/song-001.mp3 ... song-020.mp3
        └── en/song-001.mp3 ... song-020.mp3
```

### content.json Schema

```json
{
  "stories": [
    {
      "id": "story-001",
      "category": "animals",
      "emoji": "🦁",
      "title": { "uz": "Sher va Sichqon", "ru": "Лев и Мышь", "en": "Lion and Mouse" },
      "audio": {
        "uz": "audio/stories/uz/story-001.mp3",
        "ru": "audio/stories/ru/story-001.mp3",
        "en": "audio/stories/en/story-001.mp3"
      },
    }
  ],
  "songs": [
    {
      "id": "song-001",
      "category": "lullaby",
      "emoji": "🌙",
      "title": { "uz": "Alla", "ru": "Колыбельная", "en": "Lullaby" },
      "audio": {
        "uz": "audio/songs/uz/song-001.mp3",
        "ru": "audio/songs/ru/song-001.mp3",
        "en": "audio/songs/en/song-001.mp3"
      }
    }
  ]
}
```

> **Note:** `duration` field is intentionally omitted from the schema. Duration is read at runtime from `Audio.onloadedmetadata` so it always reflects the actual MP3 length and requires no manual maintenance.
```

### Categories

| Stories | Songs |
|---|---|
| animals (hayvonlar) | lullaby (allalar) |
| nature (tabiat) | alphabet (alifbo) |
| heroes (qahramonlar) | animals (hayvonlar) |
| family (oila) | dance (raqs) |
| space (koinot) | games (o'yin) |

### Initial Content Scale
- 20+ stories × 3 languages = 60+ MP3 files
- 20+ songs × 3 languages = 60+ MP3 files
- Designed for easy addition of new content (add to content.json + drop MP3 file)

---

## Section 2 — UI/UX Design

### Tab Navigation (top of index.html)

```
[ 🎮 O'yinlar ]  [ 📖 Ertaklar ]  [ 🎵 Qo'shiqlar ]
```

- Active tab: Toca Boca accent color, rounded, bold
- Inactive tabs: gray
- Selected tab persisted in `localStorage["kz-tab"]`

### Stories / Songs Tab Layout

```
┌─────────────────────────────────────┐
│  🔍 [Search / Qidiruv...]           │
├─────────────────────────────────────┤
│  [All] [Animals] [Nature] [Heroes]  │  ← category filter chips
├─────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐            │
│  │   🦁    │ │   🌙    │            │  ← content cards (grid)
│  │ Sher va │ │  Alla   │            │
│  │ Sichqon │ │         │            │
│  │  3:00   │ │  2:00   │            │
│  └─────────┘ └─────────┘            │
├─────────────────────────────────────┤
│  ▶  Sher va Sichqon      [■] [▶]   │  ← sticky audio player
│  ━━━━━━━━━━━━━━━  1:20 / 3:00      │
└─────────────────────────────────────┘
```

### Behavior Details
- **Search:** Real-time filter on title in current language; clears on tab switch
- **Category chips:** Single-select; "All" is default; combines with search filter
- **Content cards:** Emoji + title + duration; tap to load into player and auto-play
- **Sticky player:** Always visible at bottom of tab; shows title, progress bar, current/total time, play/pause and stop buttons
- **Language change:** Current audio stops; same content reloads in new language; search/category state preserved

---

## Section 3 — Manager Classes

### Class Hierarchy

```
ContentManager (base)
├── StoryManager
└── SongManager

AudioPlayer (singleton, shared by both managers)
UIManager (existing — switchTab() added)
```

### ContentManager API

```js
class ContentManager {
  constructor(type)        // 'stories' | 'songs'
  async load()             // fetch content.json, extract type array
  filter(query, category)  // returns filtered array by search + category
  render(items)            // renders card grid to DOM
  play(id)                 // delegates to AudioPlayer
}
```

### AudioPlayer API

```js
class AudioPlayer {
  play(src, title)     // load new audio and start playback
  pause()
  resume()
  seek(seconds)        // called by progress bar interaction
  onTimeUpdate(cb)     // callback for progress bar update
  onEnded(cb)          // auto-advance or stop
}
```

### UIManager Addition

```js
switchTab(tab)   // 'games' | 'stories' | 'songs'
                 // shows correct section, hides others
                 // saves to localStorage["kz-tab"]
                 // restores on page load
```

### i18n Integration
- All titles, category labels, and UI strings go through existing `TranslationManager`
- `T` object in `index.html` extended with keys: `stories`, `songs`, `search`, `all`, category names, player labels
- Audio src resolved as `item.audio[currentLang]` at play time

---

## Development Notes

- **Placeholder audio:** During development, 1–2 second silent MP3 placeholder files are used so the UI can be tested end-to-end before real recordings are available. Real MP3s drop in as a straight file replacement with no code changes.
- **20 stories + 20 songs** is the initial target; the system supports unlimited entries via `content.json` additions.

---

## Non-Goals (out of scope)

- AI-generated content (real API calls) — future version
- Offline download/caching of audio — future version
- Playlist / queue feature — future version
- In-game story overlay — future version
