# Faza 5: Story Viewer Fix + Song Lyrics Viewer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Content.json ertak ko'rishda AI regen tugmasini yashirish + qo'shiqlar uchun lyrics overlay qo'shish.

**Architecture:** Faqat 2 fayl — `index.html` va `main.js`. `#ai-viewer` regen tugmasi default yashirin bo'ladi, faqat AI mode da ko'rinadi. Yangi `#lyrics-viewer` overlay mavjud `.aiv-hdr`/`.aiv-body` CSS klasslari bilan yasaladi.

**Tech Stack:** Vanilla JavaScript, HTML5, CSS. Android WebView orqali ishlaydi.

## Global Constraints

- Faqat `app/src/main/assets/www/index.html` va `app/src/main/assets/www/main.js` o'zgartiriladi
- Native Kotlin kodi (.kt fayllar) o'zgartirilmaydi
- `.h` class = `display:none` — overlay show/hide uchun yagona pattern
- Yangi CSS klasslari minimal: faqat `#lyrics-viewer`, `#lyrics-viewer.h`, `.lv-lyrics`
- `SongManager._play()` — badge hook AVVAL, lyrics show, keyin `super._play(item)`
- `SongManager` — `ContentManager._play()` base metodini chaqirish majburiy: `super._play(item)`

---

## File Map

| Harakat | Fayl | Nima o'zgaradi |
|---------|------|----------------|
| Modify | `app/src/main/assets/www/index.html` | regen btn id+h; lyrics-viewer HTML+CSS; closeLyrics() |
| Modify | `app/src/main/assets/www/main.js` | generateAiStory() regen show; SongManager._play() lyrics |

---

## Task 1: Regen Tugma Fix + Song Lyrics Viewer

**Files:**
- Modify: `app/src/main/assets/www/index.html`
- Modify: `app/src/main/assets/www/main.js`

**Interfaces:**
- Consumes: `#ai-viewer` (mavjud overlay, z-index:350) — regen tugmasi qo'shiladi
- Consumes: `generateAiStory()` (main.js line 647) — regen btn show qo'shiladi
- Consumes: `SongManager._play(item)` (main.js line 404) — lyrics qo'shiladi
- Consumes: `ContentManager._play(item)` (main.js line 273) — audio player, `super._play()` orqali chaqiriladi
- Consumes: `window.badgeManager.onSongPlayed()` (mavjud) — badge hook
- Produces: `#lyrics-viewer` overlay — qo'shiq bosilganda ko'rinadi
- Produces: `closeLyrics()` — lyrics overlayni yopadi
- Produces: `document.getElementById('aiv-regen-btn')` — AI mode da show, default hidden

---

### 1a. index.html — Regen tugmaga id va h class qo'shish

`app/src/main/assets/www/index.html` da `#ai-viewer` ichidagi regen tugmasini toping (line ~970):

```html
<button class="aiv-play-btn" style="background:#4CAF50" onclick="generateAiStory()">
```

- [ ] **Step 1: Regen tugmasiga id va h class qo'shish**

Yuqoridagi qatorni quyidagicha almashtiring:

```html
<button id="aiv-regen-btn" class="aiv-play-btn h" style="background:#4CAF50" onclick="generateAiStory()">
```

---

### 1b. index.html — `#lyrics-viewer` CSS qo'shish

`index.html` da `/* ── AI STORY VIEWER ── */` CSS blokning oxirini toping (line ~210 atrofida, `.aiv-actions` yoki `.aiv-play-btn` dan keyin):

```css
.aiv-play-btn{
  flex:1; background:var(--accent); color:white; border:none; border-radius:16px;
  padding:14px; font-weight:900; font-family:'Nunito'; cursor:pointer;
  display:flex; align-items:center; justify-content:center; gap:8px;
```

- [ ] **Step 2: Lyrics viewer CSS qo'shish**

AI STORY VIEWER CSS blokining OXIRIGA (closing `}` dan keyin) quyidagini qo'shing:

```css
    /* ── SONG LYRICS VIEWER ── */
    #lyrics-viewer{
      position:fixed; inset:0; z-index:350; background:var(--bg);
      display:flex; flex-direction:column; animation:shUp 0.4s var(--spring);
    }
    #lyrics-viewer.h{display:none}
    .lv-lyrics{
      font-size:16px; color:var(--text); font-family:'Nunito',sans-serif;
      white-space:pre-wrap; line-height:2; margin:0;
    }
```

---

### 1c. index.html — `#lyrics-viewer` HTML qo'shish

`index.html` da `<!-- AI VIEWER -->` div ning oxirini toping (line ~973 atrofida):

```html
</div>
<!-- AI VIEWER -->
<div id="ai-viewer" class="h">
  ...
</div>
```

- [ ] **Step 3: `#lyrics-viewer` HTML ni `#ai-viewer` div DAN KEYIN qo'shish**

`</div>` — `#ai-viewer` ni yopuvchi teg dan keyin quyidagini qo'shing:

```html

  <!-- SONG LYRICS VIEWER -->
  <div id="lyrics-viewer" class="h">
    <div class="aiv-hdr">
      <button class="ni-pill" style="width:40px;background:#F0F0F0" onclick="closeLyrics()">✕</button>
      <div style="color:var(--text);flex:1;text-align:center;font-weight:800" id="lv-title">🎵</div>
      <div style="width:40px"></div>
    </div>
    <div class="aiv-body">
      <div class="aiv-story-title" id="lv-song-title"></div>
      <pre class="lv-lyrics" id="lv-lyrics"></pre>
    </div>
  </div>
```

---

### 1d. index.html — `closeLyrics()` funksiya qo'shish

`index.html` da `function closeAi()` qatorini toping (line ~1446):

```javascript
    function closeAi() { app.closeAi(); }
```

- [ ] **Step 4: `closeLyrics()` funksiya qo'shish**

`function closeAi()` qatoridan KEYIN quyidagini qo'shing:

```javascript
    function closeLyrics() { document.getElementById('lyrics-viewer').classList.add('h'); }
```

---

### 1e. main.js — `generateAiStory()` regen btn show

`main.js` da `generateAiStory()` metodini toping (line 647). Metodning boshida `viewer.classList.remove("h")` qatori bor:

```javascript
generateAiStory() {
    const viewer = document.getElementById("ai-viewer");
    const loading = document.getElementById("aiv-loading");
    const content = document.getElementById("aiv-content-wrap");

    viewer.classList.remove("h");
    loading.classList.remove("h");
    content.classList.add("h");
```

- [ ] **Step 5: `generateAiStory()` da regen btn show qo'shish**

`viewer.classList.remove("h");` qatoridan OLDIN quyidagini qo'shing:

```javascript
    document.getElementById('aiv-regen-btn').classList.remove('h');
    viewer.classList.remove("h");
```

Natija:
```javascript
generateAiStory() {
    const viewer = document.getElementById("ai-viewer");
    const loading = document.getElementById("aiv-loading");
    const content = document.getElementById("aiv-content-wrap");

    document.getElementById('aiv-regen-btn').classList.remove('h');
    viewer.classList.remove("h");
    loading.classList.remove("h");
    content.classList.add("h");
```

---

### 1f. main.js — `SongManager._play()` lyrics qo'shish

`main.js` da `class SongManager` blokini toping (line 399-407):

```javascript
class SongManager extends ContentManager {
    constructor(player, translator, ui) {
        super('songs', player, translator, ui);
    }

    _play(item) {
        if (window.badgeManager) badgeManager.onSongPlayed();
        super._play(item);
    }
}
```

- [ ] **Step 6: `SongManager._play()` ni to'liq yangilash**

Butun `_play(item)` metodini quyidagicha almashtiring:

```javascript
    _play(item) {
        if (window.badgeManager) badgeManager.onSongPlayed();
        const lang = this.translator.lang;
        const title = item.title[lang] || item.title.en;
        const text = item.text ? (item.text[lang] || item.text.en || '') : '';
        if (text) {
            document.getElementById('lv-title').textContent = item.emoji + ' ' + title;
            document.getElementById('lv-song-title').textContent = item.emoji + ' ' + title;
            document.getElementById('lv-lyrics').textContent = text;
            document.getElementById('lyrics-viewer').classList.remove('h');
        }
        super._play(item);
    }
```

---

### 1g. Commit

- [ ] **Step 7: O'zgarishlarni stage va commit qilish**

```bash
git add app/src/main/assets/www/index.html app/src/main/assets/www/main.js
git commit -m "feat(faza5): song lyrics viewer + hide AI regen btn for content stories"
```

---

## Task 2: Build, Install va Verify

**Files:** Hech qanday fayl o'zgartirilmaydi

**Interfaces:**
- Consumes: Task 1 o'zgarishlari

- [ ] **Step 1: Build va install**

```powershell
.\gradlew installDebug
```

Kutilgan natija: `BUILD SUCCESSFUL` va APK qurilmaga o'rnatiladi.

- [ ] **Step 2: Ertak ko'rishda regen tugma tekshiruvi**

1. App ochiladi, Ertaklar tabiga o'tish
2. Biror ertak kartasini bosish
3. `#ai-viewer` ochiladi — faqat "🔊 Eshittirish" tugmasi ko'rinishi kerak
4. "🔄 Yangi ertak" tugmasi **ko'rinmasligi** kerak ✅

- [ ] **Step 3: KidzoAI regen tugma tekshiruvi**

1. KidzoAI tab (yoki AI Studio) ga o'tish
2. "Ertak To'qish" tugmasini bosish
3. `#ai-viewer` ochiladi — **ikkala tugma** ko'rinishi kerak: "🔊 Eshittirish" + "🔄 Yangi ertak" ✅

- [ ] **Step 4: Song lyrics tekshiruvi**

1. Qo'shiqlar tabiga o'tish
2. Biror qo'shiq kartasini bosish
3. `#lyrics-viewer` ochiladi — emoji + sarlavha + to'liq so'zlar ko'rinishi kerak
4. Audio player bar pastda ko'rinishi kerak
5. ✕ tugmasi lyrics viewerni yopishi kerak

- [ ] **Step 5: Ko'p til tekshiruvi**

1. `🌍 RU` ga o'tish
2. Qo'shiq bosish → ruscha so'zlar ko'rinishi kerak
3. `🌍 EN` ga o'tish
4. Qo'shiq bosish → inglizcha so'zlar ko'rinishi kerak

- [ ] **Step 6: Logcat tekshiruvi**

```powershell
adb logcat -s chromium | Select-String -Pattern "Error|error|Uncaught"
```

Kutilgan natija: Hech qanday JS xatosi yo'q.

- [ ] **Step 7: Commit**

```bash
git commit --allow-empty -m "chore(faza5): build verified — lyrics viewer + regen fix working"
```
