# Parental Dashboard Design

**Date:** 2026-05-17
**Feature:** PIN-protected parental dashboard with play-time stats, content filtering, and daily time limit

---

## Goal

Give parents a hidden, PIN-protected panel reachable by long-pressing the music button. The panel shows daily and weekly play-time stats, lets parents change the age group and hide content categories, and enforces a hard daily time limit that locks the app until the parent enters their PIN.

## Architecture

**Three new components:**
- `ParentalStatsManager` — tracks session time and game launches; all data in SharedPreferences
- `ParentalDashboardActivity` — full-screen native UI; single scrollable layout; reads/writes SharedPreferences and updates WebView localStorage on save
- Lock overlay — a native `FrameLayout` added to `MainActivity`'s root when the daily limit is reached

**Entry:** JS long-press (3 s) on `#music-pill` → `AndroidAdMob.openParentalDashboard()` → native PIN dialog → `ParentalDashboardActivity`.

## Files

| Action | File |
|--------|------|
| Create | `app/src/main/java/uz/kidzone/app/ParentalStatsManager.java` |
| Create | `app/src/main/java/uz/kidzone/app/ParentalDashboardActivity.java` |
| Create | `app/src/main/res/layout/activity_parental_dashboard.xml` |
| Create | `app/src/main/res/layout/view_lock_overlay.xml` |
| Modify | `app/src/main/java/uz/kidzone/app/MainActivity.java` |
| Modify | `app/src/main/AndroidManifest.xml` |
| Modify | `app/src/main/assets/www/index.html` |
| Modify | `app/src/main/res/values/strings.xml` |

## SharedPreferences Keys

All stored in `"kz_prefs"` (shared with existing keys).

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `kz_pin` | String | `""` | 4-digit PIN; empty = skipped/not set |
| `kz_time_limit` | int | `0` | Daily limit in minutes; `0` = no limit |
| `kz_pt_YYYYMMDD` | int | `0` | Total play minutes saved for that date |
| `kz_gl_YYYYMMDD` | String | `""` | Comma-separated game IDs launched that date |
| `kz_cat_off` | String | `""` | Comma-separated hidden categories, e.g. `"dance,lullaby"` |

## Entry Point — Long-Press Music Button

Add to `index.html`, attached to `#music-pill`:

```javascript
let _parentTimer;
document.getElementById('music-pill').addEventListener('touchstart', () => {
    _parentTimer = setTimeout(() => {
        if (window.AndroidAdMob) AndroidAdMob.openParentalDashboard();
    }, 3000);
}, { passive: true });
document.getElementById('music-pill').addEventListener('touchend', () => {
    clearTimeout(_parentTimer);
}, { passive: true });
```

Normal tap still fires the existing `click` handler unchanged.

## PIN Dialog (shown in MainActivity before opening dashboard)

Two states depending on `kz_pin`:

**PIN not set (`kz_pin == ""`):**
1. Show dialog: "Create a parent PIN" — 4 dot indicators + number pad
2. After 4 digits entered: "Confirm PIN" — re-enter to confirm
3. Match → save `kz_pin`, proceed to `ParentalDashboardActivity`
4. Mismatch → shake dots, reset, try again
5. "Skip" button → proceed without setting PIN (leaves `kz_pin == ""`)

**PIN already set:**
1. Show dialog: "Enter parent PIN" — 4 dot indicators + number pad
2. Correct → proceed to `ParentalDashboardActivity`
3. Wrong → shake dots (3-cycle horizontal translate), reset digits; no retry limit

Number pad layout: `[1][2][3] / [4][5][6] / [7][8][9] / [ ][0][⌫]`

The dialog is a native `AlertDialog` with a custom inflated view. No external library.

## ParentalDashboardActivity

Single `ScrollView` layout with 4 sections. Back arrow in toolbar returns to `MainActivity` (no data loss — all changes saved immediately on toggle/selection).

### Section 1 — Stats (📊)

- "Today: X min" — plain `TextView`
- Weekly bar chart: `LinearLayout` (horizontal) containing 7 `View` bars + day labels below each. Bar height is proportional to the max value in the 7-day window; minimum visible height 4dp for days with 0 minutes.
- "Today's games": row of emoji icons (one per unique game launched today), derived from `kz_gl_TODAY` and matched against `content.json` emoji field via a static lookup map.

### Section 2 — Content Filter (🎮)

**Age group:** 3 `MaterialButton` toggle cards (same style as onboarding: 2–4 / 5–7 / 8+). Tapping one saves `kz_age` and injects `localStorage.setItem('kz-age', '...')` into WebView via `MainActivity` static reference.

**Category toggles:** 9 rows, one per category. Each row: emoji + category name + `SwitchCompat`. On toggle:
- Update `kz_cat_off` in SharedPreferences (add/remove category)
- Inject `localStorage.setItem('kz-cat-off', '...')` into WebView

Categories: `alphabet`, `animals`, `dance`, `family`, `games`, `heroes`, `lullaby`, `nature`, `space`.

### Section 3 — Time Limit (⏰)

- `TextView` showing current limit: "30 min" or "No limit"
- `[−]` and `[+]` `MaterialButton`s, steps of 15 minutes, range 0–180
- `0` displays as "No limit"
- Change saves immediately to `kz_time_limit`

### Section 4 — PIN Settings (🔒)

- Single `MaterialButton`: "Change PIN" (if PIN set) or "Set PIN" (if not set)
- Taps into the same PIN-creation flow used on first access

## ParentalStatsManager

```java
public class ParentalStatsManager {
    ParentalStatsManager(Context ctx)

    void onSessionStart()               // records System.currentTimeMillis() to memory
    void onSessionEnd()                 // computes elapsed minutes, adds to kz_pt_TODAY
    void onGameLaunched(String gameId)  // appends gameId to kz_gl_TODAY (no duplicates within day)

    int getTodayMinutes()               // saved kz_pt_TODAY + current in-session minutes
    int[] getWeeklyMinutes()            // int[7]: index 0 = 6 days ago, index 6 = today
    List<String> getTodayGames()        // parsed kz_gl_TODAY

    int getTimeLimitMinutes()           // reads kz_time_limit
    void setTimeLimitMinutes(int m)     // writes kz_time_limit
    boolean isTimeLimitReached()        // getTodayMinutes() >= limit && limit > 0
}
```

Date keys use `new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date())`.

## MainActivity Changes

### New fields
```java
private ParentalStatsManager statsManager;
private android.os.Handler timeLockHandler;
private Runnable timeLockRunnable;
private View lockOverlay;
```

### Lifecycle hooks
```java
// onResume:
statsManager.onSessionStart();
timeLockHandler.postDelayed(timeLockRunnable, 60_000);

// onPause:
statsManager.onSessionEnd();
timeLockHandler.removeCallbacks(timeLockRunnable);
```

### timeLockRunnable (fires every 60 s while foregrounded)
```java
timeLockRunnable = () -> {
    if (statsManager.isTimeLimitReached()) showLockOverlay();
    else timeLockHandler.postDelayed(timeLockRunnable, 60_000);
};
```

### AdMobBridge new methods
```java
@JavascriptInterface
public void openParentalDashboard() {
    runOnUiThread(() -> showPinDialog());
}

@JavascriptInterface
public void gameLaunched(String gameId) {
    statsManager.onGameLaunched(gameId);
}
```

### showPinDialog()
Reads `kz_pin`. Routes to "create PIN" flow or "enter PIN" flow as described above. On success: `startActivity(new Intent(this, ParentalDashboardActivity.class))`.

### Passing WebView reference to Dashboard
`ParentalDashboardActivity` needs to call `evaluateJavascript` on the WebView after saving content filter changes. Use a static weak reference: `MainActivity` sets `MainActivity.instance = new WeakReference<>(this)` in `onCreate`, clears in `onDestroy`. `ParentalDashboardActivity` calls `MainActivity.instance.get().webViewManager.evaluateJavascript(...)`.

## Time Lock Overlay (`view_lock_overlay.xml`)

Full-screen `FrameLayout` over the entire window (`match_parent` × `match_parent`, background `#CC000000`).

```
Centre column (vertical LinearLayout):
  - ⏰ emoji, 64sp
  - "Vaqt tugadi!" / "Время вышло!" / "Time's up!" (from kz_lang)
  - "Davom etish uchun PIN kiriting" / equivalent
  - 4 dot indicators
  - Number pad (same layout as PIN dialog)
```

Correct PIN entered → `statsManager.setTimeLimitMinutes(0)` (disables lock for remainder of this session), remove overlay from root, restart `timeLockRunnable` next `onResume`.

Wrong PIN → shake dots, reset.

## Visual Style

- Background: `#FFF8F0`
- Accent: `#FF6B35`
- Section headers: bold 14sp, `#FF6B35`, all-caps
- Rounded corners: 16dp on cards, 24dp on buttons
- Full-screen, no status bar — same as `MainActivity`

## Testing

1. Long-press music button < 3 s → nothing happens
2. Long-press ≥ 3 s → PIN dialog appears
3. First launch: "Create PIN" flow — mismatch shows shake, match saves PIN and opens dashboard
4. "Skip" → opens dashboard without PIN
5. Second entry → "Enter PIN" flow
6. Wrong PIN → shake, no lockout
7. Stats section shows correct today minutes and 7-day chart
8. Toggling a category off → removed from WebView content immediately
9. Age group change → `kz-age` updated in WebView localStorage
10. Time limit set to 1 min → after 60 s overlay appears, blocks interaction
11. Correct PIN on overlay → overlay dismissed, play continues
12. `onPause` → `onResume` → session time accumulates correctly across backgrounding
13. New day → `kz_pt_YYYYMMDD` for yesterday preserved, today starts at 0
