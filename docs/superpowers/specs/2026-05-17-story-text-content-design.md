# Story Text Content Design

**Date:** 2026-05-17  
**Feature:** Add prose text to all 20 stories in content.json

---

## Goal

Add a `text` field to each story entry in `content.json` so that tapping a story card shows the full story text in the existing `ai-viewer` modal and reads it aloud via `_doSpeak()`.

## Architecture

**Single file change:** `app/src/main/assets/www/content.json`

Each story entry gains:
```json
"text": {
  "uz": "...",
  "ru": "...",
  "en": "..."
}
```

No code changes required. `StoryManager._play()` already checks `item.text` and shows it in `#ai-viewer` with `_doSpeak()`.

## Content Spec

- **20 stories**, each with UZ / RU / EN text
- **Length:** 150–250 words per language
- **Audience:** Children aged 2–8, simple vocabulary
- **Style per category:**
  - `animals` — Aesop-style fable with a moral
  - `nature` — wonder and discovery
  - `heroes` — brave child protagonist
  - `family` — warm, nurturing tone
  - `space` — adventure and curiosity
- **Quality:** Independent stylistic translation per language (not literal machine translation)

## Stories

| ID | Title (UZ) | Category |
|----|------------|----------|
| story-001 | Sher va Sichqon | animals |
| story-002 | Fil va Chumoli | animals |
| story-003 | Toshbaqa va Quyon | animals |
| story-004 | Tulki va Uzum | animals |
| story-005 | Sehrli Daraxt | nature |
| story-006 | Yomg'irdan Keyin | nature |
| story-007 | Bahor Keldi | nature |
| story-008 | Qorbobo | nature |
| story-009 | Jasur Bolakay | heroes |
| story-010 | Kichkina Qahramonlar | heroes |
| story-011 | Yulduzga Sayohat | heroes |
| story-012 | Sehrgar Bola | heroes |
| story-013 | Biz Birgamiz | family |
| story-014 | Buvi Ertagi | family |
| story-015 | Yangi Uy | family |
| story-016 | Aka-Uka | family |
| story-017 | Koinotda Sayohat | space |
| story-018 | Yulduz Bolasi | space |
| story-019 | Marsdagi Sarguzasht | space |
| story-020 | Robot Do'stim | space |

## Songs

Not changed in this feature. Addressed in a future iteration.

## Testing

1. Tap any story card → `ai-viewer` opens with title + text
2. Text reads aloud automatically via `_doSpeak()`
3. Close button hides viewer
4. Language switch (UZ→RU→EN) → text updates to correct language
5. Stories without audio still show text (no regression)
