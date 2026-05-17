# Onboarding Design

**Date:** 2026-05-17
**Feature:** First-launch onboarding screen (Native Android)

---

## Goal

Show a 3-step native onboarding on first app launch: language selection → age selection → Kidzo intro with TTS. After completion, persist choices and never show again.

## Architecture

**Single file approach:** One `OnboardingActivity` with one layout. Three sections shown/hidden via `setVisibility`. No Fragments, no ViewPager2.

**Persistence:** `SharedPreferences("kz_prefs")` — shared with existing app preferences.

**Bridge to WebView:** After WebView loads in `MainActivity`, inject `kz-lang` and `kz-age` into `localStorage` via `evaluateJavascript`.

## Files

| Action | File |
|--------|------|
| Create | `app/src/main/java/uz/kidzone/app/OnboardingActivity.java` |
| Create | `app/src/main/res/layout/activity_onboarding.xml` |
| Modify | `app/src/main/java/uz/kidzone/app/MainActivity.java` |
| Modify | `app/src/main/res/values/strings.xml` (add onboarding strings) |
| Modify | `app/src/main/AndroidManifest.xml` (register OnboardingActivity) |

## SharedPreferences Keys

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `kz_onboarding_done` | boolean | false | Whether onboarding has been completed |
| `kz_lang` | String | `"uz"` | Selected language code |
| `kz_age` | String | `"2-4"` | Selected age group |

## Flow

```
App launch → MainActivity.onCreate()
  → check SharedPreferences "kz_onboarding_done"
  → false: startActivity(OnboardingActivity), finish()
  → true: proceed normally, inject lang+age into WebView localStorage after load
```

## Step 0 — Language Selection

- Kidzo logo (`ic_bird_ai`) centered at top
- Title: "Tilni tanlang / Выберите язык / Choose language" (shown in all 3 simultaneously)
- 3 large buttons, one per row:
  - 🇺🇿 O'zbek
  - 🇷🇺 Русский
  - 🇬🇧 English
- Selecting a button highlights it (accent background), deselects others
- "Keyingi →" button at bottom — disabled until a language is selected
- Default pre-selected: none (user must explicitly choose)

## Step 1 — Age Selection

- Title in selected language: `"Yosh guruhini tanlang"` / `"Выберите возраст"` / `"Select age group"`
- 3 large cards in a row:
  - 👶 2–4
  - 🧒 5–7
  - 👦 8+
- Selecting a card highlights it
- "Keyingi →" button — disabled until an age is selected
- "← Orqaga" back link at top-left to return to Step 0

## Step 2 — Kidzo Intro

- Kidzo image (`ic_bird_ai`) starts at scale 0, animates to 1.15 then settles at 1.0 (spring, 600ms)
- Greeting text below image (in selected language):
  - UZ: `"Salom! Men Kidzo 🐥\nKidZone'ga xush kelibsiz!"`
  - RU: `"Привет! Я Кидзо 🐥\nДобро пожаловать в KidZone!"`
  - EN: `"Hi! I'm Kidzo 🐥\nWelcome to KidZone!"`
- TextToSpeech speaks the greeting once on screen entry (locale: `uz_UZ` / `ru_RU` / `en_US`)
- "Boshlash 🚀" / "Начать 🚀" / "Let's go 🚀" button
- On tap:
  1. Save `kz_onboarding_done = true`, `kz_lang`, `kz_age` to SharedPreferences
  2. Stop TTS
  3. `startActivity(new Intent(this, MainActivity.class))`
  4. `finish()`

## MainActivity Changes

### First-launch check (top of `onCreate`, before `initializeUI`):
```java
SharedPreferences prefs = getSharedPreferences("kz_prefs", MODE_PRIVATE);
if (!prefs.getBoolean("kz_onboarding_done", false)) {
    startActivity(new Intent(this, OnboardingActivity.class));
    finish();
    return;
}
```

### Language injection after WebView load:
In `KidWebViewManager` — after `onPageFinished`, or via `webViewManager.loadUrl` callback — inject:
```javascript
localStorage.setItem('kz-lang', '<saved_lang>');
localStorage.setItem('kz-age', '<saved_age>');
```

The injection uses `evaluateJavascript` called from `MainActivity` after `webViewManager.loadUrl`. Since `loadUrl` is async, the injection happens inside `KidWebViewManager.onPageFinished` callback, which is already wired to `MainActivity` via a listener or direct call.

Concretely: `KidWebViewManager` exposes `setOnPageReadyCallback(Runnable r)` — `MainActivity` sets a callback that calls `evaluateJavascript` with the lang/age injection once after the first page load.

## Visual Style

- Background: `#FFF8F0` (warm off-white, matches app palette)
- Accent color: `#FF6B35` (existing app accent)
- Font: Nunito / system default
- Rounded corners on all buttons (24dp radius)
- Full-screen (no status bar) — same as MainActivity

## Testing

1. Fresh install → OnboardingActivity opens, MainActivity does not
2. Complete onboarding (UZ, 5–7) → MainActivity opens, `kz-lang=uz` and `kz-age=5-7` set in WebView localStorage
3. Relaunch app → OnboardingActivity skipped, goes to MainActivity directly
4. Step 0: "Keyingi" disabled until language tapped
5. Step 1: "Keyingi" disabled until age tapped; back arrow returns to Step 0
6. Step 2: Kidzo animates in, TTS plays greeting once, "Boshlash" starts app
7. TTS stopped on `onDestroy` (no leak)
8. Rotate screen on Step 1 → selected age preserved (save to field, not just UI state)
